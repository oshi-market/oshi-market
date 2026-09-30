import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { fetchItems } from './api';
import { CONDITION_LABEL, STATUS_LABEL } from './constants';

const EMPTY_PAGE = { content: [], number: 0, totalPages: 0 };

function ItemListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(EMPTY_PAGE);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const keyword = searchParams.get('keyword') ?? '';
  const pageNumber = Number(searchParams.get('page') ?? 0);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setError('');

    fetchItems({ keyword: keyword || undefined, page: pageNumber, size: 20 })
      .then(({ data }) => {
        if (!cancelled) setPage(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err, '상품 목록을 불러오지 못했습니다.'));
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [keyword, pageNumber]);

  function goToPage(next) {
    const params = new URLSearchParams(searchParams);
    params.set('page', String(next));
    setSearchParams(params);
  }

  return (
    <div className="page">
      <div className="page-header">
        <h2>{keyword ? `"${keyword}" 검색 결과` : '상품 둘러보기'}</h2>
        <Link to="/items/new" className="btn btn-primary btn-sm">
          상품 등록
        </Link>
      </div>

      {isLoading && <p className="muted">불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}
      {!isLoading && !error && page.content.length === 0 && (
        <p className="muted">조건에 맞는 상품이 없습니다.</p>
      )}

      <div className="item-grid">
        {page.content.map((item) => (
          <Link to={`/items/${item.id}`} key={item.id} className="item-card">
            <div className="item-card-thumb" aria-hidden="true">
              🖼️
            </div>
            <div className="item-card-body">
              <p className="item-card-title">{item.title}</p>
              <p className="item-card-price">{item.price.toLocaleString()}원</p>
              <div className="item-card-meta">
                <span className={`badge badge-status-${item.status.toLowerCase()}`}>
                  {STATUS_LABEL[item.status]}
                </span>
                <span className="badge badge-outline">{CONDITION_LABEL[item.condition]}</span>
              </div>
            </div>
          </Link>
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

export default ItemListPage;
