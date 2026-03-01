package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lniosy.usedappliance.dto.evaluation.EvaluationDto;
import com.lniosy.usedappliance.dto.shop.ShopDto;
import com.lniosy.usedappliance.dto.shop.ShopOverviewDto;
import com.lniosy.usedappliance.dto.shop.ShopOverviewProductDto;
import com.lniosy.usedappliance.dto.shop.ShopUpsertRequest;
import com.lniosy.usedappliance.entity.Evaluation;
import com.lniosy.usedappliance.entity.Product;
import com.lniosy.usedappliance.entity.ProductImage;
import com.lniosy.usedappliance.entity.Shop;
import com.lniosy.usedappliance.mapper.EvaluationMapper;
import com.lniosy.usedappliance.mapper.ProductImageMapper;
import com.lniosy.usedappliance.mapper.ProductMapper;
import com.lniosy.usedappliance.mapper.ShopMapper;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;

@Service
public class ShopService {
    private final ShopMapper shopMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final EvaluationMapper evaluationMapper;

    public ShopService(ShopMapper shopMapper, ProductMapper productMapper,
                       ProductImageMapper productImageMapper, EvaluationMapper evaluationMapper) {
        this.shopMapper = shopMapper;
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.evaluationMapper = evaluationMapper;
    }

    public ShopDto myShop(Long userId) {
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getUserId, userId)
                .last("limit 1"));
        if (shop == null) {
            return null;
        }
        return toDto(shop);
    }

    public ShopDto upsertMyShop(Long userId, ShopUpsertRequest req) {
        Shop old = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getUserId, userId)
                .last("limit 1"));
        if (old == null) {
            Shop shop = new Shop();
            shop.setUserId(userId);
            shop.setName(req.name());
            shop.setLogoUrl(req.logoUrl());
            shop.setIntro(req.intro());
            shop.setCategories(req.categories());
            shop.setRegion(req.region());
            shopMapper.insert(shop);
            return toDto(shop);
        }
        shopMapper.update(null, new LambdaUpdateWrapper<Shop>()
                .eq(Shop::getId, old.getId())
                .set(Shop::getName, req.name())
                .set(Shop::getLogoUrl, req.logoUrl())
                .set(Shop::getIntro, req.intro())
                .set(Shop::getCategories, req.categories())
                .set(Shop::getRegion, req.region()));
        Shop updated = shopMapper.selectById(old.getId());
        return toDto(updated);
    }

    public ShopDto detail(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        return shop == null ? null : toDto(shop);
    }

    public ShopOverviewDto overview(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            return null;
        }
        Long sellerId = shop.getUserId();
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, sellerId)
                .eq(Product::getStatus, "ON_SHELF")
                .orderByDesc(Product::getId));
        List<ShopOverviewProductDto> productDtos = products.stream().map(this::toOverviewProduct).toList();

        List<Evaluation> evaluations = evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getSellerId, sellerId)
                .orderByDesc(Evaluation::getId)
                .last("limit 20"));
        List<EvaluationDto> evaluationDtos = evaluations.stream().map(this::toEvaluationDto).toList();
        double rating = evaluations.stream().mapToInt(Evaluation::getScore).average().orElse(5.0);

        return new ShopOverviewDto(
                toDto(shop),
                rating,
                (long) evaluations.size(),
                (long) productDtos.size(),
                productDtos,
                evaluationDtos
        );
    }

    private ShopDto toDto(Shop s) {
        return new ShopDto(s.getId(), s.getUserId(), s.getName(), s.getLogoUrl(), s.getIntro(), s.getCategories(), s.getRegion());
    }

    private ShopOverviewProductDto toOverviewProduct(Product p) {
        ProductImage img = productImageMapper.selectOne(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, p.getId())
                .orderByAsc(ProductImage::getSort)
                .last("limit 1"));
        return new ShopOverviewProductDto(p.getId(), p.getTitle(), p.getPrice(), p.getStatus(), img == null ? "" : img.getImageUrl());
    }

    private EvaluationDto toEvaluationDto(Evaluation e) {
        List<String> images = (e.getImages() == null || e.getImages().isBlank()) ? List.of() : List.of(e.getImages().split(","));
        List<String> tags = (e.getTags() == null || e.getTags().isBlank()) ? List.of() : List.of(e.getTags().split(","));
        long createdAt = e.getCreatedAt() == null ? System.currentTimeMillis() :
                e.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new EvaluationDto(e.getOrderId(), e.getProductId(), e.getSellerId(), e.getBuyerId(),
                e.getScore(), e.getContent(), images, tags, createdAt);
    }
}
