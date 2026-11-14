CREATE TABLE account_settings (
 user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
 address VARCHAR(500), email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
 addresses_json TEXT NOT NULL DEFAULT '[]'
);
