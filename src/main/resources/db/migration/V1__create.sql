create table stop(
    stopId varchar not null primary key,
    name varchar not null,
    latitude float not null,
    longitude float not null
);

create table trip(
    tripId varchar not null primary key,
    routeId varchar not null,
    serviceId varchar,
    tripHeadSign varchar,
    tripShortName varchar
);
-- Table defined for H2, needs to change id IDENTITY for other RDBMS
create table tripConnection(
    id IDENTITY not null primary key,
    departureStopId varchar not null,
    arrivalStopId varchar not null,
    tripId varchar not null,
    departureTimeHour int not null,
    departureTimeMinute int not null,
    departureTimeSecond int not null,
    arrivalTimeHour int not null,
    arrivalTimeMinute int not null,
    arrivalTimeSecond int not null
);

alter table tripConnection ADD FOREIGN KEY (departureStopId)
    REFERENCES stop(stopId);

alter table tripConnection ADD FOREIGN KEY (arrivalStopId)
    REFERENCES stop(stopId);

alter table tripConnection ADD FOREIGN KEY (tripId)
    REFERENCES trip(tripId);