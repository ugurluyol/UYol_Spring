CREATE TABLE otp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    otp_code CHAR(6) NOT NULL,
    user_id CHAR(36) NOT NULL,
    is_confirmed TINYINT(1) NOT NULL,
    creation_date DATETIME NOT NULL,
    expiration_date DATETIME NOT NULL,
    CONSTRAINT user_otp_fk FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE
);

-- один активный OTP на пользователя
CREATE UNIQUE INDEX unique_active_otp_per_user
ON otp(user_id, is_confirmed);
