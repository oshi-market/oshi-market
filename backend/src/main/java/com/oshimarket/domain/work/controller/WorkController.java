package com.oshimarket.domain.work.controller;

import com.oshimarket.domain.work.dto.WorkResponse;
import com.oshimarket.domain.work.service.WorkService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 작품 목록 조회. 상품 검색 필터에서도 쓰이므로 비로그인 허용 (SecurityConfig). */
@RestController
@RequestMapping("/api/works")
public class WorkController {

    private final WorkService workService;

    public WorkController(WorkService workService) {
        this.workService = workService;
    }

    @GetMapping
    public List<WorkResponse> getWorks() {
        return workService.getWorks();
    }
}
