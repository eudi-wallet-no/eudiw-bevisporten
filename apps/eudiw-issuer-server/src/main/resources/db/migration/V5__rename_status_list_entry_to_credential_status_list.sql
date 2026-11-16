ALTER TABLE `status_list_entry` RENAME TO `credential_status_list`;

ALTER TABLE `credential_status_list` ADD COLUMN `created_ms` BIGINT NOT NULL DEFAULT 0;

ALTER TABLE `credential_status_list` DROP FOREIGN KEY `fk_status_list_entry_transaction`;
ALTER TABLE `credential_status_list` DROP INDEX `uq_status_list_entry_transaction_uri_idx`;

ALTER TABLE `credential_status_list`
    ADD CONSTRAINT `uq_credential_status_list_transaction_uri_idx`
        UNIQUE (`credential_issuance_transaction_id`, `uri`, `idx`);

ALTER TABLE `credential_status_list`
    ADD CONSTRAINT `fk_credential_status_list_transaction`
        FOREIGN KEY (`credential_issuance_transaction_id`)
            REFERENCES `credential_issuance_transaction` (`id`)
            ON DELETE CASCADE;
