import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { createItem, fetchItem, fetchWorks, updateItem } from './api';
import { CATEGORY_LABEL } from './constants';
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

function ItemFormPage() {
  const { itemId } = useParams();
  const isEdit = Boolean(itemId);
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(isEdit);
  const [works, setWorks] = useState([]);

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
        setForm({
          title: data.title,
          description: data.description ?? '',
          category: data.category,
          workTag: data.workTag ?? '',
          characterTag: data.characterTag ?? '',
          price: String(data.price),
          condition: data.condition,
        });
      })
      .catch((err) => setError(getErrorMessage(err, '상품 정보를 불러오지 못했습니다.')))
      .finally(() => setIsLoading(false));
  }, [itemId, isEdit]);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setIsSubmitting(true);

    const payload = { ...form, workTag: form.workTag.trim() || null, price: Number(form.price) };

    try {
      if (isEdit) {
        await updateItem(itemId, payload);
        navigate(`/items/${itemId}`);
      } else {
        const { data } = await createItem(payload);
        navigate(`/items/${data.id}`);
      }
    } catch (err) {
      setError(getErrorMessage(err, '저장에 실패했습니다.'));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isLoading) {
    return (
      <div className="page">
        <p className="muted">불러오는 중...</p>
      </div>
    );
  }

  return (
    <div className="page item-form-page">
      <div className="card">
        <h2>{isEdit ? '상품 수정' : '상품 등록'}</h2>
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
          <div className="field">
            <label htmlFor="item-work-tag">작품명</label>
            <WorkCombobox
              id="item-work-tag"
              value={form.workTag}
              onChange={(workTag) => setForm((prev) => ({ ...prev, workTag }))}
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
