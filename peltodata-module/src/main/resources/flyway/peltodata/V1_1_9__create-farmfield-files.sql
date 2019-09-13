create table peltodata_field_file
(
    id bigserial
        primary key,
    original_file_name varchar(255) not null,
    full_path varchar(1024) not null,
    file_date date not null,
    type varchar(100),
    field_id bigint
);

alter table peltodata_field_file
    add constraint peltodata_field_file_peltodata_field_id_fk
        foreign key (field_id) references peltodata_field(id);
