CREATE TABLE IF NOT EXISTS peltodata_field
(
  id               BIGSERIAL                  NOT NULL,
  description      CHARACTER VARYING(256),
  user_id          integer,
  CONSTRAINT       peltodata_fields_pkey      PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS peltodata_field_layer
(
  field_id         integer                    REFERENCES peltodata_field (id) ON DELETE CASCADE ON UPDATE CASCADE,
  layer_id         integer                    REFERENCES oskari_maplayer (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT peltodata_field_layer_pkey PRIMARY KEY (field_id, layer_id)
);
/* this is for easier aggregation than relying using just permissions*/
ALTER TABLE peltodata_field ADD CONSTRAINT peltodata_field_user_fk
 FOREIGN KEY (user_id) REFERENCES oskari_users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE;
