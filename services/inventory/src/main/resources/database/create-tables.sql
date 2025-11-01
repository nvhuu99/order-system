-- products
CREATE TABLE `products` (
  `id` VARCHAR(36) NOT NULL,
  `price` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `name` VARCHAR(255) NOT NULL,
  `stock` INT NOT NULL DEFAULT 0,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- product_reservations
CREATE TABLE `product_reservations` (
  `id` VARCHAR(36) NOT NULL,
  `user_id` VARCHAR(36) NOT NULL,
  `product_id` VARCHAR(36) NOT NULL,
  `quantity` INT NOT NULL DEFAULT 0,
  `expired_at` DATETIME NOT NULL,
  `total_reserved_snapshot` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(50) NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_pr_product_id` (`product_id`),
  CONSTRAINT `fk_pr_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
