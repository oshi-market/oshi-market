import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import ItemCard from '../item/ItemCard';
import { fetchMyWishlist } from './api';

const EMPTY_PAGE = { content: [], number: 0, totalPages: 0, totalElements: 0 };

function MyWishlistPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(EMPTY_PAGE);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  const pageNumber = Number(searchParams.get('page') ?? 0);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setError('');

    fetchMyWishlist({ page: pageNumber, size: 20 })
      .then(({ data }) => {
        if (!cancelled) setPage(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err, '찜한 상품을 불러오지 못했습니다.'));
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [pageNumber]);

  function goToPage(next) {
    setSearchParams({ page: String(next) });
  }

  return (
    <div className="page">
      <div className="page-header">
        <h2>찜한 상품</h2>
        {!isLoading && !error && <span className="muted">{page.totalElements}개</span>}
      </div>

      {isLoading && <p className="muted">불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}
      {!isLoading && !error && page.content.length === 0 && (
        <p className="muted">
          아직 찜한 상품이 없습니다. <Link to="/items">상품 둘러보기 →</Link>
        </p>
      )}

      <div className="item-grid">
        {page.content.map((item) => (
          <ItemCard key={item.id} item={item} />
        ))}
      </div>

      {page.totalPages > 1 && (
        <div className="pagination">
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            disabled={page.number <= 0}
            onClick={() => goToPage(page.number - 1)}
          >
            이전
          </button>
          <span className="muted">
            {page.number + 1} / {page.totalPages}
          </span>
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            disabled={page.number >= page.totalPages - 1}
            onClick={() => goToPage(page.number + 1)}
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}

export default MyWishlistPage;
