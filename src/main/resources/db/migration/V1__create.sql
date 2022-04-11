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
    routeTypeId integer not null,
    dtype       varchar
);

alter table route
    add foreign key (routeTypeId) references routeType (routeTypeId);

create table stop
(
    id                 integer not null primary key,
    stopId             varchar not null,
    name               varchar,
    latitude           float,
    longitude          float,
    locationTypeId     integer,
    wheelChairBoarding integer,
    dtype              varchar
);

alter table stop
    ADD FOREIGN KEY (locationTypeId) REFERENCES locationType (locationTypeId);

create table trip
(
    id                   integer not null primary key,
    tripId               varchar not null,
    routeId              integer not null,
    serviceId            int     not null, -- TODO Change to int
    tripHeadSign         varchar,
    tripShortName        varchar,
    wheelChairAccessible integer,
    bikesAllowed         integer,
    dtype                varchar
);

alter table trip
    add foreign key (routeId) references route (id);

create table serviceDay
(
    id           integer not null primary key,
    serviceId    varchar not null,
    serviceIdInt int     not null,
    serviceDay   date,
    willGo       boolean,
    dtype        varchar
);

create table footPath
(
    departureStopId integer not null,
    arrivalStopId   integer not null,
    duration        integer not null,
    PRIMARY KEY (departureStopId, arrivalStopId)
);

alter table footPath
    ADD FOREIGN KEY (departureStopId) REFERENCES stop (id);
alter table footPath
    ADD FOREIGN KEY (arrivalStopId) REFERENCES stop (id);

create table tripConnection
(
    tripConnectionId           integer not null primary key,
    departureStopId            integer not null,
    arrivalStopId              integer not null,
    tripId                     integer not null,
    departureStopArrivalTime   integer not null,
    departureStopDepartureTime integer not null,
    arrivalStopArrivalTime     integer not null,
    arrivalStopDepartureTime   integer not null,
    dtype                      varchar
);

alter table tripConnection
    ADD FOREIGN KEY (departureStopId) REFERENCES stop (id);
alter table tripConnection
    ADD FOREIGN KEY (arrivalStopId) REFERENCES stop (id);
alter table tripConnection
    ADD FOREIGN KEY (tripId) REFERENCES trip (id);