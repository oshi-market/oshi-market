import { LEVEL_TITLE } from './constants';

/** "Lv.3 입덕 완료 · ★4.8 (12)" 한 줄 요약. 상품 상세·프로필에서 판매자 옆에 표시. */
export function SellerLevelBadge({ sellerLevel }) {
  if (!sellerLevel) return null;
  return (
    <span className="seller-level">
      <span className="seller-level-badge">
        Lv.{sellerLevel.level} {LEVEL_TITLE[sellerLevel.level]}
      </span>
      <span className="seller-level-rating">
        {sellerLevel.averageRating != null
          ? `★${sellerLevel.averageRating.toFixed(1)} (${sellerLevel.reviewCount})`
          : '받은 별점 없음'}
      </span>
    </span>
  );
}

/** 프로필 경험치 바. 현재 레벨 시작 경험치 → 다음 레벨 경험치 사이에서 얼마나 왔는지 보여준다. */
export function ExpBar({ sellerLevel }) {
  if (!sellerLevel) return null;
  const { exp, currentLevelExp, nextLevelExp } = sellerLevel;
  const isMax = nextLevelExp == null;
  const percent = isMax ? 100 : Math.round(((exp - currentLevelExp) / (nextLevelExp - currentLevelExp)) * 100);

  return (
    <div className="exp-bar">
      <div
        className="exp-bar-track"
        role="progressbar"
        aria-label="다음 레벨까지 경험치"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={percent}
      >
        <div className="exp-bar-fill" style={{ width: `${percent}%` }} />
      </div>
      <p className="exp-bar-label muted">
        {isMax ? `EXP ${exp} · 최고 레벨 달성!` : `EXP ${exp} / ${nextLevelExp} · 다음 레벨까지 ${nextLevelExp - exp}`}
      </p>
    </div>
  );
}
