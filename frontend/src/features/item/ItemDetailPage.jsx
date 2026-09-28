import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { useAuth } from '../member/useAuth';
import { deleteItem, fetchItem } from './api';
import { CONDITION_LABEL, STATUS_LABEL } from './constants';

function ItemDetailPage() {
  const { itemId } = useParams();
  const { member, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [item, setItem] = useState(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setError('');

    fetchItem(itemId)
      .then(({ data }) => {
        if (!cancelled) setItem(data);
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
    try {
      await deleteItem(itemId);
      navigate('/items');
    } catch (err) {
      setError(getErrorMessage(err, '삭제에 실패했습니다.'));
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

  return (
    <div className="page item-detail-page">
      <div className="item-detail-thumb" aria-hidden="true">
        🖼️
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
          {item.category}
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
      </div>
    </div>
  );
}

export default ItemDetailPage;
