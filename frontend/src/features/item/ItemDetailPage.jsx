import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { openChatRoom } from '../chat/api';
import { SellerLevelBadge } from '../member/SellerLevel';
import { fetchMemberProfile } from '../member/api';
import { useAuth } from '../member/useAuth';
import WishlistButton from '../wishlist/WishlistButton';
import { deleteItem, fetchItem } from './api';
import { CATEGORY_LABEL, CONDITION_LABEL, STATUS_LABEL } from './constants';
import { imageUrl } from './imageUrl';

function ItemDetailPage() {
  const { itemId } = useParams();
  const { member, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [item, setItem] = useState(null);
  const [error, setError] = useState('');
  const [deleteError, setDeleteError] = useState('');
  const [chatError, setChatError] = useState('');
  const [isOpeningChat, setIsOpeningChat] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [sellerLevel, setSellerLevel] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setError('');

    fetchItem(itemId)
      .then(({ data }) => {
        if (!cancelled) {
          setItem(data);
          setSelectedIndex(0);
        }
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err, '상품 정보를 불러오지 못했습니다.'));
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [itemId]);

  // 판매자 레벨·별점은 공개 프로필에서 가져온다. 못 불러와도 상품 상세는 그대로 보여준다
  const sellerId = item?.sellerId;
  useEffect(() => {
    if (sellerId == null) return undefined;
    let cancelled = false;
    fetchMemberProfile(sellerId)
      .then(({ data }) => {
        if (!cancelled) setSellerLevel(data.sellerLevel);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [sellerId]);

  async function handleDelete() {
    if (!window.confirm('이 상품을 삭제할까요?')) {
      return;
    }
    setDeleteError('');
    try {
      await deleteItem(itemId);
      navigate('/items');
    } catch (err) {
      // 상세 화면은 그대로 두고 버튼 옆에만 안내 (예: 채팅/거래가 진행된 상품은 409)
      setDeleteError(getErrorMessage(err, '삭제에 실패했습니다.'));
    }
  }

  // 이미 방이 있으면 그 방, 없으면 새 방 (두 번 눌러도 서버가 방 하나만 만든다)
  async function handleOpenChat() {
    if (!isAuthenticated) {
      // 로그인 후 이 상품으로 돌아오게 한다 (LoginPage가 state.from을 읽음, #15)
      navigate('/login', { state: { from: location } });
      return;
    }
    setChatError('');
    setIsOpeningChat(true);
    try {
      const { data } = await openChatRoom(itemId);
      navigate(`/chat/${data.id}`);
    } catch (err) {
      setChatError(getErrorMessage(err, '채팅방을 열지 못했습니다.'));
    } finally {
      setIsOpeningChat(false);
    }
  }

  if (isLoading) {
    return (
      <div className="page">
        <p className="muted">불러오는 중...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page">
        <p className="form-error">{error}</p>
      </div>
    );
  }

  if (!item) {
    return null;
  }

  const isOwner = isAuthenticated && member?.id === item.sellerId;
  // 거래 중이거나 판매 완료된 상품은 서버에서도 수정을 막는다 (ITEM_NOT_EDITABLE)
  const isEditable = item.status === 'SELLING';
  const images = item.images ?? [];
  const selectedImage = images[selectedIndex] ?? images[0];

  return (
    <div className="page item-detail-page">
      <div className="item-detail-gallery">
        <div className="item-detail-thumb">
          {selectedImage ? (
            <img src={imageUrl(selectedImage.url, 'detail')} alt={item.title} />
          ) : (
            <span aria-hidden="true">🖼️</span>
          )}
        </div>
        {images.length > 1 && (
          <div className="item-detail-thumbs">
            {images.map((image, index) => (
              <button
                type="button"
                key={image.id}
                className={`item-detail-thumbs-button${index === selectedIndex ? ' is-active' : ''}`}
                aria-label={`${index + 1}번째 사진 보기`}
                onClick={() => setSelectedIndex(index)}
              >
                <img src={imageUrl(image.url, 'mini')} alt="" />
              </button>
            ))}
          </div>
        )}
      </div>
      <div className="item-detail-body">
        <div className="item-detail-meta">
          <span className={`badge badge-status-${item.status.toLowerCase()}`}>
            {STATUS_LABEL[item.status]}
          </span>
          <span className="badge badge-outline">{CONDITION_LABEL[item.condition]}</span>
        </div>
        <h2>{item.title}</h2>
        <p className="item-detail-price">{item.price.toLocaleString()}원</p>
        <p className="item-detail-seller">
          판매자{' '}
          <Link to={`/members/${item.sellerId}`}>{item.sellerNickname ?? '알 수 없는 판매자'}</Link>{' '}
          <SellerLevelBadge sellerLevel={sellerLevel} />
        </p>
        <p className="muted">
          {CATEGORY_LABEL[item.category] ?? item.category}
          {item.workTag && ` · ${item.workTag}`}
          {item.characterTag && ` · ${item.characterTag}`}
        </p>
        {item.workTag && (
          <Link
            to={`/curations?work=${encodeURIComponent(item.workTag)}`}
            className="item-detail-curation-link"
          >
            {item.workTag} 공식 구매처 보기 →
          </Link>
        )}
        <p className="item-detail-description">{item.description || '설명이 없습니다.'}</p>

        {!isOwner && (
          <div className="item-detail-actions">
            <button type="button" className="btn btn-primary btn-sm" onClick={handleOpenChat} disabled={isOpeningChat}>
              채팅하기
            </button>
            <WishlistButton itemId={item.id} isOwner={false} />
          </div>
        )}
        {!isOwner && item.status === 'SOLD_OUT' && (
          <p className="muted">판매 완료된 상품이에요. 전에 나눈 채팅만 열 수 있어요.</p>
        )}
        {!isOwner && chatError && (
          <p className="form-error" role="alert">
            {chatError}
          </p>
        )}

        {isOwner && (
          <div className="item-detail-actions">
            <WishlistButton itemId={item.id} isOwner />
            {isEditable && (
              <Link to={`/items/${item.id}/edit`} className="btn btn-ghost btn-sm">
                수정
              </Link>
            )}
            <button type="button" className="btn btn-ghost btn-sm" onClick={handleDelete}>
              삭제
            </button>
          </div>
        )}
        {isOwner && !isEditable && (
          <p className="muted">거래 중이거나 판매 완료된 상품은 수정할 수 없습니다.</p>
        )}
        {isOwner && deleteError && (
          <p className="form-error" role="alert">
            {deleteError}
          </p>
        )}
      </div>
    </div>
  );
}

export default ItemDetailPage;
