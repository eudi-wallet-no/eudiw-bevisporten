ALTER TABLE `status_list_entry` RENAME TO `credential_status_list`;
ALTER TABLE `credential_status_list` ADD COLUMN `created_ms` BIGINT NOT NULL DEFAULT 0;
