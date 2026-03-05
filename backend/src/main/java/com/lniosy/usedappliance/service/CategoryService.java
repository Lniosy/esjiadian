package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lniosy.usedappliance.dto.category.CategoryOptionDto;
import com.lniosy.usedappliance.entity.Category;
import com.lniosy.usedappliance.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryOptionDto> tree() {
        List<Category> all = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
        if (all.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Category>> childrenByParent = all.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));
        List<Category> roots = childrenByParent.getOrDefault(0L, List.of()).stream()
                .sorted(order())
                .toList();
        return roots.stream()
                .map(root -> toTreeNode(root, childrenByParent))
                .toList();
    }

    public List<CategoryOptionDto> flat() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId))
                .stream()
                .map(c -> new CategoryOptionDto(c.getId(), c.getParentId(), c.getName(), c.getSort(), List.of()))
                .toList();
    }

    private CategoryOptionDto toTreeNode(Category current, Map<Long, List<Category>> childrenByParent) {
        List<CategoryOptionDto> children = childrenByParent.getOrDefault(current.getId(), List.of()).stream()
                .sorted(order())
                .map(child -> toTreeNode(child, childrenByParent))
                .toList();
        return new CategoryOptionDto(current.getId(), current.getParentId(), current.getName(), current.getSort(), children);
    }

    private Comparator<Category> order() {
        return Comparator.<Category>comparingInt(c -> c.getSort() == null ? 0 : c.getSort())
                .thenComparingLong(c -> c.getId() == null ? 0L : c.getId());
    }
}
