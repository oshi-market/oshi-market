package com.oshimarket.domain.member.service;

import com.oshimarket.domain.member.dto.LoginRequest;
import com.oshimarket.domain.member.dto.MemberPublicResponse;
import com.oshimarket.domain.member.dto.MemberResponse;
import com.oshimarket.domain.member.dto.MemberUpdateRequest;
import com.oshimarket.domain.member.dto.SignupRequest;
import com.oshimarket.domain.member.dto.TokenResponse;
import com.oshimarket.domain.member.entity.Member;
import com.oshimarket.domain.member.repository.MemberRepository;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import com.oshimarket.global.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

    static final int MAX_PASSWORD_BYTES = 72;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptLimiter loginAttemptLimiter;

    public MemberService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            LoginAttemptLimiter loginAttemptLimiter
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginAttemptLimiter = loginAttemptLimiter;
    }

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        // BCrypt는 72바이트까지만 처리해서 넘으면 encode가 예외를 던진다(500). 한글은 한 글자가 3바이트라
        // SignupRequest의 @Size(64자)만으로는 못 막아서 바이트 수를 따로 확인한다
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "비밀번호가 너무 깁니다. 한글만 쓰면 24자까지 입력할 수 있습니다.");
        }
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (memberRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        Member member = Member.of(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname()
        );

        return MemberResponse.from(memberRepository.save(member));
    }

    public TokenResponse login(LoginRequest request) {
        loginAttemptLimiter.checkAllowed(request.email());

        Member member = memberRepository.findByEmail(request.email()).orElse(null);
        // 없는 이메일과 틀린 비밀번호를 똑같이 실패로 센다 (가입 여부가 드러나지 않게)
        if (member == null || !passwordEncoder.matches(request.password(), member.getPassword())) {
            loginAttemptLimiter.recordFailure(request.email());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        loginAttemptLimiter.recordSuccess(request.email());

        String accessToken = jwtTokenProvider.createAccessToken(member.getId());
        return TokenResponse.of(accessToken, jwtTokenProvider.getAccessTokenExpirationMs());
    }

    public MemberResponse getMe(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return MemberResponse.from(member);
    }

    public MemberPublicResponse getPublicProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return MemberPublicResponse.from(member);
    }

    @Transactional
    public MemberResponse updateProfile(Long memberId, MemberUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        boolean nicknameChanged = !member.getNickname().equals(request.nickname());
        if (nicknameChanged && memberRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        member.updateNickname(request.nickname());
        return MemberResponse.from(member);
    }
}
