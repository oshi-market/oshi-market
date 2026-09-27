package com.oshimarket.domain.member.controller;

import com.oshimarket.domain.member.dto.MemberResponse;
import com.oshimarket.domain.member.service.MemberService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PATCH /api/members/me (프로필 수정)는 1_MVP기획서.md 기준 부가 기능이라
 * 이번 브랜치 범위에서 제외 — 2차 개발(프로필 조회/수정, 미배정)에서 다룸.
 */
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/me")
    public MemberResponse getMe(@AuthenticationPrincipal Long memberId) {
        return memberService.getMe(memberId);
    }
}
