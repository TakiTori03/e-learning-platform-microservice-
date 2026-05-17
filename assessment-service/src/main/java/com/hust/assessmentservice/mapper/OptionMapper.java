package com.hust.assessmentservice.mapper;

import com.hust.assessmentservice.dto.request.OptionRequest;
import com.hust.assessmentservice.dto.response.OptionResponse;
import com.hust.assessmentservice.entity.Option;
import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class)
public interface OptionMapper extends BaseMapper<Option, OptionRequest, OptionResponse> {
}
