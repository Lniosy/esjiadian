package com.lniosy.usedappliance.common;

import java.util.List;

public record PageResult<T>(List<T> list, long total, int pageNum, int pageSize, int pages) {
}
