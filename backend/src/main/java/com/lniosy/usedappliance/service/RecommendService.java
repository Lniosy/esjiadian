package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lniosy.usedappliance.dto.product.ProductDto;
import com.lniosy.usedappliance.entity.Product;
import com.lniosy.usedappliance.entity.ProductImage;
import com.lniosy.usedappliance.entity.Shop;
import com.lniosy.usedappliance.entity.SysUser;
import com.lniosy.usedappliance.entity.UserAddress;
import com.lniosy.usedappliance.mapper.ProductImageMapper;
import com.lniosy.usedappliance.mapper.ProductMapper;
import com.lniosy.usedappliance.mapper.ShopMapper;
import com.lniosy.usedappliance.mapper.SysUserMapper;
import com.lniosy.usedappliance.mapper.UserAddressMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RecommendService {
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final SysUserMapper sysUserMapper;
    private final ShopMapper shopMapper;
    private final UserAddressMapper userAddressMapper;
    private final RecommendTraceService recommendTraceService;

    public RecommendService(ProductMapper productMapper, ProductImageMapper productImageMapper,
                            SysUserMapper sysUserMapper, ShopMapper shopMapper, UserAddressMapper userAddressMapper,
                            RecommendTraceService recommendTraceService) {
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.sysUserMapper = sysUserMapper;
        this.shopMapper = shopMapper;
        this.userAddressMapper = userAddressMapper;
        this.recommendTraceService = recommendTraceService;
    }

    public List<ProductDto> home(Long userId) {
        List<Product> candidates = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, "ON_SHELF")
                .orderByDesc(Product::getSalesCount)
                .orderByDesc(Product::getId)
                .last("limit 300"));
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Product> hotRanking = candidates.stream()
                .sorted(Comparator.comparing(Product::getSalesCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Product::getId, Comparator.reverseOrder()))
                .toList();
        List<Product> freshRanking = candidates.stream()
                .sorted(Comparator.comparing(Product::getId, Comparator.reverseOrder()))
                .toList();

        RecommendTraceService.UserPreference preference = recommendTraceService.preference(userId, 30, 500);
        if (preference.isEmpty()) {
            return mixedRanking(List.of(), hotRanking, freshRanking, 0, 8, 4).stream()
                    .limit(12)
                    .map(this::toDto)
                    .toList();
        }
        Map<Long, Double> scoreMap = new HashMap<>();
        for (Product product : candidates) {
            scoreMap.put(product.getId(), personalizedScore(product, preference));
        }
        List<Product> personalizedRanking = candidates.stream()
                .sorted(Comparator.comparingDouble((Product p) -> scoreMap.getOrDefault(p.getId(), 0D)).reversed()
                        .thenComparing(Product::getSalesCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Product::getId, Comparator.reverseOrder()))
                .toList();
        return mixedRanking(personalizedRanking, hotRanking, freshRanking, 8, 2, 2).stream()
                .limit(12)
                .map(this::toDto)
                .toList();
    }

    public List<ProductDto> related(Long productId) {
        Product current = productMapper.selectById(productId);
        if (current == null) {
            return List.of();
        }
        List<Product> candidates = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .ne(Product::getId, productId)
                .eq(Product::getStatus, "ON_SHELF")
                .last("limit 120"));
        return candidates.stream()
                .sorted(Comparator.comparingDouble((Product p) -> relatedScore(current, p)).reversed()
                        .thenComparing(Product::getSalesCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Product::getId, Comparator.reverseOrder()))
                .limit(8)
                .map(this::toDto)
                .toList();
    }

    public List<ProductDto> featured() {
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, "ON_SHELF")
                        .orderByDesc(Product::getSalesCount)
                        .orderByDesc(Product::getId)
                        .last("limit 8"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<ProductDto> nearby(Long userId) {
        String city = resolveUserCity(userId);
        if (city.isBlank()) {
            return List.of();
        }
        String simpleCity = normalizeCity(city);
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, "ON_SHELF");
        if (!simpleCity.isBlank() && !simpleCity.equals(city)) {
            query.and(w -> w.like(Product::getRegion, city).or().like(Product::getRegion, simpleCity));
        } else {
            query.like(Product::getRegion, city);
        }
        return productMapper.selectList(query
                        .orderByDesc(Product::getSalesCount)
                        .orderByDesc(Product::getId)
                        .last("limit 12"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    private double personalizedScore(Product product, RecommendTraceService.UserPreference preference) {
        double score = 0D;
        int sales = product.getSalesCount() == null ? 0 : product.getSalesCount();
        score += Math.min(20, sales) * 0.2;
        score += preference.brandWeights().getOrDefault(safeText(product.getBrand()), 0D) * 1.6;
        score += preference.categoryWeights().getOrDefault(product.getCategoryId(), 0D) * 1.2;
        if (preference.interactedProductIds().contains(product.getId())) {
            score -= 1.5;
        }
        return score;
    }

    private double relatedScore(Product current, Product candidate) {
        double score = 0D;
        if (safeText(current.getBrand()).equalsIgnoreCase(safeText(candidate.getBrand()))) {
            score += 3.0;
        }
        if (current.getCategoryId() != null && current.getCategoryId().equals(candidate.getCategoryId())) {
            score += 2.0;
        }
        if (current.getSellerId() != null && current.getSellerId().equals(candidate.getSellerId())) {
            score += 1.5;
        }
        BigDecimal base = current.getPrice() == null ? BigDecimal.ZERO : current.getPrice();
        BigDecimal target = candidate.getPrice() == null ? BigDecimal.ZERO : candidate.getPrice();
        if (base.compareTo(BigDecimal.ZERO) > 0 && target.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = base.subtract(target).abs();
            BigDecimal ratio = diff.divide(base, 4, java.math.RoundingMode.HALF_UP);
            double r = ratio.doubleValue();
            if (r <= 0.20) {
                score += 1.2;
            } else if (r <= 0.50) {
                score += 0.6;
            }
        }
        int sales = candidate.getSalesCount() == null ? 0 : candidate.getSalesCount();
        score += Math.min(10, sales) * 0.05;
        return score;
    }

    private List<Product> mixedRanking(List<Product> personalized,
                                       List<Product> hot,
                                       List<Product> fresh,
                                       int personalizedQuota,
                                       int hotQuota,
                                       int freshQuota) {
        List<Product> result = new ArrayList<>();
        Set<Long> usedIds = new HashSet<>();
        appendUnique(result, usedIds, personalized, personalizedQuota);
        appendUnique(result, usedIds, hot, hotQuota);
        appendUnique(result, usedIds, fresh, freshQuota);
        if (result.size() < 12) {
            appendUnique(result, usedIds, personalized, 12 - result.size());
        }
        if (result.size() < 12) {
            appendUnique(result, usedIds, hot, 12 - result.size());
        }
        if (result.size() < 12) {
            appendUnique(result, usedIds, fresh, 12 - result.size());
        }
        return result;
    }

    private void appendUnique(List<Product> result, Set<Long> usedIds, List<Product> source, int limit) {
        if (limit <= 0 || source == null || source.isEmpty()) {
            return;
        }
        int added = 0;
        for (Product item : source) {
            if (item == null || item.getId() == null || usedIds.contains(item.getId())) {
                continue;
            }
            result.add(item);
            usedIds.add(item.getId());
            added++;
            if (added >= limit || result.size() >= 12) {
                return;
            }
        }
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    private String resolveUserCity(Long userId) {
        if (userId == null || userId <= 0) {
            return "";
        }
        UserAddress address = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, true)
                .orderByDesc(UserAddress::getId)
                .last("limit 1"));
        if (address == null) {
            address = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                    .eq(UserAddress::getUserId, userId)
                    .orderByDesc(UserAddress::getId)
                    .last("limit 1"));
        }
        return address == null ? "" : safeText(address.getCity());
    }

    private String normalizeCity(String city) {
        String text = safeText(city);
        if (text.endsWith("市")) {
            return text.substring(0, text.length() - 1);
        }
        return text;
    }

    private ProductDto toDto(Product p) {
        SysUser seller = sysUserMapper.selectById(p.getSellerId());
        // 查询卖家店铺名称，优先使用店铺名展示
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getUserId, p.getSellerId())
                .last("LIMIT 1"));
        String shopName = shop != null ? shop.getName() : null;
        String shopLogo = shop != null ? shop.getLogoUrl() : null;

        List<String> images = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, p.getId())
                        .orderByAsc(ProductImage::getSort))
                .stream().map(ProductImage::getImageUrl).toList();
        return new ProductDto(p.getId(), p.getSellerId(),
                seller == null ? "未知用户" : seller.getNickname(),
                seller == null ? "" : seller.getAvatarUrl(),
                shopName, shopLogo,
                sellerScoreValue(seller),
                sellerAuthStatusValue(seller),
                p.getTitle(), p.getCategoryId(), p.getBrand(), p.getModel(),
                p.getPurchaseDate(), displayConditionLevel(p.getConditionLevel()), p.getFunctionStatus(), p.getRepairHistory(),
                p.getDescription(), p.getVideoUrl(), p.getPrice(), p.getOriginalPrice(),
                p.getRegion(), p.getTradeMethods(), p.getStatus(), p.getRejectReason(), p.getSold(), p.getSalesCount(), images);
    }

    private double sellerScoreValue(SysUser seller) {
        if (seller == null || seller.getSellerScore() == null) {
            return 5.0;
        }
        BigDecimal score = seller.getSellerScore();
        return score.doubleValue();
    }

    private String sellerAuthStatusValue(SysUser seller) {
        if (seller == null || seller.getAuthStatus() == null || seller.getAuthStatus().isBlank()) {
            return "NOT_SUBMITTED";
        }
        return seller.getAuthStatus();
    }

    private String displayConditionLevel(String stored) {
        if (stored == null || stored.isBlank()) {
            return stored;
        }
        String normalized = stored.trim().toUpperCase();
        return switch (normalized) {
            case "NEW", "全新" -> "全新";
            case "LIKE_NEW", "99新", "95新", "9.5成新" -> "95新";
            case "GOOD", "9成新" -> "9成新";
            case "FAIR", "8成新及以下", "8成新以下", "7成新及以下" -> "8成新及以下";
            default -> stored;
        };
    }
}
