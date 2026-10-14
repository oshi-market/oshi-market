package com.oshimarket.domain.member.repository;

import com.oshimarket.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    /**
     * 별점 하나를 판매자에게 반영. 읽고-계산하고-저장하면 두 구매자가 동시에 별점을 줄 때 한쪽이 덮어써서
     * 사라질 수 있어서(lost update), DB가 현재 값에 바로 더하는 UPDATE 한 번으로 처리한다.
     * 경험치는 깎여도 0 아래로 내려가지 않는다 (ck_user_account_exp_non_negative).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Member m SET m.exp = GREATEST(m.exp + :expDelta, 0), "
            + "m.reviewCount = m.reviewCount + 1, m.ratingSum = m.ratingSum + :rating WHERE m.id = :memberId")
    int applyReview(@Param("memberId") Long memberId, @Param("rating") int rating, @Param("expDelta") int expDelta);
}
