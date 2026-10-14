import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { useAuth } from '../member/useAuth';
import { fetchItems } from './api';
import ItemCard from './ItemCard';

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
          <ItemCard key={item.id} item={item} />
        ))}
      </div>
    </div>
  );
}

export default MyItemsPage;
