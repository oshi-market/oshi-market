import { useState } from 'react';
import { getErrorMessage } from '../../api/errors';
import { createReview } from './api';

const STAR_LABEL = { 1: '별로예요', 2: '아쉬워요', 3: '보통이에요', 4: '좋아요', 5: '최고예요' };

/**
 * 거래 완료 후 구매자가 판매자에게 별점. 한 번 등록하면 수정·삭제가 안 되니 등록 전에 주의사항을 꼭 보여준다.
 * 등록에 성공하면 onDone(rating)으로 목록의 해당 거래를 갱신한다.
 */
function ReviewForm({ transactionId, onDone, onCancel }) {
  const [rating, setRating] = useState(0);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit() {
    setError('');
    setIsSubmitting(true);
    try {
      await createReview(transactionId, rating);
      onDone(rating);
    } catch (err) {
      setError(getErrorMessage(err, '별점을 등록하지 못했습니다.'));
      setIsSubmitting(false);
    }
  }

  return (
    <div className="review-form">
      <p className="review-form-title">판매자와의 거래는 어떠셨나요?</p>
      <div className="review-stars" role="radiogroup" aria-label="별점">
        {[1, 2, 3, 4, 5].map((value) => (
          <button
            type="button"
            key={value}
            role="radio"
            aria-checked={rating === value}
            aria-label={`${value}점`}
            className={`review-star${value <= rating ? ' is-on' : ''}`}
            onClick={() => setRating(value)}
          >
            ★
          </button>
        ))}
        {rating > 0 && <span className="muted">{STAR_LABEL[rating]}</span>}
      </div>

      <div className="review-notice" role="note">
        <strong>⚠️ 별점을 남기기 전에 확인해 주세요</strong>
        <ul>
          <li>한 번 등록한 별점은 <b>수정하거나 삭제할 수 없어요.</b></li>
          <li>별점은 판매자의 경험치와 레벨에 바로 반영돼요. 낮은 별점(★1~2)은 경험치가 깎여요.</li>
          <li>실제 거래 경험을 바탕으로 신중하게 선택해 주세요.</li>
        </ul>
      </div>

      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      <div className="review-form-actions">
        <button type="button" className="btn btn-ghost btn-sm" onClick={onCancel} disabled={isSubmitting}>
          취소
        </button>
        <button
          type="button"
          className="btn btn-primary btn-sm"
          onClick={handleSubmit}
          disabled={rating === 0 || isSubmitting}
        >
          {rating > 0 ? `★${rating}점으로 등록` : '별점을 선택해 주세요'}
        </button>
      </div>
    </div>
  );
}

export default ReviewForm;
