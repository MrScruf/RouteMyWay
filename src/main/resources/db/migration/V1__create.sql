create table stop
(
    stopId         varchar not null primary key,
    name           varchar,
    latitude       float,
    longitude      float,
    locationTypeId int
);

create table trip
(
    tripId        varchar not null primary key,
    routeId       varchar not null,
    serviceId     varchar not null,
    tripHeadSign  varchar,
    tripShortName varchar
);

create table locationType
(
    locationTypeId int     not null primary key,
    name           varchar not null
);
alter table stop
    ADD FOREIGN KEY (locationTypeId)
        REFERENCES locationType (locationTypeId);


create table footConnection
(
    departureStopId varchar not null,
    arrivalStopId   varchar not null,
    duration        int     not null,
    PRIMARY KEY (departureStopId, arrivalStopId)
);

alter table footConnection
    ADD FOREIGN KEY (departureStopId)
        REFERENCES stop (stopId);

alter table footConnection
    ADD FOREIGN KEY (arrivalStopId)
        REFERENCES stop (stopId);
-- Table defined for H2, needs to change id IDENTITY for other RDBMS
create table tripConnection
(
    tripConnectionId    identity not null primary key,
    departureStopId     varchar  not null,
    arrivalStopId       varchar  not null,
    tripId              varchar  not null,
    departureTimeHour   int      not null,
    departureTimeMinute int      not null,
    departureTimeSecond int      not null,
    arrivalTimeHour     int      not null,
    arrivalTimeMinute   int      not null,
    arrivalTimeSecond   int      not null
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