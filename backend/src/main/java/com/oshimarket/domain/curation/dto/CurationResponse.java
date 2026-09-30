package com.oshimarket.domain.curation.dto;

import com.oshimarket.domain.curation.entity.Curation;
import java.time.LocalDate;

public record CurationResponse(
        Long id,
        String workTag,
        String characterTag,
        String storeName,
        String url,
        LocalDate periodStart,
        LocalDate periodEnd
) {

    public static CurationResponse from(Curation curation) {
        return new CurationResponse(
                curation.getId(),
                curation.getWorkTag(),
                curation.getCharacterTag(),
                curation.getStoreName(),
                curation.getUrl(),
                curation.getPeriodStart(),
                curation.getPeriodEnd()
        );
    }
}
