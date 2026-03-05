package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.common.PageResult;
import com.lniosy.usedappliance.dto.product.ProductCreateRequest;
import com.lniosy.usedappliance.dto.product.ProductDto;
import com.lniosy.usedappliance.dto.product.ProductQuery;
import com.lniosy.usedappliance.entity.Product;
import com.lniosy.usedappliance.entity.ProductImage;
import com.lniosy.usedappliance.entity.Shop;
import com.lniosy.usedappliance.entity.SysUser;
import com.lniosy.usedappliance.mapper.ProductImageMapper;
import com.lniosy.usedappliance.mapper.ProductMapper;
import com.lniosy.usedappliance.mapper.ShopMapper;
import com.lniosy.usedappliance.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final SysUserMapper sysUserMapper;
    private final ShopMapper shopMapper;
    private final RecommendTraceService recommendTraceService;

    public ProductService(ProductMapper productMapper, ProductImageMapper productImageMapper,
                          SysUserMapper sysUserMapper, ShopMapper shopMapper,
                          RecommendTraceService recommendTraceService) {
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.sysUserMapper = sysUserMapper;
        this.shopMapper = shopMapper;
        this.recommendTraceService = recommendTraceService;
    }

    public ProductDto create(Long sellerId, ProductCreateRequest req) {
        SysUser seller = sysUserMapper.selectById(sellerId);
        if (seller == null || !"APPROVED".equals(seller.getAuthStatus())) {
            throw new BizException(403, "发布商品前需完成实名认证");
        }

        Product product = new Product();
        product.setSellerId(sellerId);
        product.setCategoryId(req.categoryId());
        product.setTitle(req.title());
        product.setBrand(req.brand());
        product.setModel(req.model());
        product.setPurchaseDate(req.purchaseDate());
        product.setConditionLevel(normalizeConditionLevel(req.conditionLevel()));
        product.setFunctionStatus(req.functionStatus());
        product.setRepairHistory(req.repairHistory());
        product.setDescription(req.description());
        product.setVideoUrl(req.videoUrl());
        product.setPrice(req.price());
        product.setOriginalPrice(req.originalPrice());
        product.setRegion(req.region());
        product.setTradeMethods(req.tradeMethods());
        product.setStatus("PENDING_REVIEW");
        product.setSold(false);
        product.setSalesCount(0);
        productMapper.insert(product);
        saveImages(product.getId(), req.images());
        return toDto(product);
    }

    public ProductDto update(Long sellerId, Long productId, ProductCreateRequest req) {
        Product old = productMapper.selectById(productId);
        if (old == null) {
            throw new BizException(404, "商品不存在");
        }
        if (!old.getSellerId().equals(sellerId) || "SOLD".equals(old.getStatus())) {
            throw new BizException(403, "无权编辑或商品已售出");
        }
        old.setCategoryId(req.categoryId());
        old.setTitle(req.title());
        old.setBrand(req.brand());
        old.setModel(req.model());
        old.setPurchaseDate(req.purchaseDate());
        old.setConditionLevel(normalizeConditionLevel(req.conditionLevel()));
        old.setFunctionStatus(req.functionStatus());
        old.setRepairHistory(req.repairHistory());
        old.setDescription(req.description());
        old.setVideoUrl(req.videoUrl());
        old.setPrice(req.price());
        old.setOriginalPrice(req.originalPrice());
        old.setRegion(req.region());
        old.setTradeMethods(req.tradeMethods());
        old.setStatus("PENDING_REVIEW");
        old.setRejectReason(null);
        productMapper.updateById(old);

        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, productId));
        saveImages(productId, req.images());
        return toDto(old);
    }

    public ProductDto detail(Long id) {
        return detail(id, null);
    }

    public ProductDto detail(Long id, Long viewerId) {
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException(404, "商品不存在");
        }
        recommendTraceService.recordEvent(viewerId, p.getId(), "DETAIL_VIEW", 1.0);
        return toDto(p);
    }

    public PageResult<ProductDto> list(ProductQuery q) {
        return list(q, null);
    }

    public PageResult<ProductDto> list(ProductQuery q, Long viewerId) {
        int pageNum = q.pageNum() == null ? 1 : q.pageNum();
        int pageSize = q.pageSize() == null ? 10 : q.pageSize();
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, "ON_SHELF");
        if (q.keyword() != null && !q.keyword().isBlank()) {
            qw.and(w -> w.like(Product::getTitle, q.keyword())
                    .or().like(Product::getBrand, q.keyword())
                    .or().like(Product::getModel, q.keyword()));
        }
        if (q.categoryId() != null) qw.eq(Product::getCategoryId, q.categoryId());
        if (q.sellerId() != null) qw.eq(Product::getSellerId, q.sellerId());
        if (q.conditionLevel() != null && !q.conditionLevel().isBlank()) {
            List<String> conditionCandidates = conditionQueryCandidates(q.conditionLevel());
            if (!conditionCandidates.isEmpty()) {
                if (conditionCandidates.size() == 1) {
                    qw.eq(Product::getConditionLevel, conditionCandidates.get(0));
                } else {
                    qw.in(Product::getConditionLevel, conditionCandidates);
                }
            }
        }
        if (q.functionStatus() != null) qw.eq(Product::getFunctionStatus, q.functionStatus());
        if (q.region() != null && !q.region().isBlank()) qw.eq(Product::getRegion, q.region().trim());
        if (q.tradeMethod() != null && !q.tradeMethod().isBlank()) qw.like(Product::getTradeMethods, q.tradeMethod());
        if (q.minPrice() != null) qw.ge(Product::getPrice, q.minPrice());
        if (q.maxPrice() != null) qw.le(Product::getPrice, q.maxPrice());
        applySorting(qw, q.sortBy());

        Page<Product> page = productMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        if (viewerId != null && viewerId > 0) {
            boolean hasKeyword = q.keyword() != null && !q.keyword().isBlank();
            String eventType = hasKeyword ? "SEARCH_EXPOSE" : "LIST_EXPOSE";
            double eventScore = hasKeyword ? searchExposeScore(q.keyword()) : 0.3;
            page.getRecords().stream()
                    .limit(20)
                    .forEach(p -> recommendTraceService.recordEvent(viewerId, p.getId(), eventType, eventScore));
        }
        List<ProductDto> list = page.getRecords().stream().map(this::toDto).toList();
        return new PageResult<>(list, page.getTotal(), pageNum, pageSize, (int) page.getPages());
    }

    public void onShelf(Long sellerId, Long productId) {
        Product old = checkOwner(sellerId, productId);
        if ("SOLD".equals(old.getStatus()) || Boolean.TRUE.equals(old.getSold())) {
            throw new BizException(400, "已售商品不可上架");
        }
        if ("ON_SHELF".equals(old.getStatus())) {
            return;
        }
        long onShelfCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, sellerId)
                .eq(Product::getStatus, "ON_SHELF"));
        if (onShelfCount >= 50) {
            throw new BizException(400, "最多同时上架50件商品");
        }
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .set(Product::getStatus, "ON_SHELF")
                .set(Product::getRejectReason, null));
    }

    public void offShelf(Long sellerId, Long productId) {
        checkOwner(sellerId, productId);
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .set(Product::getStatus, "OFF_SHELF"));
    }

    public void adminApprove(Long id) {
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id)
                .set(Product::getStatus, "ON_SHELF")
                .set(Product::getRejectReason, null));
    }

    public void adminReject(Long id, String reason) {
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id)
                .set(Product::getStatus, "REJECTED")
                .set(Product::getRejectReason, reason));
    }

    public List<ProductDto> adminReviewList(String status, String keyword) {
        String targetStatus = (status == null || status.isBlank()) ? "PENDING_REVIEW" : status.toUpperCase();
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, targetStatus)
                .orderByDesc(Product::getId);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Product::getTitle, keyword)
                    .or().like(Product::getBrand, keyword)
                    .or().like(Product::getModel, keyword));
        }
        return productMapper.selectList(qw).stream().map(this::toDto).toList();
    }

    private Product checkOwner(Long sellerId, Long productId) {
        Product old = productMapper.selectById(productId);
        if (old == null) {
            throw new BizException(404, "商品不存在");
        }
        if (!old.getSellerId().equals(sellerId)) {
            throw new BizException(403, "无权操作");
        }
        return old;
    }

    private void applySorting(LambdaQueryWrapper<Product> qw, String sortBy) {
        String key = sortBy == null ? "COMPREHENSIVE" : sortBy.toUpperCase();
        switch (key) {
            case "PRICE_ASC" -> qw.orderByAsc(Product::getPrice);
            case "PRICE_DESC" -> qw.orderByDesc(Product::getPrice);
            case "LATEST" -> qw.orderByDesc(Product::getId);
            case "SALES_DESC" -> qw.orderByDesc(Product::getSalesCount).orderByDesc(Product::getId);
            default -> qw.orderByDesc(Product::getId);
        }
    }

    private void saveImages(Long productId, List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        for (int i = 0; i < images.size(); i++) {
            ProductImage img = new ProductImage();
            img.setProductId(productId);
            img.setImageUrl(images.get(i));
            img.setSort(i);
            productImageMapper.insert(img);
        }
    }

    private String normalizeConditionLevel(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return value;
        }
        String upper = value.toUpperCase();
        return switch (upper) {
            case "NEW", "全新" -> "NEW";
            case "LIKE_NEW", "99新", "95新", "9.5成新" -> "LIKE_NEW";
            case "GOOD", "9成新" -> "GOOD";
            case "FAIR", "8成新及以下", "8成新以下", "7成新及以下" -> "FAIR";
            default -> value;
        };
    }

    private List<String> conditionQueryCandidates(String raw) {
        String normalized = normalizeConditionLevel(raw);
        if (normalized == null || normalized.isBlank()) {
            return List.of();
        }
        return switch (normalized) {
            case "NEW" -> List.of("NEW", "全新");
            case "LIKE_NEW" -> List.of("LIKE_NEW", "99新", "95新", "9.5成新");
            case "GOOD" -> List.of("GOOD", "9成新");
            case "FAIR" -> List.of("FAIR", "8成新及以下", "8成新以下", "7成新及以下");
            default -> List.of(normalized);
        };
    }

    private double searchExposeScore(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return 0.8;
        }
        int len = keyword.trim().length();
        double score = 0.8 + Math.min(0.6, len * 0.06);
        return Math.min(1.4, score);
    }

    private String displayConditionLevel(String stored) {
        String normalized = normalizeConditionLevel(stored);
        if (normalized == null || normalized.isBlank()) {
            return stored;
        }
        return switch (normalized) {
            case "NEW" -> "全新";
            case "LIKE_NEW" -> "95新";
            case "GOOD" -> "9成新";
            case "FAIR" -> "8成新及以下";
            default -> stored;
        };
    }

    private double sellerScoreValue(SysUser seller) {
        if (seller == null || seller.getSellerScore() == null) {
            return 5.0;
        }
        BigDecimal score = seller.getSellerScore();
        return score.doubleValue();
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

    private String sellerAuthStatusValue(SysUser seller) {
        if (seller == null || seller.getAuthStatus() == null || seller.getAuthStatus().isBlank()) {
            return "NOT_SUBMITTED";
        }
        return seller.getAuthStatus();
    }
}
