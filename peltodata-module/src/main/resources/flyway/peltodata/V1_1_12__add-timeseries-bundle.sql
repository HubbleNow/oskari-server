insert into portti_view_bundle_seq
(view_id, bundle_id, seqno, config, state, startup, bundleinstance)
values (1,
        (select id from portti_bundle where name = 'timeseries'),
        55,
        '{}',
        '{}',
        null,
        'timeseries')
