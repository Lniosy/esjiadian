package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.dto.evaluation.EvaluationDto;
import com.lniosy.usedappliance.dto.evaluation.EvaluationRequest;
import com.lniosy.usedappliance.dto.evaluation.RatingDetailDto;
import com.lniosy.usedappliance.dto.order.OrderDto;
import com.lniosy.usedappliance.entity.Evaluation;
import com.lniosy.usedappliance.entity.OrderInfo;
import com.lniosy.usedappliance.entity.SysUser;
import com.lniosy.usedappliance.mapper.EvaluationMapper;
import com.lniosy.usedappliance.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvaluationService {
    private final EvaluationMapper evaluationMapper;
    private final SysUserMapper sysUserMapper;
    private final OrderService orderService;

    public EvaluationService(EvaluationMapper evaluationMapper, SysUserMapper sysUserMapper, OrderService orderService) {
        this.evaluationMapper = evaluationMapper;
        this.sysUserMapper = sysUserMapper;
        this.orderService = orderService;
    }

    public EvaluationDto create(Long buyerId, Long orderId, EvaluationRequest req) {
        OrderDto order = orderService.getOrder(orderId, buyerId);
        if (!order.buyerId().equals(buyerId) || !"PENDING_REVIEW".equals(order.status())) {
            throw new BizException(400, "当前订单状态不可评价");
        }
        OrderInfo orderEntity = orderService.getOrderEntity(orderId);
        if (orderEntity != null && orderEntity.getReceivedAt() != null
                && orderEntity.getReceivedAt().plusDays(15).isBefore(LocalDateTime.now())) {
            throw new BizException(400, "订单已超过15天评价期");
        }

        Evaluation exists = evaluationMapper.selectOne(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getOrderId, orderId)
                .last("limit 1"));
        if (exists != null) {
            throw new BizException(409, "订单已评价");
        }
        Evaluation e = new Evaluation();
        e.setOrderId(orderId);
        e.setProductId(req.productId());
        e.setSellerId(order.sellerId());
        e.setBuyerId(buyerId);
        e.setScore(req.score());
        e.setContent(req.content());
        e.setImages(req.images() == null ? null : String.join(",", req.images()));
        e.setTags(req.tags() == null ? null : String.join(",", req.tags()));
        evaluationMapper.insert(e);
        recalculateSellerScore(order.sellerId());
        orderService.completeAfterReview(orderId);
        return toDto(e);
    }

    public List<EvaluationDto> byProduct(Long productId) {
        return evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getProductId, productId)
                        .orderByDesc(Evaluation::getId))
                .stream().map(this::toDto).toList();
    }

    public double shopRating(Long sellerId) {
        List<Evaluation> list = evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getSellerId, sellerId));
        return list.stream().mapToInt(Evaluation::getScore).average().orElse(5.0);
    }

    public RatingDetailDto shopRatingDetail(Long sellerId) {
        List<Evaluation> list = evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getSellerId, sellerId));
        double rating = list.stream().mapToInt(Evaluation::getScore).average().orElse(5.0);
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        Map<String, Long> tagCounter = new HashMap<>();
        for (Evaluation e : list) {
            distribution.put(e.getScore(), distribution.getOrDefault(e.getScore(), 0L) + 1);
            if (e.getTags() != null && !e.getTags().isBlank()) {
                String[] tags = e.getTags().split(",");
                for (String tag : tags) {
                    String t = tag.trim();
                    if (!t.isEmpty()) {
                        tagCounter.put(t, tagCounter.getOrDefault(t, 0L) + 1);
                    }
                }
            }
        }
        List<Map<String, Object>> tagCloud = new ArrayList<>(tagCounter.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(20)
                .map(e -> Map.<String, Object>of("tag", e.getKey(), "count", e.getValue()))
                .toList());

        return new RatingDetailDto(rating, list.size(), distribution, tagCloud);
    }

    public long count() {
        return evaluationMapper.selectCount(null);
    }

    public int recomputeAllSellerScores() {
        List<SysUser> sellers = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .like(SysUser::getRoles, "ROLE_SELLER"));
        for (SysUser seller : sellers) {
            recalculateSellerScore(seller.getId());
        }
        return sellers.size();
    }

    public void recalculateSellerScore(Long sellerId) {
        List<Evaluation> list = evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getSellerId, sellerId));
        double avg = list.stream().mapToInt(Evaluation::getScore).average().orElse(5.0);
        // Small-sample smoothing: blend toward neutral 5.0
        double weight = Math.min(1.0, list.size() / 20.0);
        double score = avg * weight + 5.0 * (1 - weight);
        BigDecimal scoreVal = BigDecimal.valueOf(score).setScale(2, java.math.RoundingMode.HALF_UP);
        SysUser toUpdate = new SysUser();
        toUpdate.setId(sellerId);
        toUpdate.setSellerScore(scoreVal);
        sysUserMapper.updateById(toUpdate);
    }

    private EvaluationDto toDto(Evaluation e) {
        List<String> images = (e.getImages() == null || e.getImages().isBlank()) ? List.of() : List.of(e.getImages().split(","));
        List<String> tags = (e.getTags() == null || e.getTags().isBlank()) ? List.of() : List.of(e.getTags().split(","));
        long createdAt = e.getCreatedAt() == null ? System.currentTimeMillis() :
                e.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new EvaluationDto(e.getOrderId(), e.getProductId(), e.getSellerId(), e.getBuyerId(),
                e.getScore(), e.getContent(), images, tags, createdAt);
    }
}
