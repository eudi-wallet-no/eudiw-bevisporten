CREATE TABLE `credential_issuance_transaction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `issuance_transaction_id` VARCHAR(255) NOT NULL,
    `credential_configuration_id` VARCHAR(255) NOT NULL,
    `credential_issuer_tenant` VARCHAR(50) NOT NULL,
    `created_ms` BIGINT NOT NULL DEFAULT 0,
    `updated_ms` BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT `pk_credential_issuance_transaction` PRIMARY KEY (`id`),
    CONSTRAINT `uq_credential_issuance_transaction_id` UNIQUE (`issuance_transaction_id`)
);

CREATE TABLE `status_list_entry` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `credential_issuance_transaction_id` BIGINT NOT NULL,
    `uri` VARCHAR(255) NOT NULL,
    `idx` INT NOT NULL,
    CONSTRAINT `pk_status_list_entry` PRIMARY KEY (`id`),
    CONSTRAINT `fk_status_list_entry_transaction`
        FOREIGN KEY (`credential_issuance_transaction_id`)
            REFERENCES `credential_issuance_transaction` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `uq_status_list_entry_transaction_uri_idx` UNIQUE (`credential_issuance_transaction_id`, `uri`, `idx`)
);
