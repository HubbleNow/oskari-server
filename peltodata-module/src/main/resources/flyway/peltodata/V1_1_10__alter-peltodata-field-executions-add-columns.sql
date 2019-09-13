alter table peltodata_field_execution
    add output_filename varchar(1024);

alter table peltodata_field_execution
    add field_file_id bigint;

alter table peltodata_field_execution
    add constraint peltodata_field_execution_peltodata_field_file_id_fk
        foreign key (field_file_id) references peltodata_field_file(id);
