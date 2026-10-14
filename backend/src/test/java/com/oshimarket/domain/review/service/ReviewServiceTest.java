package com.oshimarket.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oshimarket.domain.member.repository.MemberRepository;
import com.oshimarket.domain.review.dto.ReviewCreateRequest;
import com.oshimarket.domain.review.dto.ReviewResponse;
import com.oshimarket.domain.review.entity.Review;
import com.oshimarket.domain.review.repository.ReviewRepository;
import com.oshimarket.domain.transaction.entity.Transaction;
import com.oshimarket.domain.transaction.entity.TransactionStatus;
import com.oshimarket.domain.transaction.repository.TransactionRepository;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ReviewServiceTest {

    private static final Long TRANSACTION_ID = 1L;
    private static final Long BUYER_ID = 10L;
    private static final Long SELLER_ID = 20L;

    private ReviewRepository reviewRepository;
    private TransactionRepository transactionRepository;
    private MemberRepository memberRepository;
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        memberRepository = mock(MemberRepository.class);
        reviewService = new ReviewService(reviewRepository, transactionRepository, memberRepository);
    }

    @Test
    void 완료된_거래의_구매자가_별점을_주면_저장하고_판매자_경험치를_반영한다() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(completedTransaction()));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.create(TRANSACTION_ID, BUYER_ID, new ReviewCreateRequest(5));

        assertThat(response.rating()).isEqualTo(5);
        // ★5 = +10 EXP가 거래의 판매자에게 반영
        verify(memberRepository).applyReview(SELLER_ID, 5, 10);
    }

    @Test
    void 낮은_별점은_경험치를_깎는다() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(completedTransaction()));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reviewService.create(TRANSACTION_ID, BUYER_ID, new ReviewCreateRequest(1));

        verify(memberRepository).applyReview(SELLER_ID, 1, -5);
    }

    @Test
    void 판매자는_별점을_남길_수_없다() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(completedTransaction()));

        assertErrorCode(() -> reviewService.create(TRANSACTION_ID, SELLER_ID, new ReviewCreateRequest(5)),
                ErrorCode.REVIEW_FORBIDDEN);
        verify(memberRepository, never()).applyReview(anyLong(), anyInt(), anyInt());
    }

    @Test
    void 완료되지_않은_거래에는_별점을_남길_수_없다() {
        Transaction inProgress = withId(Transaction.request(100L, 200L, BUYER_ID, SELLER_ID), TRANSACTION_ID);
        inProgress.changeStatus(TransactionStatus.IN_PROGRESS);
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(inProgress));

        assertErrorCode(() -> reviewService.create(TRANSACTION_ID, BUYER_ID, new ReviewCreateRequest(5)),
                ErrorCode.REVIEW_NOT_COMPLETED);
    }

    @Test
    void 이미_별점을_남긴_거래는_다시_남길_수_없다() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(completedTransaction()));
        when(reviewRepository.existsByTransactionId(TRANSACTION_ID)).thenReturn(true);

        assertErrorCode(() -> reviewService.create(TRANSACTION_ID, BUYER_ID, new ReviewCreateRequest(3)),
                ErrorCode.REVIEW_ALREADY_EXISTS);
        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void 동시에_두_번_눌러_UNIQUE에_걸리면_경험치는_반영하지_않는다() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(completedTransaction()));
        when(reviewRepository.saveAndFlush(any(Review.class)))
                .thenThrow(new DataIntegrityViolationException("uk_review_transaction"));

        assertErrorCode(() -> reviewService.create(TRANSACTION_ID, BUYER_ID, new ReviewCreateRequest(5)),
                ErrorCode.REVIEW_ALREADY_EXISTS);
        verify(memberRepository, never()).applyReview(anyLong(), anyInt(), anyInt());
    }

    @Test
    void 없는_거래면_예외가_발생한다() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        assertErrorCode(() -> reviewService.create(TRANSACTION_ID, BUYER_ID, new ReviewCreateRequest(5)),
                ErrorCode.TRANSACTION_NOT_FOUND);
    }

    private static void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private static Transaction completedTransaction() {
        Transaction transaction = withId(Transaction.request(100L, 200L, BUYER_ID, SELLER_ID), TRANSACTION_ID);
        transaction.changeStatus(TransactionStatus.IN_PROGRESS);
        transaction.changeStatus(TransactionStatus.COMPLETED);
        return transaction;
    }

    private static Transaction withId(Transaction transaction, Long id) {
        try {
            Field idField = Transaction.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(transaction, id);
            return transaction;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
