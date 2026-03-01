package com.lniosy.usedappliance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lniosy.usedappliance.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
