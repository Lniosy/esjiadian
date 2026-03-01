package com.lniosy.usedappliance.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdminStatsMapper {
    @Select("""
            SELECT DATE(created_at) AS d, COUNT(*) AS c
            FROM sys_user
            WHERE created_at >= #{from}
            GROUP BY DATE(created_at)
            """)
    List<Map<String, Object>> userDaily(@Param("from") LocalDateTime from);

    @Select("""
            SELECT DATE(created_at) AS d, COUNT(*) AS c
            FROM product
            WHERE created_at >= #{from}
            GROUP BY DATE(created_at)
            """)
    List<Map<String, Object>> productDaily(@Param("from") LocalDateTime from);

    @Select("""
            SELECT DATE(created_at) AS d, COUNT(*) AS c
            FROM order_info
            WHERE created_at >= #{from}
            GROUP BY DATE(created_at)
            """)
    List<Map<String, Object>> orderDaily(@Param("from") LocalDateTime from);

    @Select("""
            SELECT DATE(paid_at) AS d, COALESCE(SUM(paid_amount), 0) AS c
            FROM order_info
            WHERE paid_at IS NOT NULL AND paid_at >= #{from}
            GROUP BY DATE(paid_at)
            """)
    List<Map<String, Object>> gmvDaily(@Param("from") LocalDateTime from);
}
