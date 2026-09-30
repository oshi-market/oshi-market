import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { useAuth } from '../member/useAuth';
import { deleteItem, fetchItem } from './api';
import { CATEGORY_LABEL, CONDITION_LABEL, STATUS_LABEL } from './constants';
import { imageUrl } from './imageUrl';

function ItemDetailPage() {
  const { itemId } = useParams();
  const { member, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [item, setItem] = useState(null);
  const [error, setError] = useState('');
  const [deleteError, setDeleteError] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [selectedIndex, setSelectedIndex] = useState(0);

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
        <p className="muted">
          {CATEGORY_LABEL[item.category] ?? item.category}
          {item.workTag && ` · ${item.workTag}`}
          {item.characterTag && ` · ${item.characterTag}`}
        </p>
        <p className="item-detail-description">{item.description || '설명이 없습니다.'}</p>

        {isOwner && (
          <div className="item-detail-actions">
            <Link to={`/items/${item.id}/edit`} className="btn btn-ghost btn-sm">
              수정
            </Link>
            <button type="button" className="btn btn-ghost btn-sm" onClick={handleDelete}>
              삭제
            </button>
          </div>
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
