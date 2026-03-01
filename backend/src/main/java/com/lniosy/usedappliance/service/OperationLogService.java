package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lniosy.usedappliance.entity.OperationLog;
import com.lniosy.usedappliance.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class OperationLogService {
    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void log(Long operatorId, String operatorRole, String action, String targetType, Object targetId, String detail) {
        OperationLog log = new OperationLog();
        log.setOperatorId(operatorId);
        log.setOperatorRole(operatorRole);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId == null ? null : String.valueOf(targetId));
        log.setDetail(detail);
        operationLogMapper.insert(log);
    }

    public List<Map<String, Object>> latest(Integer limit) {
        int size = limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
        return operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
                        .orderByDesc(OperationLog::getId)
                        .last("limit " + size))
                .stream()
                .map(log -> Map.<String, Object>of(
                        "id", log.getId(),
                        "operatorId", log.getOperatorId(),
                        "operatorRole", log.getOperatorRole(),
                        "action", log.getAction(),
                        "targetType", log.getTargetType(),
                        "targetId", log.getTargetId() == null ? "" : log.getTargetId(),
                        "detail", log.getDetail() == null ? "" : log.getDetail(),
                        "createdAt", log.getCreatedAt() == null ? 0L :
                                log.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                ))
                .toList();
    }
}
