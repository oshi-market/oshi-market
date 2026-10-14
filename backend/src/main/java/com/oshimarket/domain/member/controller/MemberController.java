package com.oshimarket.domain.member.controller;

import com.oshimarket.domain.member.dto.MemberPublicResponse;
import com.oshimarket.domain.member.dto.MemberResponse;
import com.oshimarket.domain.member.dto.MemberUpdateRequest;
import com.oshimarket.domain.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PatchMapping("/me")
    public MemberResponse updateMe(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MemberUpdateRequest request
    ) {
        return memberService.updateProfile(memberId, request);
    }

    /** 판매자 공개 프로필. 숫자 id만 받아서 /me와 겹치지 않게 한다 (SecurityConfig에서 로그인 없이 허용). */
    @GetMapping("/{memberId:\\d+}")
    public MemberPublicResponse getPublicProfile(@PathVariable Long memberId) {
        return memberService.getPublicProfile(memberId);
    }
}
