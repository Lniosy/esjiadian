package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lniosy.usedappliance.entity.Product;
import com.lniosy.usedappliance.entity.RecommendTrace;
import com.lniosy.usedappliance.entity.UserFavorite;
import com.lniosy.usedappliance.mapper.ProductMapper;
import com.lniosy.usedappliance.mapper.RecommendTraceMapper;
import com.lniosy.usedappliance.mapper.UserFavoriteMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendTraceService {
    private final RecommendTraceMapper recommendTraceMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final ProductMapper productMapper;

    public RecommendTraceService(RecommendTraceMapper recommendTraceMapper,
                                 UserFavoriteMapper userFavoriteMapper,
                                 ProductMapper productMapper) {
        this.recommendTraceMapper = recommendTraceMapper;
        this.userFavoriteMapper = userFavoriteMapper;
        this.productMapper = productMapper;
    }

    public void recordEvent(Long userId, Long productId, String eventType, double score) {
        recordEvent(userId, productId, null, eventType, score);
    }

    public void recordEvent(Long userId, Long productId, Long categoryId, String eventType, double score) {
        if (userId == null || eventType == null || eventType.isBlank()) {
            return;
        }
        RecommendTrace trace = new RecommendTrace();
        trace.setUserId(userId);
        trace.setProductId((productId == null || productId <= 0) ? null : productId);
        trace.setCategoryId((categoryId == null || categoryId <= 0) ? null : categoryId);
        trace.setEventType(eventType.trim().toUpperCase());
        trace.setEventScore(BigDecimal.valueOf(score <= 0 ? defaultScore(eventType) : score));
        recommendTraceMapper.insert(trace);
    }

    public UserPreference preference(Long userId, int recentDays, int maxRecords) {
        if (userId == null || userId <= 0) {
            return UserPreference.empty();
        }
        int size = Math.max(20, Math.min(maxRecords, 500));
        LocalDateTime deadline = LocalDateTime.now().minusDays(Math.max(1, recentDays));
        List<RecommendTrace> traces = recommendTraceMapper.selectList(new LambdaQueryWrapper<RecommendTrace>()
                        .eq(RecommendTrace::getUserId, userId)
                        .ge(RecommendTrace::getCreatedAt, deadline)
                        .orderByDesc(RecommendTrace::getId)
                        .last("limit " + size));
        List<UserFavorite> favorites = userFavoriteMapper.selectList(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .orderByDesc(UserFavorite::getId)
                .last("limit 300"));
        if (traces.isEmpty() && favorites.isEmpty()) {
            return UserPreference.empty();
        }

        Set<Long> productIds = traces.stream()
                .map(RecommendTrace::getProductId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        productIds.addAll(favorites.stream()
                .map(UserFavorite::getProductId)
                .filter(id -> id != null && id > 0)
                .toList());
        Map<Long, Product> productById = productIds.isEmpty()
                ? Collections.emptyMap()
                : productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .in(Product::getId, productIds))
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));

        Map<String, Double> brandWeights = new HashMap<>();
        Map<Long, Double> categoryWeights = new HashMap<>();
        Set<Long> interactedProductIds = new HashSet<>();

        for (RecommendTrace trace : traces) {
            double score = trace.getEventScore() == null ? defaultScore(trace.getEventType()) : trace.getEventScore().doubleValue();
            if (score <= 0) {
                continue;
            }
            if (trace.getCategoryId() != null && trace.getCategoryId() > 0) {
                categoryWeights.merge(trace.getCategoryId(), score, Double::sum);
            }
            Product product = productById.get(trace.getProductId());
            if (product == null) {
                continue;
            }
            interactedProductIds.add(product.getId());
            if (product.getBrand() != null && !product.getBrand().isBlank()) {
                brandWeights.merge(product.getBrand().trim(), score, Double::sum);
            }
            if (product.getCategoryId() != null) {
                categoryWeights.merge(product.getCategoryId(), score, Double::sum);
            }
        }
        for (UserFavorite favorite : favorites) {
            Product product = productById.get(favorite.getProductId());
            if (product == null) {
                continue;
            }
            double score = 3.5;
            interactedProductIds.add(product.getId());
            if (product.getBrand() != null && !product.getBrand().isBlank()) {
                brandWeights.merge(product.getBrand().trim(), score, Double::sum);
            }
            if (product.getCategoryId() != null) {
                categoryWeights.merge(product.getCategoryId(), score, Double::sum);
            }
        }
        return new UserPreference(brandWeights, categoryWeights, interactedProductIds);
    }

    private double defaultScore(String eventType) {
        String type = eventType == null ? "" : eventType.trim().toUpperCase();
        return switch (type) {
            case "PAY_SUCCESS" -> 4.0;
            case "ORDER_CREATE" -> 3.0;
            case "FAVORITE" -> 3.5;
            case "CART" -> 2.5;
            case "RECOMMEND_CLICK" -> 1.2;
            case "DETAIL_VIEW", "CATEGORY_FILTER" -> 1.0;
            case "CATEGORY_CLICK" -> 0.8;
            case "SEARCH_EXPOSE" -> 1.2;
            case "RECOMMEND_EXPOSE", "LIST_EXPOSE" -> 0.3;
            default -> 1.0;
        };
    }

    public record UserPreference(Map<String, Double> brandWeights,
                                 Map<Long, Double> categoryWeights,
                                 Set<Long> interactedProductIds) {
        public static UserPreference empty() {
            return new UserPreference(Map.of(), Map.of(), Set.of());
        }

        public boolean isEmpty() {
            return brandWeights.isEmpty() && categoryWeights.isEmpty();
        }
    }
}
