create table locationType
(
    locationTypeId integer not null primary key,
    name           varchar not null
);

create table routeType
(
    routeTypeId integer not null primary key,
    name        varchar not null
);

create table route
(
    id          integer not null primary key,
    routeId     varchar not null,
    shortName   varchar,
    longName    varchar,
    routeTypeId integer not null
);

alter table route
    add foreign key (routeTypeId) references routeType (routeTypeId) on DELETE CASCADE;

create table stop
(
    id                 integer not null primary key,
    stopId             varchar not null,
    name               varchar,
    latitude           float,
    longitude          float,
    locationTypeId     integer,
    wheelChairBoarding integer
);

alter table stop
    ADD FOREIGN KEY (locationTypeId) REFERENCES locationType (locationTypeId);

create table trip
(
    id                   integer not null primary key,
    tripId               varchar not null,
    routeId              integer not null,
    tripHeadSign         varchar,
    tripShortName        varchar,
    wheelChairAccessible integer,
    bikesAllowed         integer
);

alter table trip
    add foreign key (routeId) references route (id);

create table calendar
(
    service_id integer not null primary key,
    monday     boolean not null,
    tuesday    boolean not null,
    wednesday  boolean not null,
    thursday   boolean not null,
    friday     boolean not null,
    saturday   boolean not null,
    sunday     boolean not null,
    start_date date,
    end_date   date
);

create table footConnection
(
    departureStopId   integer not null,
    arrivalStopId     integer not null,
    durationInSeconds integer not null,
    PRIMARY KEY (departureStopId, arrivalStopId)
);

alter table footPath
    ADD FOREIGN KEY (departureStopId) REFERENCES stop (id) on DELETE CASCADE;
alter table footPath
    ADD FOREIGN KEY (arrivalStopId) REFERENCES stop (id) on DELETE CASCADE;

create table tripConnection
(
    id              integer not null primary key,
    departureStopId integer not null,
    arrivalStopId   integer not null,
    tripId          integer not null,
    departureTime   integer not null,
    arrivalTime     integer not null
);

alter table tripConnection
    ADD FOREIGN KEY (departureStopId) REFERENCES stop (id) on DELETE CASCADE;
alter table tripConnection
    ADD FOREIGN KEY (arrivalStopId) REFERENCES stop (id) on DELETE CASCADE;
alter table tripConnection
    ADD FOREIGN KEY (tripId) REFERENCES trip (id) on DELETE CASCADE;