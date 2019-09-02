alter table peltodata_field
    add crop_type varchar(20) not null default 'wheat';

alter table peltodata_field
    add sowing_date date not null default '2019-05-01';
