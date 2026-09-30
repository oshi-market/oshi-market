-- 상품 사진. 파일은 Cloudinary에 저장하고 DB에는 URL과 삭제용 public_id만 보관.
-- sort_order가 가장 작은 사진이 목록 썸네일. 상품 삭제 시 사진 행도 함께 삭제(ON DELETE CASCADE),
-- Cloudinary 파일 삭제는 애플리케이션(ItemService#delete)에서 처리.

CREATE TABLE item_image (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    public_id VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_image_item FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE
);
CREATE INDEX idx_item_image_item ON item_image (item_id, sort_order);
