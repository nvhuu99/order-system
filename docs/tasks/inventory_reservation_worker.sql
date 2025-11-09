INSERT INTO products (id, price, name, stock, updated_at) VALUES
('prod01', 10.00, 'Product 01 - zero stock', 0, '2025-11-01 00:00:00'),
('prod02', 20.00, 'Product 02 - stock 5', 5, '2025-11-01 00:00:00'),
('prod03', 30.00, 'Product 03 - stock 3', 3, '2025-11-01 00:00:00'),
('prod04', 40.00, 'Product 04 - stock 10', 10, '2025-11-01 00:00:00'),
('prod05', 50.00, 'Product 05 - stock 2', 2, '2025-11-01 00:00:00');

INSERT INTO product_reservations
  (id, product_id, user_id, desired_amount, reserved, status, expires_at, updated_at)
VALUES
('r-prod01-1', 'prod01', 'user1', 1, 0, 'PENDING', '2025-11-10 00:00:00', '2025-11-04 12:00:00'),

('r-prod02-1', 'prod02', 'user2', 3, 0, 'PENDING', '2025-11-10 00:00:00', '2025-11-04 09:00:00'),
('r-prod02-2', 'prod02', 'user3', 3, 0, 'PENDING', '2025-11-10 00:00:00', '2025-11-04 10:00:00'),
('r-prod02-3', 'prod02', 'user4', 1, 0, 'PENDING', '2025-11-10 00:00:00', '2025-11-04 11:00:00'),

('r-prod03-1', 'prod03', 'user5', 2, 2, 'PENDING', '2025-10-30 00:00:00', '2025-11-04 08:00:00'),
('r-prod03-2', 'prod03', 'user6', 3, 0, 'PENDING', '2025-11-10 00:00:00', '2025-11-04 12:00:00'),

('r-prod04-1', 'prod04', 'user7', 2, 0, 'PENDING', '2025-11-10 00:00:00', '2025-11-04 08:00:00'),
('r-prod04-2', 'prod04', 'user8', 3, 0, 'PENDING', '2025-11-10 00:00:00', '2025-11-04 09:00:00'),
('r-prod04-3', 'prod04', 'user9', 5, 0, 'PENDING', '2025-11-03 10:00:00', '2025-11-04 10:00:00'),

('r-prod05-1', 'prod05', 'user10', 3, 0, 'PENDING', '2025-11-10 00:00:00', '2025-11-04 07:00:00');


----


UPDATE product_reservations pr
JOIN (
  SELECT
    pr_inner.id,
    pr_inner.product_id,
    pr_inner.desired_amount,
    pr_inner.expires_at,
    pr_inner.updated_at,
    p.stock,
    COALESCE(
      SUM(
        CASE
          WHEN pr_inner.expires_at > CURRENT_TIMESTAMP() AND pr_inner.status != 'EXPIRED'
            THEN pr_inner.desired_amount
          ELSE 0
        END
      ) OVER (
        PARTITION BY pr_inner.product_id
        ORDER BY pr_inner.updated_at
        ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
      ), 0
    ) AS reservation_accumulation
  FROM product_reservations pr_inner
  JOIN products p ON p.id = pr_inner.product_id
  WHERE pr_inner.product_id IN ('prod01', 'prod02', 'prod03', 'prod04', 'prod05')
) AS a ON pr.id = a.id
SET
  pr.reserved = CASE
    -- expired: clear reservation
    WHEN a.expires_at <= CURRENT_TIMESTAMP() THEN 0
    -- product already fully consumed by prior reservations
    WHEN a.stock <= a.reservation_accumulation THEN 0
    -- partially available: reserve leftover stock
    WHEN a.stock < a.reservation_accumulation + a.desired_amount
      THEN a.stock - a.reservation_accumulation
    -- otherwise: reserve desired amount
    ELSE a.desired_amount
  END,
  pr.status = CASE
    WHEN a.expires_at <= CURRENT_TIMESTAMP() THEN 'EXPIRED'
    WHEN a.stock <= a.reservation_accumulation THEN 'INSUFFICIENT_STOCK'
    WHEN a.stock < a.reservation_accumulation + a.desired_amount THEN 'INSUFFICIENT_STOCK'
    ELSE 'OK'
  END,
  pr.updated_at = CURRENT_TIMESTAMP()
WHERE pr.product_id IN ('prod01', 'prod02', 'prod03', 'prod04', 'prod05');