package com.oshimarket.domain.curation.controller;

import com.oshimarket.domain.curation.dto.CurationResponse;
import com.oshimarket.domain.curation.dto.CurationSearchCondition;
import com.oshimarket.domain.curation.service.CurationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 조회 전용 API. 등록/수정은 MVP 범위에 없음 (1_MVP기획서.md, 3_아키텍처및서비스흐름.md 참고). */
@RestController
@RequestMapping("/api/curations")
public class CurationController {

    private final CurationService curationService;

    public CurationController(CurationService curationService) {
        this.curationService = curationService;
    }

    @GetMapping
    public List<CurationResponse> search(@ModelAttribute CurationSearchCondition condition) {
        return curationService.search(condition);
    }

    @GetMapping("/{curationId}")
    public CurationResponse getCuration(@PathVariable Long curationId) {
        return curationService.getCuration(curationId);
    }
}
