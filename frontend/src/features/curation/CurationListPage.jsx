import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { fetchWorks } from '../item/api';
import WorkCombobox from '../item/WorkCombobox';
import CurationNotice from './CurationNotice';
import { fetchCurations } from './api';

function CurationListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [curations, setCurations] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const [works, setWorks] = useState([]);

  const work = searchParams.get('work') ?? '';
  const character = searchParams.get('character') ?? '';
  // 작품 입력칸은 드롭다운(WorkCombobox)이라 제어 컴포넌트로 둔다. 상품 상세에서 ?work=로 들어오면 그 값으로 채움
  const [workInput, setWorkInput] = useState(work);
  const [prevWork, setPrevWork] = useState(work);
  if (work !== prevWork) {
    setPrevWork(work);
    setWorkInput(work);
  }

  useEffect(() => {
    fetchWorks()
      .then(({ data }) => setWorks(data))
      // 작품 목록을 못 불러와도 직접 입력으로 필터는 가능하니 조용히 넘어간다
      .catch(() => {});
  }, []);

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
    const nextWork = workInput.trim();
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
        <WorkCombobox
          value={workInput}
          onChange={setWorkInput}
          options={works.map((w) => w.name)}
          placeholder="작품명 검색"
        />
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
          <Link
            to={`/curations/${curation.id}`}
            key={curation.id}
            className={`curation-card${curation.ended ? ' is-ended' : ''}`}
          >
            <p className="curation-card-store">
              {curation.storeName}
              {curation.ended && <span className="badge badge-outline">판매 종료</span>}
            </p>
            <p className="muted">
              {curation.workTag}
              {curation.characterTag && ` · ${curation.characterTag}`}
            </p>
          </Link>
        ))}
      </div>

      <CurationNotice />
    </div>
  );
}

export default CurationListPage;
