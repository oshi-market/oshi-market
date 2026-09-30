package com.oshimarket.domain.work.dto;

import com.oshimarket.domain.work.entity.Work;

public record WorkResponse(
        Long id,
        String name
) {

    public static WorkResponse from(Work work) {
        return new WorkResponse(work.getId(), work.getName());
    }
}
