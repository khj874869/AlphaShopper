alter table product add column if not exists version bigint not null default 0;
alter table purchase_order add column if not exists version bigint not null default 0;
alter table purchase_order add column if not exists idempotency_key varchar(100);

create unique index if not exists uk_purchase_order_idempotency_key
    on purchase_order (member_id, idempotency_key);
