CREATE TABLE IF NOT EXISTS `status_list`
(
    `id`                INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
    `list_size`         INTEGER UNSIGNED NOT NULL,
    `seed`              INTEGER UNSIGNED NOT NULL DEFAULT 0,
    `next_index`        INTEGER UNSIGNED NOT NULL DEFAULT 0,
    `created_ms`        BIGINT(20) UNSIGNED NOT NULL DEFAULT 0,
    `updated_ms`        BIGINT(20) UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `status_list_entry`
(
    `status_list_id`    INTEGER UNSIGNED NOT NULL,
    `list_index`        INTEGER UNSIGNED NOT NULL DEFAULT 0,
    `status_value`      TINYINT UNSIGNED NOT NULL DEFAULT 0,
    `created_ms`        BIGINT(20) UNSIGNED NOT NULL DEFAULT 0,
    `updated_ms`        BIGINT(20) UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`status_list_id`, `list_index`),
    CONSTRAINT `status_list_entry_fk` FOREIGN KEY (`status_list_id`) REFERENCES `status_list` (`id`)
);

