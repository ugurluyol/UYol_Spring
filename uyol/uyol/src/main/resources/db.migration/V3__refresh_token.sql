CREATE TABLE refresh_token (
    user_id CHAR(36) NOT NULL,
    token TEXT NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_account FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token ON refresh_token(token);
