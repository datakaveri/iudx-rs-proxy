Example implementations
-----------------------

#### Elasticsearch ####


#### PostgresSQL ####

Current example implementation for postgresSQL uses below table schema for queries.

~~~sql

CREATE TABLE IF NOT EXISTS pune_flood
(
    id character varying NOT NULL,
    referencelevel numeric(8,2),
    observationdatetime timestamp without time zone NOT NULL,
    measureddistance numeric(8,2),
    currentlevel numeric(8,2)
)

~~~

sample data :

| id                            | refrencelevel | observationdatetime | measureddistance | currentlevel |
|-------------------------------|---------------|---------------------|------------------|--------------|
| iisc.ac.in/sha/rs/city/FWR056 | 13.20         | 2020-11-11 09:15:00 | 12.55            | 0.65         |
| iisc.ac.in/sha/rs/city/FWR056 | 13.20         | 2020-11-11 10:15:00 | 12.56            | 0.64         |
