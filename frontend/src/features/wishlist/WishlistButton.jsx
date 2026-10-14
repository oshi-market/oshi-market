import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { useAuth } from '../member/useAuth';
import { addWishlist, fetchWishlistStatus, removeWishlist } from './api';

/**
 * 상품 상세의 ♡ 버튼. 판매자 본인에게는 버튼 대신 찜 수만 보여준다(서버도 본인 상품 찜을 막음).
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
    return <span className="muted wishlist-count">♡ {status.count}명이 찜했어요</span>;
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
        {status.wishlisted ? '♥' : '♡'} 찜 {status.count}
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
