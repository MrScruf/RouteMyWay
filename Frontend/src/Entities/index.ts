import SearchResult from "../Components/SearchResultView";

export interface SearchResult {
  id: number;
  name: string;
  other: any | null;
}

export interface Stop extends SearchResult {
  wheelChairBoarding: number;
  stopId: string;
}

export interface Trip {
  tripId: string;
  routeId: number;
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
  departureStopId: number;
  arrivalStopId: number;
}
export interface TripConnection extends Connection {
  tripId: number;
  tripConnectionId: number;
  depTime: string;
  arrTime: string;
}
export interface FootConnection extends Connection {
  durationInMinutes: number;
}
export interface Paths {
  stops: Array<Stop>;
  trips: Array<Trip>;
  routes: Array<Route>;
  paths: Array<Array<Connection>>;
}
export interface Vehicle{
  id: number,
  name: string
}