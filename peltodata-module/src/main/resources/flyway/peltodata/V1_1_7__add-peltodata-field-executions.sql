create table peltodata_field_execution
(
    id bigserial
        constraint table_name_pk
        primary key,
    state int,
    execution_started_at timestamp default now(),
    field_id bigint not null
        constraint table_name_peltodata_field_id_fk
            references peltodata_field,
    output_type varchar(50) not null
);

comment on column peltodata_field_execution.state is '-10 = error, 0 = started, 10 = completed';

