package com.oshimarket.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class MemberServiceTest {

    private MemberRepository memberRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        memberService = new MemberService(memberRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void signup_성공하면_인코딩된_비밀번호로_저장한다() {
        SignupRequest request = new SignupRequest("test@example.com", "password123", "닉네임");
        when(memberRepository.existsByEmail(request.email())).thenReturn(false);
        when(memberRepository.existsByNickname(request.nickname())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));

        MemberResponse response = memberService.signup(request);

        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.nickname()).isEqualTo(request.nickname());
        assertThat(response.sellerLevel().level()).isEqualTo(1);
        assertThat(response.sellerLevel().exp()).isZero();
        assertThat(response.sellerLevel().averageRating()).isNull();
    }

    @Test
    void signup_이메일이_중복이면_예외가_발생한다() {
        SignupRequest request = new SignupRequest("dup@example.com", "password123", "닉네임");
        when(memberRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

        verify(memberRepository, never()).save(any());
    }

    @Test
    void signup_닉네임이_중복이면_예외가_발생한다() {
        SignupRequest request = new SignupRequest("test@example.com", "password123", "중복닉네임");
        when(memberRepository.existsByEmail(request.email())).thenReturn(false);
        when(memberRepository.existsByNickname(request.nickname())).thenReturn(true);

        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);

        verify(memberRepository, never()).save(any());
    }

    @Test
    void login_이메일과_비밀번호가_일치하면_토큰을_발급한다() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        Member member = withId(Member.of(request.email(), "encoded-password", "닉네임"), 1L);
        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(request.password(), member.getPassword())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(member.getId(), member.getEmail())).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3_600_000L);

        TokenResponse response = memberService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMs()).isEqualTo(3_600_000L);
    }

    @Test
    void signup_이메일은_공백을_지우고_소문자로_저장한다() {
        SignupRequest request = new SignupRequest("  Test@Example.COM ", "password123", "닉네임");
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));

        MemberResponse response = memberService.signup(request);

        assertThat(response.email()).isEqualTo("test@example.com");
        // 중복 검사도 정규화된 값으로 해서 Test@ / test@ 를 같은 이메일로 본다
        verify(memberRepository).existsByEmail("test@example.com");
    }

    @Test
    void signup_대소문자만_다른_이메일이_이미_있으면_예외가_발생한다() {
        SignupRequest request = new SignupRequest("DUP@example.com", "password123", "닉네임");
        when(memberRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void login_대소문자가_달라도_같은_계정으로_로그인된다() {
        LoginRequest request = new LoginRequest(" TEST@example.com", "password123");
        Member member = withId(Member.of("test@example.com", "encoded-password", "닉네임"), 1L);
        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(1L, "test@example.com")).thenReturn("access-token");

        assertThat(memberService.login(request).accessToken()).isEqualTo("access-token");
    }

    @Test
    void login_존재하지_않는_이메일이면_예외가_발생한다() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password123");
        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void login_비밀번호가_틀리면_예외가_발생한다() {
        LoginRequest request = new LoginRequest("test@example.com", "wrong-password");
        Member member = withId(Member.of(request.email(), "encoded-password", "닉네임"), 1L);
        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void getMe_존재하는_회원이면_정보를_반환한다() {
        Member member = withId(Member.of("test@example.com", "encoded-password", "닉네임"), 1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberResponse response = memberService.getMe(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@example.com");
    }

    @Test
    void getMe_존재하지_않는_회원이면_예외가_발생한다() {
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMe(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void getPublicProfile_닉네임과_가입일만_반환한다() {
        Member member = withId(Member.of("test@example.com", "encoded-password", "닉네임"), 1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberPublicResponse response = memberService.getPublicProfile(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nickname()).isEqualTo("닉네임");
        // 로그인 없이 공개되는 응답이라 이메일 필드 자체가 없어야 한다
        assertThat(MemberPublicResponse.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly("id", "nickname", "createdAt", "sellerLevel");
    }

    @Test
    void getPublicProfile_존재하지_않는_회원이면_예외가_발생한다() {
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getPublicProfile(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void updateProfile_존재하는_회원이면_닉네임이_변경된다() {
        Member member = withId(Member.of("test@example.com", "encoded-password", "기존닉네임"), 1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname("새닉네임")).thenReturn(false);

        MemberResponse response = memberService.updateProfile(1L, new MemberUpdateRequest("새닉네임"));

        assertThat(response.nickname()).isEqualTo("새닉네임");
    }

    @Test
    void updateProfile_닉네임을_바꾸지_않으면_중복_체크를_하지_않는다() {
        Member member = withId(Member.of("test@example.com", "encoded-password", "닉네임"), 1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberResponse response = memberService.updateProfile(1L, new MemberUpdateRequest("닉네임"));

        assertThat(response.nickname()).isEqualTo("닉네임");
        verify(memberRepository, never()).existsByNickname(anyString());
    }

    @Test
    void updateProfile_다른_사람이_쓰는_닉네임이면_예외가_발생한다() {
        Member member = withId(Member.of("test@example.com", "encoded-password", "기존닉네임"), 1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname("중복닉네임")).thenReturn(true);

        assertThatThrownBy(() -> memberService.updateProfile(1L, new MemberUpdateRequest("중복닉네임")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    void updateProfile_존재하지_않는_회원이면_예외가_발생한다() {
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.updateProfile(999L, new MemberUpdateRequest("닉네임")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    /** BIGSERIAL로 생성되는 id는 리플렉션으로 채워서 저장 이후 상태를 흉내낸다 (DB 없이 단위 테스트). */
    private static Member withId(Member member, Long id) {
        try {
            Field idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, id);
            return member;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
