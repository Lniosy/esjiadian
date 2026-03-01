package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lniosy.usedappliance.dto.product.ProductDto;
import com.lniosy.usedappliance.entity.Product;
import com.lniosy.usedappliance.entity.ProductImage;
import com.lniosy.usedappliance.entity.SysUser;
import com.lniosy.usedappliance.mapper.ProductImageMapper;
import com.lniosy.usedappliance.mapper.ProductMapper;
import com.lniosy.usedappliance.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendService {
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final SysUserMapper sysUserMapper;

    public RecommendService(ProductMapper productMapper, ProductImageMapper productImageMapper, SysUserMapper sysUserMapper) {
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public List<ProductDto> home(Long userId) {
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, "ON_SHELF")
                        .orderByDesc(Product::getId)
                        .last("limit 12"))
                .stream().map(this::toDto).toList();
    }

    public List<ProductDto> related(Long productId) {
        Product current = productMapper.selectById(productId);
        if (current == null) {
            return List.of();
        }
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .ne(Product::getId, productId)
                        .eq(Product::getStatus, "ON_SHELF")
                        .and(w -> w.eq(Product::getBrand, current.getBrand())
                                .or().eq(Product::getCategoryId, current.getCategoryId()))
                        .last("limit 8"))
                .stream().map(this::toDto).toList();
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
