CREATE TABLE IF NOT EXISTS news_articles (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    title VARCHAR(1024) NOT NULL,
    description TEXT,
    author VARCHAR(512),
    content TEXT,
    url_to_image TEXT,
    category VARCHAR(50) NOT NULL,
    stored_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_news_articles_url UNIQUE (url)
);
