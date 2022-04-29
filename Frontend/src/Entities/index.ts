import SearchResult from "../Components/SearchResultView";


export interface Stop {
  id: number;
  name: string;
  wheelChairBoarding: number;
  stopId: string;
}

export interface Trip {
  tripId: string;
  route: Route;
  tripHeadSign: string;
  tripShortName: string;
  wheelChairAccessible: number;
  bikesAllowed: number;
  id: number;
}
export interface RouteType {
  routeTypeId: number;
  name: string;
}
export interface Route {
  routeId: string;
  shortName: string;
  longName: string;
  routeTypeId: RouteType;
  id: number;
}

export interface Connection {
  name: string;
  departureStop: Stop;
  arrivalStop: Stop;
}
export interface TripConnection extends Connection {
  trip: Trip;
  departureTime: string;
  arrivalTime: string;
}
export interface FootConnection extends Connection {
  durationInMinutes: number;
}

export interface PathEntity{
  connections: Array<Connection>
}
export interface Vehicle{
  id: number,
  name: string
}