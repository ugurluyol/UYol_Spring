CREATE TABLE user_account (
    id CHAR(36) NOT NULL,
    firstname VARCHAR(48) NOT NULL,
    surname VARCHAR(56) NOT NULL,
    phone VARCHAR(22),
    email VARCHAR(256) NOT NULL,
    password VARCHAR(255),
    birth_date DATETIME NOT NULL,
    is_verified TINYINT(1) NOT NULL,
    is_2FA_enabled TINYINT(1) NOT NULL,
    is_banned TINYINT(1) NOT NULL,
    secret_key VARCHAR(64) NOT NULL,
    counter BIGINT NOT NULL,
    creation_date DATETIME NOT NULL,
    last_updated DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX user_email_index ON user_account (email);
CREATE UNIQUE INDEX user_phone_index ON user_account (phone);
