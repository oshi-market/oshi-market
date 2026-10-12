import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { useAuth } from '../member/useAuth';
import { createItem, deleteItemImage, fetchItem, fetchWorks, updateItem, uploadItemImages } from './api';
import { CATEGORY_LABEL } from './constants';
import ItemImagePicker from './ItemImagePicker';
import WorkCombobox from './WorkCombobox';

const EMPTY_FORM = {
  title: '',
  description: '',
  category: '',
  workTag: '',
  characterTag: '',
  price: '',
  condition: 'NEW',
};

/**
 * 작성 중인 입력값 임시 저장(sessionStorage, 탭을 닫으면 사라짐). 저장 도중 로그인이 만료돼 로그인 화면으로
 * 이동해도 다시 돌아오면 이어서 쓸 수 있게 한다. 같은 탭에서 다른 계정으로 로그인한 경우를 막으려고
 * 작성자 memberId를 같이 저장한다. 사진 파일은 브라우저 보안상 저장할 수 없어 다시 선택해야 한다.
 */
function draftKey(itemId) {
  return `itemFormDraft:${itemId ?? 'new'}`;
}

function readDraft(key, memberId) {
  try {
    const draft = JSON.parse(sessionStorage.getItem(key));
    return draft && draft.memberId === memberId ? draft.form : null;
  } catch {
    return null;
  }
}

function saveDraft(key, memberId, form) {
  try {
    sessionStorage.setItem(key, JSON.stringify({ memberId, form }));
  } catch {
    // 저장소를 못 쓰는 환경(시크릿 모드 용량 제한 등)이면 임시 저장 없이 진행
  }
}

function removeDraft(key) {
  try {
    sessionStorage.removeItem(key);
  } catch {
    // 위와 같음
  }
}

