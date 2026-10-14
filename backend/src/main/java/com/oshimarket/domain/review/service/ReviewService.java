package com.oshimarket.domain.review.service;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 거래 후 별점. 구매자만, 완료된 거래에만, 거래당 한 번. 저장과 동시에 판매자 경험치·평균 별점을 반영한다.
 * (리뷰 저장과 경험치 반영은 한 트랜잭션이라 둘 중 하나만 반영되는 일은 없다)
 */
@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            TransactionRepository transactionRepository,
            MemberRepository memberRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.transactionRepository = transactionRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public ReviewResponse create(Long transactionId, Long memberId, ReviewCreateRequest request) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (!memberId.equals(transaction.getBuyerId())) {
            throw new BusinessException(ErrorCode.REVIEW_FORBIDDEN);
        }
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_COMPLETED);
        }
        if (reviewRepository.existsByTransactionId(transactionId)) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review;
        try {
            // 같은 거래에 두 요청이 동시에 들어와 위 검사를 함께 통과해도 UNIQUE(transaction_id)가 하나만 남긴다.
            // flush로 여기서 바로 INSERT해야 경험치 반영 전에 중복을 잡을 수 있다
            review = reviewRepository.saveAndFlush(Review.of(transactionId, memberId, request.rating()));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        memberRepository.applyReview(transaction.getSellerId(), review.getRating(), review.expDelta());
        return ReviewResponse.from(review);
    }
}
