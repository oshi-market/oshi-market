import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { fetchCuration } from './api';

function CurationDetailPage() {
  const { curationId } = useParams();
  const [curation, setCuration] = useState(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    fetchCuration(curationId)
      .then(({ data }) => {
        if (!cancelled) setCuration(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err, '구매처 정보를 불러오지 못했습니다.'));
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [curationId]);

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

  if (!curation) {
    return null;
  }

  return (
    <div className="page">
      <div className="card curation-detail-card">
        <p className="muted">
          {curation.workTag}
          {curation.characterTag && ` · ${curation.characterTag}`}
        </p>
        <h2>{curation.storeName}</h2>
        {(curation.periodStart || curation.periodEnd) && (
          <p className="muted">
            판매 기간: {curation.periodStart ?? '상시'} ~ {curation.periodEnd ?? '상시'}
          </p>
        )}
        <a href={curation.url} target="_blank" rel="noreferrer" className="btn btn-primary">
          구매하러 가기
        </a>
      </div>
    </div>
  );
}

export default CurationDetailPage;