function ItemFormPage() {
  const { itemId } = useParams();
  const isEdit = Boolean(itemId);
  const navigate = useNavigate();
  const location = useLocation();
  const { member } = useAuth();
  const key = draftKey(itemId);
  // 등록 화면은 처음부터 임시 저장본을 채우고, 수정 화면은 상품 정보를 불러온 뒤에 확인한다
  const [form, setForm] = useState(() => (isEdit ? null : readDraft(key, member?.id)) ?? EMPTY_FORM);
  const [isDraftRestored, setIsDraftRestored] = useState(() => !isEdit && readDraft(key, member?.id) !== null);
  // 사용자가 직접 입력했을 때만 임시 저장 (수정 화면을 열기만 해도 저장본이 생기지 않게)
  const [isDirty, setIsDirty] = useState(false);
  const originalFormRef = useRef(EMPTY_FORM);
  // 등록 화면에서 사진만 실패해 수정 화면으로 넘어온 경우 그 에러를 이어서 보여준다
  const [error, setError] = useState(location.state?.error ?? '');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(isEdit);
  // 수정 화면을 주소로 바로 연 경우에도 판매 중이 아닌 상품은 폼 대신 안내만 보여준다
  const [isLocked, setIsLocked] = useState(false);
  const [works, setWorks] = useState([]);
  const [existingImages, setExistingImages] = useState([]);
  const [removedImageIds, setRemovedImageIds] = useState([]);
  const [newFiles, setNewFiles] = useState([]);

  /** 작품 목록을 못 불러와도 폼은 쓸 수 있게 두고, 잘못된 작품명은 서버 검증(WORK_NOT_FOUND)에 맡긴다. */
  useEffect(() => {
    fetchWorks()
      .then(({ data }) => setWorks(data))
      .catch(() => setWorks([]));
  }, []);

  useEffect(() => {
    if (!isEdit) {
      return;
    }
    fetchItem(itemId)
      .then(({ data }) => {
        const original = {
          title: data.title,
          description: data.description ?? '',
          category: data.category,
          workTag: data.workTag ?? '',
          characterTag: data.characterTag ?? '',
          price: String(data.price),
          condition: data.condition,
        };
        originalFormRef.current = original;
        setIsLocked(data.status !== 'SELLING');
        const draft = readDraft(key, member?.id);
        setForm(draft ?? original);
        setIsDraftRestored(draft !== null);
        setExistingImages(data.images ?? []);
      })
      .catch((err) => setError(getErrorMessage(err, '상품 정보를 불러오지 못했습니다.')))
      .finally(() => setIsLoading(false));
    // 상품을 바꿔 열 때만 다시 불러온다 (key/member는 itemId가 같으면 바뀌지 않음)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [itemId, isEdit]);

  useEffect(() => {
    if (isDirty && member?.id) {
      saveDraft(key, member.id, form);
    }
  }, [form, isDirty, key, member?.id]);

  function updateForm(changes) {
    setForm((prev) => ({ ...prev, ...changes }));
    setIsDirty(true);
  }

  function handleChange(event) {
    const { name, value } = event.target;
    updateForm({ [name]: value });
  }

  function discardDraft() {
    removeDraft(key);
    setIsDirty(false);
    setIsDraftRestored(false);
    setForm(isEdit ? originalFormRef.current : EMPTY_FORM);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setIsSubmitting(true);

    const payload = { ...form, workTag: form.workTag.trim() || null, price: Number(form.price) };

    let savedId = itemId;
    try {
      if (isEdit) {
        await updateItem(itemId, payload);
      } else {
        const { data } = await createItem(payload);
        savedId = data.id;
      }
    } catch (err) {
      setError(getErrorMessage(err, '저장에 실패했습니다.'));
      setIsSubmitting(false);
      return;
    }

    // 상품 정보는 저장됐으므로 임시 저장본 정리 (사진 단계가 실패해도 다시 쓸 필요 없음)
    removeDraft(key);
    setIsDirty(false);

    // 상품 저장 후 사진 반영. 사진만 실패하면 상품은 이미 저장됐으므로 수정 화면으로 보내 다시 시도하게 한다
    // (등록 화면에 머물면 다시 누를 때 상품이 중복 등록됨)
    try {
      await Promise.all(removedImageIds.map((imageId) => deleteItemImage(savedId, imageId)));
      if (newFiles.length > 0) {
        await uploadItemImages(savedId, newFiles);
      }
      navigate(`/items/${savedId}`);
    } catch (err) {
      const message = `상품은 저장됐지만 사진 반영에 실패했습니다. ${getErrorMessage(err, '')}`.trim();
      setIsSubmitting(false);
      if (isEdit) {
        setError(message);
      } else {
        navigate(`/items/${savedId}/edit`, { replace: true, state: { error: message } });
      }
    }
  }

  if (isLoading) {
    return (
      <div className="page">
        <p className="muted">불러오는 중...</p>
      </div>
    );
  }

  if (isLocked) {
    return (
      <div className="page">
        <p className="form-error">거래 중이거나 판매 완료된 상품은 수정할 수 없습니다.</p>
        <Link to={`/items/${itemId}`}>상품으로 돌아가기</Link>
      </div>
    );
  }

  return (
    <div className="page item-form-page">
      <div className="card">
        <h2>{isEdit ? '상품 수정' : '상품 등록'}</h2>
        {isDraftRestored && (
          <div className="draft-notice">
            <p>작성 중이던 내용을 불러왔습니다. 사진은 다시 선택해주세요.</p>
            <button type="button" className="btn btn-ghost btn-sm" onClick={discardDraft}>
              {isEdit ? '원래 내용으로' : '새로 작성'}
            </button>
          </div>
        )}
        <form onSubmit={handleSubmit}>
          <label className="field">
            상품명
            <input
              className="input"
              name="title"
              value={form.title}
              onChange={handleChange}
              maxLength={100}
              required
            />
          </label>
          <label className="field">
            설명
            <textarea
              className="input"
              name="description"
              value={form.description}
              onChange={handleChange}
              rows={4}
              maxLength={2000}
            />
          </label>
          <label className="field">
            카테고리
            <select className="input" name="category" value={form.category} onChange={handleChange} required>
              <option value="" disabled>
                카테고리를 선택해주세요
              </option>
              {Object.entries(CATEGORY_LABEL).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <ItemImagePicker
            existingImages={existingImages}
            removedIds={removedImageIds}
            onRemoveExisting={(imageId) => setRemovedImageIds((prev) => [...prev, imageId])}
            newFiles={newFiles}
            onChangeNewFiles={setNewFiles}
            onError={setError}
          />
          <div className="field">
            <label htmlFor="item-work-tag">작품명</label>
            <WorkCombobox
              id="item-work-tag"
              value={form.workTag}
              onChange={(workTag) => updateForm({ workTag })}
              options={works.map((work) => work.name)}
              placeholder="작품명을 검색해서 선택해주세요 (선택)"
            />
          </div>
          <label className="field">
            캐릭터명
            <input
              className="input"
              name="characterTag"
              value={form.characterTag}
              onChange={handleChange}
              maxLength={50}
            />
          </label>
          <label className="field">
            가격
            <input
              className="input"
              type="number"
              name="price"
              value={form.price}
              onChange={handleChange}
              min={1}
              required
            />
          </label>
          <label className="field">
            상태
            <select className="input" name="condition" value={form.condition} onChange={handleChange}>
              <option value="NEW">새 상품</option>
              <option value="USED">중고</option>
            </select>
          </label>
          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            {isSubmitting ? '저장 중...' : isEdit ? '수정하기' : '등록하기'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default ItemFormPage;
