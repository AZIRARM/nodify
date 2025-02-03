package com.itexpert.content.api.mappers;

import com.itexpert.content.lib.models.Feedback;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {
    Feedback fromEntity(com.itexpert.content.lib.entities.Feedback source);

    com.itexpert.content.lib.entities.Feedback fromModel(Feedback source);
}
