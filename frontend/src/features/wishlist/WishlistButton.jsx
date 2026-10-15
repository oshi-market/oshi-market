import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { useAuth } from '../member/useAuth';
import { addWishlist, fetchWishlistStatus, removeWishlist } from './api';

/** 하트 아이콘. 찜하면 채우고, 안 했으면 테두리만. 색은 버튼의 color를 따라간다. */
function HeartIcon({ filled }) {
  return (
    <svg className="wishlist-heart" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
      <path
        d="M12 20.5s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7.6a4.3 4.3 0 0 1 7.5 2.7c0 5.6-7.5 10.2-7.5 10.2Z"
        fill={filled ? 'currentColor' : 'none'}
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
    </svg>
  );
}

/**
 * 상품 상세의 하트 버튼. 판매자 본인에게는 버튼 대신 찜 수만 보여준다(서버도 본인 상품 찜을 막음).
 * 찜 상태를 못 불러와도 상품 상세는 그대로 보여주고 버튼만 숨긴다.
 */
function WishlistButton({ itemId, isOwner }) {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [status, setStatus] = useState(null);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');

  // 로그인 상태가 바뀌면 "내가 찜했는지"가 달라지므로 다시 불러온다
  useEffect(() => {
    let cancelled = false;
    fetchWishlistStatus(itemId)
      .then(({ data }) => {
        if (!cancelled) setStatus(data);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [itemId, isAuthenticated]);

  async function handleToggle() {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } });
      return;
    }
    setError('');
    setIsSaving(true);
    try {
      const { data } = status.wishlisted ? await removeWishlist(itemId) : await addWishlist(itemId);
      setStatus(data);
    } catch (err) {
      setError(getErrorMessage(err, '찜하지 못했습니다.'));
    } finally {
      setIsSaving(false);
    }
  }

  if (!status) {
    return null;
  }

  if (isOwner) {
    return (
      <span className="muted wishlist-count">
        <HeartIcon filled={false} /> {status.count}명이 찜했어요
      </span>
    );
  }

  return (
    <>
      <button
        type="button"
        className={`btn btn-ghost btn-sm wishlist-button${status.wishlisted ? ' is-active' : ''}`}
        aria-pressed={status.wishlisted}
        onClick={handleToggle}
        disabled={isSaving}
      >
        {/* key가 바뀌면 아이콘이 새로 그려져서, 찜할 때마다 튀는 애니메이션이 다시 재생된다 */}
        <HeartIcon key={String(status.wishlisted)} filled={status.wishlisted} />
        찜 {status.count}
      </button>
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
    </>
  );
}

export default WishlistButton;
