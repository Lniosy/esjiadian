package com.lniosy.usedappliance.dto.category;

import java.util.List;

public record CategoryOptionDto(
        Long id,
        Long parentId,
        String name,
        Integer sort,
        List<CategoryOptionDto> children
) {
}
