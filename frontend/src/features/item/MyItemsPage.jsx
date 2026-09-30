import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { useAuth } from '../member/useAuth';
import { fetchItems } from './api';
import ItemThumb from './ItemThumb';
import { STATUS_LABEL } from './constants';

function MyItemsPage() {
  const { member } = useAuth();
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!member?.id) {
      return;
    }
    fetchItems({ sellerId: member.id, size: 50 })
      .then(({ data }) => setItems(data.content))
      .catch((err) => setError(getErrorMessage(err, '내 상품을 불러오지 못했습니다.')))
      .finally(() => setIsLoading(false));
  }, [member?.id]);

  return (
    <div className="page">
      <div className="page-header">
        <h2>내 상품</h2>
        <Link to="/items/new" className="btn btn-primary btn-sm">
          상품 등록
        </Link>
      </div>

      {isLoading && <p className="muted">불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}
      {!isLoading && !error && items.length === 0 && <p className="muted">등록한 상품이 없습니다.</p>}

      <div className="item-grid">
        {items.map((item) => (
          <Link to={`/items/${item.id}`} key={item.id} className="item-card">
            <ItemThumb url={item.thumbnailUrl} alt={item.title} />
            <div className="item-card-body">
              <p className="item-card-title">{item.title}</p>
              <p className="item-card-price">{item.price.toLocaleString()}원</p>
              <div className="item-card-meta">
                <span className={`badge badge-status-${item.status.toLowerCase()}`}>
                  {STATUS_LABEL[item.status]}
                </span>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}

export default MyItemsPage;
