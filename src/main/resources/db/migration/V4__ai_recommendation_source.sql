alter table ai_interaction_log
    add column if not exists recommendation_source varchar(30) not null default 'DATABASE';

create index if not exists idx_ai_interaction_log_recommendation_source
    on ai_interaction_log (recommendation_source);
