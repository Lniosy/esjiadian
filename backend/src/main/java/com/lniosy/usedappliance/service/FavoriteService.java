package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.entity.Product;
import com.lniosy.usedappliance.entity.UserFavorite;
import com.lniosy.usedappliance.mapper.ProductMapper;
import com.lniosy.usedappliance.mapper.UserFavoriteMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FavoriteService {
    private final UserFavoriteMapper userFavoriteMapper;
    private final ProductMapper productMapper;
    private final RecommendTraceService recommendTraceService;

    public FavoriteService(UserFavoriteMapper userFavoriteMapper,
                           ProductMapper productMapper,
                           RecommendTraceService recommendTraceService) {
        this.userFavoriteMapper = userFavoriteMapper;
        this.productMapper = productMapper;
        this.recommendTraceService = recommendTraceService;
    }

    public List<Long> listIds(Long userId) {
        return userFavoriteMapper.selectList(new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .orderByDesc(UserFavorite::getId))
                .stream()
                .map(UserFavorite::getProductId)
                .filter(id -> id != null && id > 0)
                .toList();
    }

    public boolean exists(Long userId, Long productId) {
        if (productId == null || productId <= 0) {
            return false;
        }
        return userFavoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getProductId, productId)) > 0;
    }

    public void add(Long userId, Long productId) {
        if (productId == null || productId <= 0) {
            throw new BizException(400, "商品ID无效");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException(404, "商品不存在");
        }
        if (exists(userId, productId)) {
            return;
        }
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        userFavoriteMapper.insert(favorite);
        recommendTraceService.recordEvent(userId, productId, product.getCategoryId(), "FAVORITE", 3.5);
    }

    public void remove(Long userId, Long productId) {
        userFavoriteMapper.delete(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getProductId, productId));
    }
}
