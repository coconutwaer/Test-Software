-- Create wines table if it does not exist
CREATE TABLE IF NOT EXISTS wines (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)    NOT NULL,
    winery      VARCHAR(255),
    country     VARCHAR(100),
    region      VARCHAR(100),
    grape_variety VARCHAR(100),
    wine_type   VARCHAR(50),
    sweetness   VARCHAR(50),
    body        VARCHAR(50),
    acidity     VARCHAR(50),
    tannin      VARCHAR(50),
    flavor_notes  VARCHAR(500),
    food_pairing  VARCHAR(500),
    price       NUMERIC(8, 2)   NOT NULL,
    rating      NUMERIC(3, 1),
    image_url   VARCHAR(500),
    description VARCHAR(1000)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_wine_type     ON wines (wine_type);
CREATE INDEX IF NOT EXISTS idx_country       ON wines (country);
CREATE INDEX IF NOT EXISTS idx_region        ON wines (region);
CREATE INDEX IF NOT EXISTS idx_grape_variety ON wines (grape_variety);
CREATE INDEX IF NOT EXISTS idx_sweetness     ON wines (sweetness);
CREATE INDEX IF NOT EXISTS idx_price         ON wines (price);
CREATE INDEX IF NOT EXISTS idx_rating        ON wines (rating);

-- Create chat_logs table if it does not exist
CREATE TABLE IF NOT EXISTS chat_logs (
    id           BIGSERIAL PRIMARY KEY,
    user_message VARCHAR(2000) NOT NULL,
    ai_response  VARCHAR(5000) NOT NULL,
    timestamp    TIMESTAMP     NOT NULL
);
