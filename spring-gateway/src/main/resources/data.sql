-- Use INSERT ON CONFLICT (Upsert) so it doesn't fail if you restart the container
INSERT INTO users (id, name, status)
VALUES ('usr_998877', 'John Doe', 'ACTIVE')
    ON CONFLICT (id) DO NOTHING;

INSERT INTO merchants (merchant_id, is_active) VALUES ('merch_123', true)
    ON CONFLICT (merchant_id) DO NOTHING;
