create table locationType
(
    locationTypeId identity not null primary key,
    name           varchar  not null
);

create table routeType
(
    routeTypeId identity not null primary key,
    name        varchar  not null
);

create table route
(
    id          identity not null primary key,
    routeId     varchar  not null,
    shortName   varchar,
    longName    varchar,
    routeTypeId integer  not null
);

create table stop
(
    id                 identity not null primary key,
    stopId             varchar  not null,
    name               varchar,
    latitude           float,
    longitude          float,
    locationTypeId     int,
    wheelChairBoarding int
);

alter table stop
    ADD FOREIGN KEY (locationTypeId) REFERENCES locationType (locationTypeId);

create table trip
(
    id                   identity not null primary key,
    tripId               varchar  not null,
    routeId              integer  not null,
    serviceId            varchar  not null,
    tripHeadSign         varchar,
    tripShortName        varchar,
    wheelChairAccessible int,
    bikesAllowed         int
);

alter table trip
    add foreign key (routeId) references route (id);

alter table route
    add foreign key (routeTypeId) references routeType (routeTypeId);

create table footPath
(
    departureStopId integer not null,
    arrivalStopId   integer not null,
    duration        int     not null,
    PRIMARY KEY (departureStopId, arrivalStopId)
);

alter table footPath
    ADD FOREIGN KEY (departureStopId)
        REFERENCES stop (id);

alter table footPath
    ADD FOREIGN KEY (arrivalStopId)
        REFERENCES stop (id);

-- Table defined for H2, needs to change id IDENTITY for other RDBMS
create table tripConnection
(
    tripConnectionId identity not null primary key,
    departureStopId  integer  not null,
    arrivalStopId    integer  not null,
    tripId           integer  not null,
    departureTime    integer  not null,
    arrivalTime      integer  not null
);

alter table tripConnection
    ADD FOREIGN KEY (departureStopId)
        REFERENCES stop (id);

alter table tripConnection
    ADD FOREIGN KEY (arrivalStopId)
        REFERENCES stop (id);

alter table tripConnection
    ADD FOREIGN KEY (tripId)
        REFERENCES trip (id);