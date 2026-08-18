ALTER TABLE `credential_issuance_transaction` ADD COLUMN `status` VARCHAR(50) NULL;
ALTER TABLE `credential_issuance_transaction` ADD COLUMN `notification_id` VARCHAR(255) NULL;
ALTER TABLE `credential_issuance_transaction` ADD UNIQUE INDEX `idx_notification_id` (`notification_id`);
