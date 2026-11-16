CREATE TABLE `wia_status_list` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `credential_issuance_transaction_id` BIGINT NOT NULL,
    `uri` VARCHAR(255) NOT NULL,
    `idx` INT NOT NULL,
    `created_ms` BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT `pk_wia_status_list` PRIMARY KEY (`id`),
    CONSTRAINT `fk_wia_status_list_transaction`
        FOREIGN KEY (`credential_issuance_transaction_id`)
            REFERENCES `credential_issuance_transaction` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `uq_wia_status_list_transaction_uri_idx` UNIQUE (`credential_issuance_transaction_id`, `uri`, `idx`)
);

CREATE INDEX `idx_wia_status_list_uri_idx` ON `wia_status_list` (`uri`, `idx`);

CREATE TABLE `ka_status_list` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `credential_issuance_transaction_id` BIGINT NOT NULL,
    `uri` VARCHAR(255) NOT NULL,
    `idx` INT NOT NULL,
    `created_ms` BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT `pk_ka_status_list` PRIMARY KEY (`id`),
    CONSTRAINT `fk_ka_status_list_transaction`
        FOREIGN KEY (`credential_issuance_transaction_id`)
            REFERENCES `credential_issuance_transaction` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `uq_ka_status_list_transaction_uri_idx` UNIQUE (`credential_issuance_transaction_id`, `uri`, `idx`)
);

CREATE INDEX `idx_ka_status_list_uri_idx` ON `ka_status_list` (`uri`, `idx`);
