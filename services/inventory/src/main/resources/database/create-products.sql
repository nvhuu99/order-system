CREATE TABLE IF NOT EXISTS `products` (
  `id` VARCHAR(36) NOT NULL,
  `price` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `name` VARCHAR(255) NOT NULL,
  `stock` INT NOT NULL DEFAULT 0,
  `reservations_expire_after_seconds` INT NOT NULL,
  `updated_at` TIMESTAMP NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
