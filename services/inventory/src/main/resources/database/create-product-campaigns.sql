CREATE TABLE IF NOT EXISTS `product_campaigns` (
  `product_id` VARCHAR(36) NOT NULL,
  `campaign_id` VARCHAR(36) NOT NULL,

  UNIQUE KEY `uk_pc_product` (`campaign_id`, `product_id`),
  KEY `idx_pc_campaign_id_product_id` (`campaign_id`, `product_id`),

  CONSTRAINT `fk_pc_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
  CONSTRAINT `fk_pc_campaign` FOREIGN KEY (`campaign_id`) REFERENCES `campaigns` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci