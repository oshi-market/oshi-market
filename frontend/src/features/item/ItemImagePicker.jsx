import { useEffect, useMemo, useRef } from 'react';
import { ALLOWED_IMAGE_TYPES, MAX_IMAGE_BYTES, MAX_IMAGES, imageUrl } from './imageUrl';

/**
 * 상품 사진 선택. 실제 업로드/삭제는 저장 버튼을 누를 때 ItemFormPage에서 한 번에 처리하고,
 * 여기서는 "남길 기존 사진"과 "새로 올릴 파일"만 관리한다.
 */
function ItemImagePicker({ existingImages, removedIds, onRemoveExisting, newFiles, onChangeNewFiles, onError }) {
  const inputRef = useRef(null);
  const keptImages = existingImages.filter((image) => !removedIds.includes(image.id));
  const remaining = MAX_IMAGES - keptImages.length - newFiles.length;

  // 새 파일 미리보기 URL. 파일 목록이 바뀌거나 화면을 떠나면 메모리 해제
  const previews = useMemo(() => newFiles.map((file) => URL.createObjectURL(file)), [newFiles]);
  useEffect(() => () => previews.forEach((url) => URL.revokeObjectURL(url)), [previews]);

  function handleSelect(event) {
    const selected = Array.from(event.target.files ?? []);
    event.target.value = ''; // 같은 파일을 다시 고를 수 있게 초기화
    if (selected.length === 0) {
      return;
    }
    if (selected.some((file) => !ALLOWED_IMAGE_TYPES.includes(file.type))) {
      onError('사진은 JPG, PNG, WEBP 형식만 올릴 수 있습니다.');
      return;
    }
    if (selected.some((file) => file.size > MAX_IMAGE_BYTES)) {
      onError('사진은 한 장당 10MB 이하만 올릴 수 있습니다.');
      return;
    }
    if (selected.length > remaining) {
      onError(`사진은 최대 ${MAX_IMAGES}장까지 등록할 수 있습니다.`);
      return;
    }
    onError('');
    onChangeNewFiles([...newFiles, ...selected]);
  }

  return (
    <div className="field">
      <span>
        사진 <span className="muted">({MAX_IMAGES - remaining}/{MAX_IMAGES}) · 첫 번째 사진이 대표 사진이 됩니다</span>
      </span>
      <div className="image-picker">
        {keptImages.map((image) => (
          <div className="image-picker-item" key={`existing-${image.id}`}>
            <img src={imageUrl(image.url, 'mini')} alt="" />
            <button
              type="button"
              className="image-picker-remove"
              aria-label="사진 삭제"
              onClick={() => onRemoveExisting(image.id)}
            >
              ✕
            </button>
          </div>
        ))}
        {previews.map((url, index) => (
          <div className="image-picker-item" key={url}>
            <img src={url} alt="" />
            <button
              type="button"
              className="image-picker-remove"
              aria-label="사진 삭제"
              onClick={() => onChangeNewFiles(newFiles.filter((_, i) => i !== index))}
            >
              ✕
            </button>
          </div>
        ))}
        {remaining > 0 && (
          <button type="button" className="image-picker-add" onClick={() => inputRef.current?.click()}>
            <span aria-hidden="true">＋</span>
            사진 추가
          </button>
        )}
      </div>
      <input
        ref={inputRef}
        type="file"
        accept={ALLOWED_IMAGE_TYPES.join(',')}
        multiple
        hidden
        onChange={handleSelect}
      />
    </div>
  );
}

export default ItemImagePicker;
