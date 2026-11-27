-- Deterministic seed for backup verification (REQ-DVP-009). Runs once, on first initdb.
-- Expected manifest counts: public.customers = 1000, public.orders = 5000.
CREATE TABLE customers (
  id         integer PRIMARY KEY,
  name       text        NOT NULL,
  created_at timestamptz NOT NULL
);

CREATE TABLE orders (
  id           integer PRIMARY KEY,
  customer_id  integer NOT NULL REFERENCES customers (id),
  amount_cents integer NOT NULL
);

INSERT INTO customers (id, name, created_at)
SELECT g, 'customer-' || g, timestamptz '2026-01-01 00:00:00+00' + g * interval '1 minute'
FROM generate_series(1, 1000) AS g;

INSERT INTO orders (id, customer_id, amount_cents)
SELECT g, (g % 1000) + 1, (g * 37) % 100000
FROM generate_series(1, 5000) AS g;
