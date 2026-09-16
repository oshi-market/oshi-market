CREATE TABLE user_account (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    trust_score INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT uk_user_nickname UNIQUE (nickname)
);

CREATE TABLE item (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    work_tag VARCHAR(50),
    character_tag VARCHAR(50),
    price INT NOT NULL,
    condition VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SELLING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_seller FOREIGN KEY (seller_id) REFERENCES user_account (id)
);
CREATE INDEX idx_item_search ON item (category, work_tag, character_tag);
CREATE INDEX idx_item_seller ON item (seller_id);
CREATE INDEX idx_item_status ON item (status);

CREATE TABLE chat_room (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chatroom_item FOREIGN KEY (item_id) REFERENCES item (id),
    CONSTRAINT fk_chatroom_buyer FOREIGN KEY (buyer_id) REFERENCES user_account (id),
    CONSTRAINT fk_chatroom_seller FOREIGN KEY (seller_id) REFERENCES user_account (id),
    CONSTRAINT uk_chatroom_item_buyer UNIQUE (item_id, buyer_id)
);
CREATE INDEX idx_chatroom_buyer ON chat_room (buyer_id);
CREATE INDEX idx_chatroom_seller ON chat_room (seller_id);

CREATE TABLE message (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_chatroom FOREIGN KEY (chat_room_id) REFERENCES chat_room (id),
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES user_account (id)
);
CREATE INDEX idx_message_room_created ON message (chat_room_id, created_at);

CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL,
    chat_room_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    transacted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_item FOREIGN KEY (item_id) REFERENCES item (id),
    CONSTRAINT fk_transaction_chatroom FOREIGN KEY (chat_room_id) REFERENCES chat_room (id),
    CONSTRAINT fk_transaction_buyer FOREIGN KEY (buyer_id) REFERENCES user_account (id),
    CONSTRAINT fk_transaction_seller FOREIGN KEY (seller_id) REFERENCES user_account (id)
);
CREATE INDEX idx_transaction_item ON transaction (item_id);
CREATE INDEX idx_transaction_chatroom ON transaction (chat_room_id);
CREATE INDEX idx_transaction_buyer ON transaction (buyer_id);
CREATE INDEX idx_transaction_seller ON transaction (seller_id);

CREATE TABLE curation (
    id BIGSERIAL PRIMARY KEY,
    work_tag VARCHAR(50) NOT NULL,
    character_tag VARCHAR(50),
    store_name VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    period_start DATE,
    period_end DATE
);
CREATE INDEX idx_curation_tags ON curation (work_tag, character_tag);

CREATE TABLE review (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    rating SMALLINT NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_transaction FOREIGN KEY (transaction_id) REFERENCES transaction (id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES user_account (id),
    CONSTRAINT uk_review_transaction UNIQUE (transaction_id)
);

CREATE TABLE wishlist (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) REFERENCES user_account (id),
    CONSTRAINT fk_wishlist_item FOREIGN KEY (item_id) REFERENCES item (id),
    CONSTRAINT uk_wishlist_user_item UNIQUE (user_id, item_id)
);
