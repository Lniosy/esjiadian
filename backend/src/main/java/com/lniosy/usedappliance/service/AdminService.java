package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.dto.admin.TrendPointDto;
import com.lniosy.usedappliance.dto.order.OrderDto;
import com.lniosy.usedappliance.dto.user.UserProfileDto;
import com.lniosy.usedappliance.entity.OrderInfo;
import com.lniosy.usedappliance.entity.OrderItem;
import com.lniosy.usedappliance.entity.Product;
import com.lniosy.usedappliance.entity.SysUser;
import com.lniosy.usedappliance.mapper.OrderInfoMapper;
import com.lniosy.usedappliance.mapper.OrderItemMapper;
import com.lniosy.usedappliance.mapper.ProductMapper;
import com.lniosy.usedappliance.mapper.SysUserMapper;
import com.lniosy.usedappliance.mapper.AdminStatsMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class AdminService {
    private final SysUserMapper sysUserMapper;
    private final ProductMapper productMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final EvaluationService evaluationService;
    private final AdminStatsMapper adminStatsMapper;

    public AdminService(SysUserMapper sysUserMapper, ProductMapper productMapper,
                        OrderInfoMapper orderInfoMapper, OrderItemMapper orderItemMapper,
                        EvaluationService evaluationService, AdminStatsMapper adminStatsMapper) {
        this.sysUserMapper = sysUserMapper;
        this.productMapper = productMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.evaluationService = evaluationService;
        this.adminStatsMapper = adminStatsMapper;
    }

    public List<UserProfileDto> users() {
        return users(null, null, null);
    }

    public List<UserProfileDto> users(String keyword, Boolean enabled, String role) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getId);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getEmail, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        if (enabled != null) {
            qw.eq(SysUser::getEnabled, enabled);
        }
        if (role != null && !role.isBlank()) {
            qw.like(SysUser::getRoles, role.toUpperCase());
        }
        return sysUserMapper.selectList(qw)
                .stream()
                .map(u -> new UserProfileDto(
                        u.getId(), u.getNickname(), u.getAvatarUrl(), u.getBio(),
                        u.getRoles(), u.getAuthStatus(), u.getEnabled()))
                .toList();
    }

    public List<OrderDto> orders() {
        List<OrderInfo> orders = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>().orderByDesc(OrderInfo::getId));
        return orders.stream().map(o -> {
            OrderItem item = orderItemMapper.selectOne(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, o.getId())
                    .last("limit 1"));
            Long productId = item == null ? 0L : item.getProductId();
            return new OrderDto(o.getId(), o.getOrderNo(), productId, o.getBuyerId(), o.getSellerId(), o.getPaidAmount(), o.getStatus());
        }).toList();
    }

    public Map<String, Object> stats() {
        return Map.of(
                "userCount", sysUserMapper.selectCount(null),
                "productCount", productMapper.selectCount(null),
                "orderCount", orderInfoMapper.selectCount(null),
                "evaluationCount", evaluationService.count());
    }

    public void setUserEnabled(Long userId, boolean enabled) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        if (Boolean.TRUE.equals(user.getEnabled()) == enabled) {
            return;
        }
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getEnabled, enabled));
    }

    public String exportOrdersCsv() {
        List<OrderInfo> orders = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>().orderByDesc(OrderInfo::getId));
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("orderId,orderNo,buyerId,sellerId,productId,paidAmount,status");
        for (OrderInfo o : orders) {
            OrderItem item = orderItemMapper.selectOne(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, o.getId())
                    .last("limit 1"));
            Long productId = item == null ? 0L : item.getProductId();
            joiner.add(csv(o.getId()) + "," + csv(o.getOrderNo()) + "," + csv(o.getBuyerId()) + ","
                    + csv(o.getSellerId()) + "," + csv(productId) + "," + csv(o.getPaidAmount()) + "," + csv(o.getStatus()));
        }
        return joiner.toString();
    }

    public Map<String, Object> trend(int days) {
        int safeDays = Math.max(1, Math.min(days, 30));
        LocalDate start = LocalDate.now().minusDays(safeDays - 1L);
        LocalDateTime from = start.atStartOfDay();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        Map<String, TrendPointDto> index = new LinkedHashMap<>();
        for (int i = 0; i < safeDays; i++) {
            String date = start.plusDays(i).format(formatter);
            index.put(date, new TrendPointDto(date, 0L, 0L, 0L, BigDecimal.ZERO));
        }

        mergeCount(index, adminStatsMapper.userDaily(from), "user");
        mergeCount(index, adminStatsMapper.productDaily(from), "product");
        mergeCount(index, adminStatsMapper.orderDaily(from), "order");
        mergeAmount(index, adminStatsMapper.gmvDaily(from));

        return Map.of("days", safeDays, "list", index.values());
    }

    public String exportTrendCsv(int days) {
        @SuppressWarnings("unchecked")
        List<TrendPointDto> list = (List<TrendPointDto>) trend(days).get("list");
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("date,userCount,productCount,orderCount,gmv");
        list.forEach(t -> joiner.add(t.date() + "," + t.userCount() + "," + t.productCount() + "," + t.orderCount() + "," + t.gmv()));
        return joiner.toString();
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private void mergeCount(Map<String, TrendPointDto> index, List<Map<String, Object>> rows, String type) {
        for (Map<String, Object> row : rows) {
            String date = String.valueOf(row.get("d"));
            TrendPointDto old = index.get(date);
            if (old == null) {
                continue;
            }
            long count = ((Number) row.get("c")).longValue();
            TrendPointDto next = switch (type) {
                case "user" -> new TrendPointDto(old.date(), count, old.productCount(), old.orderCount(), old.gmv());
                case "product" -> new TrendPointDto(old.date(), old.userCount(), count, old.orderCount(), old.gmv());
                default -> new TrendPointDto(old.date(), old.userCount(), old.productCount(), count, old.gmv());
            };
            index.put(date, next);
        }
    }

    private void mergeAmount(Map<String, TrendPointDto> index, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            String date = String.valueOf(row.get("d"));
            TrendPointDto old = index.get(date);
            if (old == null) {
                continue;
            }
            BigDecimal amount = new BigDecimal(String.valueOf(row.get("c")));
            index.put(date, new TrendPointDto(old.date(), old.userCount(), old.productCount(), old.orderCount(), amount));
        }
    }
}
