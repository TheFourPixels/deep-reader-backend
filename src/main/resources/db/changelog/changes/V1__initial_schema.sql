--liquibase formatted sql

--changeset deep-reader:1
CREATE TABLE books (
    id          BIGSERIAL                NOT NULL,
    title       VARCHAR(255)             NOT NULL,
    author      VARCHAR(255)             NOT NULL,
    genre       VARCHAR(100),
    language    VARCHAR(10)              NOT NULL DEFAULT 'en',
    description TEXT,
    cover_url   VARCHAR(512),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_books PRIMARY KEY (id)
);

--changeset deep-reader:2
CREATE TABLE chapters (
    id             BIGSERIAL                NOT NULL,
    book_id        BIGINT                   NOT NULL,
    chapter_number INTEGER                  NOT NULL,
    title          VARCHAR(255)             NOT NULL,
    content        TEXT                     NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_chapters          PRIMARY KEY (id),
    CONSTRAINT fk_chapters_book     FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,
    CONSTRAINT uq_chapters_book_num UNIQUE (book_id, chapter_number)
);

CREATE INDEX idx_chapters_book_id ON chapters (book_id);

--changeset deep-reader:3
CREATE TABLE annotations (
    id              BIGSERIAL                NOT NULL,
    book_id         BIGINT                   NOT NULL,
    chapter_id      BIGINT,
    target_phrase   VARCHAR(512)             NOT NULL,
    explanation     TEXT                     NOT NULL,
    annotation_type VARCHAR(50)              NOT NULL DEFAULT 'SLANG',
    context_snippet TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_annotations         PRIMARY KEY (id),
    CONSTRAINT fk_annotations_book    FOREIGN KEY (book_id)    REFERENCES books    (id) ON DELETE CASCADE,
    CONSTRAINT fk_annotations_chapter FOREIGN KEY (chapter_id) REFERENCES chapters (id) ON DELETE SET NULL
);

CREATE INDEX idx_annotations_book_id    ON annotations (book_id);
CREATE INDEX idx_annotations_chapter_id ON annotations (chapter_id);
