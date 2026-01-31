-- Insert dummy data into orders table
INSERT INTO orders (name, original_amount, final_amount, status, customer_id, created_at, updated_at)
VALUES ('Premium Laptop Order', 1299.99, 1169.99, 'CREATED', 1001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO orders (name, original_amount, final_amount, status, customer_id, created_at, updated_at)
VALUES ('Wireless Headphones', 199.99, 179.99, 'PAID', 1002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert dummy data into coupons table
-- Coupon 1: 20% off (PERCENTAGE type)
INSERT INTO coupons (code, type, discount_value, min_order_amount, max_uses, used_count, valid_from, valid_until, active)
VALUES ('SAVE20PC', 'PERCENTAGE', 20.00, 0.00, 100, 5, '2026-01-01 00:00:00', '2026-12-31 23:59:59', true);

-- Coupon 2: $50 off (FIXED type, requires min order of $100)
INSERT INTO coupons (code, type, discount_value, min_order_amount, max_uses, used_count, valid_from, valid_until, active)
VALUES ('FLAT50OF', 'FIXED', 50.00, 100.00, 50, 10, '2026-01-01 00:00:00', '2026-12-31 23:59:59', true);
