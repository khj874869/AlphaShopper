alter table ai_interaction_log
    add column if not exists recommendation_bucket varchar(40);

alter table product_discovery_click_log
    add column if not exists recommendation_bucket varchar(40);

alter table product_discovery_impression_log
    add column if not exists recommendation_bucket varchar(40);

create index if not exists idx_ai_interaction_log_bucket
    on ai_interaction_log (recommendation_bucket);

create index if not exists idx_product_discovery_click_bucket
    on product_discovery_click_log (recommendation_bucket);

create index if not exists idx_product_discovery_impression_bucket
    on product_discovery_impression_log (recommendation_bucket);
