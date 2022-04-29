import moment from "moment";
import { Connection, PathEntity, FootConnection, TripConnection } from "../../Entities";
import { FaWalking, FaBus, FaTram, FaSubway, FaTrain, FaShip } from 'react-icons/fa';
import { MdTram } from 'react-icons/md';
interface PathViewProps {
  path: PathEntity;
  index: number;
}

function isFootConnection(connection: any) {
  return 'durationInMinutes' in connection
}

function addMinutesToTime(time:string, minutesToAdd: number){
  if(!time)return ""
    const hours = parseInt(time.substring(0,2))
    const minutes = parseInt( time.substring(3,5))
    const seconds = time.substring(6)
    const outHours = hours+Math.floor(((minutes+minutesToAdd)/60))
    const outMinutes = (minutes+minutesToAdd)%60
    return `${outHours.toString().padStart(2,"0")}:${outMinutes.toString().padStart(2,"0")}:${seconds}`
}
 function iconByRoute(connection: TripConnection) {
    const routeType = connection.trip.route.routeTypeId
    switch (routeType?.routeTypeId) {
      case 0: return <MdTram />
      case 1: return <FaSubway />
      case 2: return <FaTrain />
      case 3: return <FaBus />
      case 4: return <FaShip />
      case 5: return <FaTram />
      case 7: return <MdTram />
      case 11: return <FaBus />
      case 12: return <MdTram />
    }
  } 
   function showConnection(connection: Connection, index: number) {
    const fromStop = connection.departureStop.name
    const toStop = connection.arrivalStop.name
    if (isFootConnection(connection)) {
      if(connection.arrivalStop.name == connection.departureStop.name)return null;
      const footConnection = (connection as FootConnection)
      return (<li className="connection" key={index}>
        <FaWalking />
        <div className="data">
        <div>Walking</div>
        <div>{fromStop} - {toStop} ({footConnection.durationInMinutes + " minutes"})</div>
        </div>
      </li>)
    }
    const tripConn = connection as TripConnection
    const trip = tripConn.trip
    const route = trip.route
    return (<li className="connection" key={index}>
      {iconByRoute(tripConn)}
      <div className="data">
        <div> {route?.routeTypeId.name} - {route?.shortName} ({route?.longName})</div>
        <div>Direction {trip?.tripHeadSign}</div>
        <div> {fromStop} ({tripConn.departureTime}) - {toStop} ({tripConn.arrivalTime})</div>
      </div>
    </li>)
  }
function Path(props: PathViewProps) {
  const firstStop = props.path.connections.at(0)?.departureStop
  const lastStop = props.path.connections.at(-1)?.arrivalStop 
  const firstTripConnection = (isFootConnection(props.path.connections.at(0)) ? props.path.connections.at(1) : props.path.connections.at(0)) as TripConnection
  const lastFootConnection = (isFootConnection(props.path.connections.at(-1)) ? props.path.connections.at(-1) : undefined) as FootConnection | undefined
  const lastTripConnection = (lastFootConnection ? props.path.connections.at(-2) : props.path.connections.at(-1)) as TripConnection
  const depTime = firstTripConnection?.departureTime
  const arrTime = addMinutesToTime(lastTripConnection?.arrivalTime ?? "", lastFootConnection?.durationInMinutes ?? 0)
  return (
    <li className="path" key={props.index}>
      <h2>{firstStop?.name} - {lastStop?.name} {(depTime && arrTime && `(${depTime} - ${arrTime})`)}</h2>
      <ul>
        {props.path.connections.map((connection: Connection, index: number) =>
          showConnection(connection, index)
        )}
      </ul>
    </li>
  );
}

export default Path;
