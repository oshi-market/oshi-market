package com.oshimarket.domain.member.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {

    private static final String EMAIL = "test@example.com";

    private TestClock clock;
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new TestClock(Instant.parse("2026-11-09T00:00:00Z"));
        limiter = new LoginAttemptLimiter(clock);
    }

    @Test
    void 기준보다_적게_틀리면_막지_않는다() {
        failTimes(LoginAttemptLimiter.MAX_FAILURES - 1);

        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void 기준만큼_틀리면_막는다() {
        failTimes(LoginAttemptLimiter.MAX_FAILURES);

        assertThatThrownBy(() -> limiter.checkAllowed(EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
    }

    @Test
    void 첫_실패부터_기간이_지나면_다시_시도할_수_있다() {
        failTimes(LoginAttemptLimiter.MAX_FAILURES);

        clock.advance(LoginAttemptLimiter.WINDOW);

        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void 기간이_지난_뒤의_실패는_1회부터_다시_센다() {
        failTimes(LoginAttemptLimiter.MAX_FAILURES - 1);
        clock.advance(LoginAttemptLimiter.WINDOW);

        failTimes(LoginAttemptLimiter.MAX_FAILURES - 1);

        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void 로그인에_성공하면_실패_횟수가_초기화된다() {
        failTimes(LoginAttemptLimiter.MAX_FAILURES - 1);

        limiter.recordSuccess(EMAIL);
        failTimes(LoginAttemptLimiter.MAX_FAILURES - 1);

        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void 다른_이메일의_실패는_영향을_주지_않는다() {
        failTimes(LoginAttemptLimiter.MAX_FAILURES);

        assertThatCode(() -> limiter.checkAllowed("other@example.com")).doesNotThrowAnyException();
    }

    private void failTimes(int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(EMAIL);
        }
    }

    /** 테스트에서 시간을 직접 흘려보내기 위한 시계. */
    private static class TestClock extends Clock {

        private Instant now;

        TestClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
