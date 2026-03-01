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
import com.lniosy.usedappliance.entity.SysUser;
import com.lniosy.usedappliance.mapper.ProductImageMapper;
import com.lniosy.usedappliance.mapper.ProductMapper;
import com.lniosy.usedappliance.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final SysUserMapper sysUserMapper;

    public ProductService(ProductMapper productMapper, ProductImageMapper productImageMapper, SysUserMapper sysUserMapper) {
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.sysUserMapper = sysUserMapper;
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
        product.setConditionLevel(req.conditionLevel());
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
        old.setConditionLevel(req.conditionLevel());
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
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException(404, "商品不存在");
        }
        return toDto(p);
    }

    public PageResult<ProductDto> list(ProductQuery q) {
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
        if (q.conditionLevel() != null) qw.eq(Product::getConditionLevel, q.conditionLevel());
        if (q.functionStatus() != null) qw.eq(Product::getFunctionStatus, q.functionStatus());
        if (q.region() != null) qw.eq(Product::getRegion, q.region());
        if (q.tradeMethod() != null && !q.tradeMethod().isBlank()) qw.like(Product::getTradeMethods, q.tradeMethod());
        if (q.minPrice() != null) qw.ge(Product::getPrice, q.minPrice());
        if (q.maxPrice() != null) qw.le(Product::getPrice, q.maxPrice());
        applySorting(qw, q.sortBy());

        Page<Product> page = productMapper.selectPage(new Page<>(pageNum, pageSize), qw);
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

    private ProductDto toDto(Product p) {
        SysUser seller = sysUserMapper.selectById(p.getSellerId());
        List<String> images = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, p.getId())
                        .orderByAsc(ProductImage::getSort))
                .stream().map(ProductImage::getImageUrl).toList();
        return new ProductDto(p.getId(), p.getSellerId(),
                seller == null ? "未知用户" : seller.getNickname(),
                seller == null ? "" : seller.getAvatarUrl(),
                p.getTitle(), p.getCategoryId(), p.getBrand(), p.getModel(),
                p.getPurchaseDate(), p.getConditionLevel(), p.getFunctionStatus(), p.getRepairHistory(),
                p.getDescription(), p.getVideoUrl(), p.getPrice(), p.getOriginalPrice(),
                p.getRegion(), p.getTradeMethods(), p.getStatus(), p.getRejectReason(), p.getSold(), p.getSalesCount(), images);
    }
}
