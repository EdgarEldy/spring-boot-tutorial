-- Migrate existing password_reset_tokens.type values from the legacy
-- "PASSWORD_RESET" string to "LINK" (the web link flow). Any in-flight
-- rows from before this migration used the web link behaviour, so LINK
-- is the correct semantic mapping. A CHECK constraint is then added so
-- only the two valid discriminators ("LINK", "OTP") can be stored going
-- forward, matching the PasswordResetTokenType Java enum.

UPDATE password_reset_tokens
    SET type = 'LINK'
    WHERE type = 'PASSWORD_RESET';

ALTER TABLE password_reset_tokens
    ADD CONSTRAINT chk_password_reset_token_type
    CHECK (type IN ('LINK', 'OTP'));
