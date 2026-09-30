import { useEffect, useId, useRef, useState } from 'react';

/**
 * 작품명 검색형 드롭다운. 네이티브 <datalist>는 브라우저가 팝업을 그려서 크기/스타일을 바꿀 수 없어
 * 입력칸 바로 아래에 펼쳐지는 목록을 직접 구현했다. 목록에 없는 값도 입력은 되지만 서버에서
 * WORK_NOT_FOUND(400)로 거절된다.
 */
function WorkCombobox({ id, value, onChange, options, placeholder }) {
  const [isOpen, setIsOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const listRef = useRef(null);
  const listId = useId();

  const query = value.trim().toLowerCase();
  const filtered = query ? options.filter((name) => name.toLowerCase().includes(query)) : options;

  /** 키보드로 이동할 때 선택된 항목이 목록 밖으로 나가지 않게 스크롤. */
  useEffect(() => {
    if (activeIndex < 0 || !listRef.current) {
      return;
    }
    listRef.current.children[activeIndex]?.scrollIntoView({ block: 'nearest' });
  }, [activeIndex]);

  function select(name) {
    onChange(name);
    setIsOpen(false);
    setActiveIndex(-1);
  }

  function handleKeyDown(event) {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setIsOpen(true);
      setActiveIndex((prev) => Math.min(prev + 1, filtered.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setActiveIndex((prev) => Math.max(prev - 1, 0));
    } else if (event.key === 'Enter' && isOpen && activeIndex >= 0) {
      // 폼 제출 대신 항목 선택
      event.preventDefault();
      select(filtered[activeIndex]);
    } else if (event.key === 'Escape') {
      setIsOpen(false);
    }
  }

  return (
    <div className="combobox">
      <input
        id={id}
        className="input"
        role="combobox"
        aria-expanded={isOpen}
        aria-controls={listId}
        aria-autocomplete="list"
        value={value}
        onChange={(event) => {
          onChange(event.target.value);
          setIsOpen(true);
          setActiveIndex(-1);
        }}
        onFocus={() => setIsOpen(true)}
        onBlur={() => setIsOpen(false)}
        onKeyDown={handleKeyDown}
        maxLength={50}
        placeholder={placeholder}
        autoComplete="off"
      />
      {isOpen && (
        <ul className="combobox-list" id={listId} role="listbox" ref={listRef}>
          {filtered.length === 0 && <li className="combobox-empty">일치하는 작품이 없습니다</li>}
          {filtered.map((name, index) => (
            <li
              key={name}
              role="option"
              aria-selected={index === activeIndex}
              className={`combobox-option${index === activeIndex ? ' is-active' : ''}`}
              // mousedown에서 선택해야 input blur(목록 닫힘)보다 먼저 처리된다
              onMouseDown={(event) => {
                event.preventDefault();
                select(name);
              }}
              onMouseEnter={() => setActiveIndex(index)}
            >
              {name}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default WorkCombobox;
