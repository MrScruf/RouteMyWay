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
    routeId     varchar not null primary key,
    shortName   varchar,
    longName    varchar,
    routeTypeId integer not null
);

create table stop
(
    stopId             varchar not null primary key,
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
    tripId               varchar not null primary key,
    routeId              varchar not null,
    serviceId            varchar not null,
    tripHeadSign         varchar,
    tripShortName        varchar,
    wheelChairAccessible int,
    bikesAllowed         int
);

alter table trip
    add foreign key (routeId) references route (routeId);

alter table route
    add foreign key (routeTypeId) references routeType (routeTypeId);

create table footPath
(
    departureStopId varchar not null,
    arrivalStopId   varchar not null,
    duration        int     not null,
    PRIMARY KEY (departureStopId, arrivalStopId)
);

alter table footPath
    ADD FOREIGN KEY (departureStopId)
        REFERENCES stop (stopId);

alter table footPath
    ADD FOREIGN KEY (arrivalStopId)
        REFERENCES stop (stopId);

-- Table defined for H2, needs to change id IDENTITY for other RDBMS
create table tripConnection
(
    tripConnectionId identity not null primary key,
    departureStopId  varchar  not null,
    arrivalStopId    varchar  not null,
    tripId           varchar  not null,
    departureTime    int      not null,
    arrivalTime      int      not null
);

alter table tripConnection
    ADD FOREIGN KEY (departureStopId)
        REFERENCES stop (stopId);

alter table tripConnection
    ADD FOREIGN KEY (arrivalStopId)
        REFERENCES stop (stopId);

alter table tripConnection
    ADD FOREIGN KEY (tripId)
        REFERENCES trip (tripId);