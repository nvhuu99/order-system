CREATE TABLE IF NOT EXISTS `product_reservations` (
  `id` VARCHAR(36) NOT NULL,
  `user_id` VARCHAR(36) NOT NULL,
  `product_id` VARCHAR(36) NOT NULL,
  `desired_amount` INT NOT NULL DEFAULT 0,
  `reserved_amount` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(50) NOT NULL,
  `expires_at` TIMESTAMP NOT NULL,
  `updated_at` TIMESTAMP NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pr_user_product` (`user_id`, `product_id`),
  KEY `idx_pr_product_id_user_id` (`product_id`, `user_id`),
  CONSTRAINT `fk_pr_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
