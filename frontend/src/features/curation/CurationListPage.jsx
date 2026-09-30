import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { fetchCurations } from './api';

function CurationListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [curations, setCurations] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const work = searchParams.get('work') ?? '';
  const character = searchParams.get('character') ?? '';

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setError('');

    fetchCurations({ work: work || undefined, character: character || undefined })
      .then(({ data }) => {
        if (!cancelled) setCurations(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err, '구매 가이드를 불러오지 못했습니다.'));
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [work, character]);

  function handleFilterSubmit(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const params = new URLSearchParams();
    const nextWork = form.get('work')?.toString().trim();
    const nextCharacter = form.get('character')?.toString().trim();
    if (nextWork) params.set('work', nextWork);
    if (nextCharacter) params.set('character', nextCharacter);
    setSearchParams(params);
  }

  return (
    <div className="page">
      <div className="page-header">
        <h2>구매 가이드</h2>
      </div>

      <form className="curation-filter" onSubmit={handleFilterSubmit}>
        <input className="input" name="work" defaultValue={work} placeholder="작품명" />
        <input className="input" name="character" defaultValue={character} placeholder="캐릭터명" />
        <button type="submit" className="btn btn-ghost btn-sm">
          필터 적용
        </button>
      </form>

      {isLoading && <p className="muted">불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}
      {!isLoading && !error && curations.length === 0 && (
        <p className="muted">조건에 맞는 구매처가 없습니다.</p>
      )}

      <div className="curation-grid">
        {curations.map((curation) => (
          <Link to={`/curations/${curation.id}`} key={curation.id} className="curation-card">
            <p className="curation-card-store">{curation.storeName}</p>
            <p className="muted">
              {curation.workTag}
              {curation.characterTag && ` · ${curation.characterTag}`}
            </p>
          </Link>
        ))}
      </div>
    </div>
  );
}

export default CurationListPage;
