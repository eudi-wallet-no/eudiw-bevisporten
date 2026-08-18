CREATE TABLE `subject_credential_transaction` (
    `id`                      BIGINT       NOT NULL AUTO_INCREMENT,
    `subject_identifier`      VARCHAR(255) NOT NULL,
    `issuance_transaction_id` VARCHAR(255) NOT NULL,
    `created_ms`              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT `pk_subject_credential_transaction`
        PRIMARY KEY (`id`),
    CONSTRAINT `fk_subject_credential_transaction_transaction`
        FOREIGN KEY (`issuance_transaction_id`)
            REFERENCES `credential_issuance_transaction` (`issuance_transaction_id`)
            ON DELETE CASCADE,
    CONSTRAINT `uq_subject_credential_transaction_transaction_id`
        UNIQUE (`issuance_transaction_id`)
);

CREATE INDEX `idx_subject_credential_transaction_subject_identifier`
    ON `subject_credential_transaction` (`subject_identifier`);
