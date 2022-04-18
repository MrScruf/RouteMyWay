import moment from "moment";
import { Connection, Paths, FootConnection, TripConnection } from "../../Entities";
import { FaWalking, FaBus, FaTram, FaSubway, FaTrain, FaShip } from 'react-icons/fa';
import { MdTram } from 'react-icons/md';
interface PathViewProps {
  paths: Paths | null;
  index: number;
}

function isFootConnection(connection: any) {
  return 'durationInMinutes' in connection
}

function Path(props: PathViewProps) {
  function iconByRoute(connection: TripConnection) {
    const routeType = routeById(tripById(connection.tripId)?.routeId ?? -1)?.routeTypeId
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
  function showConnection(connection: Connection) {
    const tmpTripConn = connection as TripConnection
    const icon = isFootConnection(connection) ? <FaWalking /> : iconByRoute(tmpTripConn)
    const fromStop = stopById(connection.departureStopId)?.name
    const toStop = stopById(connection.arrivalStopId)?.name
    if (isFootConnection(connection)) {
      return (<li className="connection">
        {icon} 
        <div className="data">
        <div>Walking</div>
        <div>{fromStop} - {toStop} ({(connection as FootConnection).durationInMinutes + " minutes"})</div>
        </div>
      </li>)
    }
    const trip = tripById(tmpTripConn.tripId)
    const route = routeById(trip?.routeId ?? -1)
    return (<li className="connection">
      {icon}
      <div className="data">
        <div> {route?.routeTypeId.name} - {route?.shortName} ({route?.longName})</div>
        <div>Direction {trip?.tripHeadSign}</div>
        <div> {fromStop} ({tmpTripConn.depTime}) - {toStop} ({tmpTripConn.arrTime})</div>
      </div>
    </li>)
  }
  function stopById(id: number) {
    return props.paths?.stops.find(stop => stop.id == id)
  }
  function tripById(id: number) {
    return props.paths?.trips.find(trip => trip.id == id)
  }
  function routeById(id: number) {
    return props.paths?.routes.find(route => route.id == id)
  }
  const path = props.paths?.paths[props.index]
  const firstTripConnection = (isFootConnection(path?.at(0)) ? path?.at(1) : path?.at(0)) as TripConnection | undefined
  const lastTripConnection = (isFootConnection(path?.at(-1)) ? path?.at(-2) : path?.at(-1)) as TripConnection | undefined
  const lastFootConnection = (isFootConnection(path?.at(-1)) ? path?.at(-1) : undefined) as FootConnection | undefined
  const firstStop = stopById(firstTripConnection?.departureStopId ?? -1)
  const lastStop = stopById(lastFootConnection?.arrivalStopId ?? lastTripConnection?.arrivalStopId ?? -1)
  const depTime = firstTripConnection?.depTime
  const arrDateTime = moment(lastTripConnection?.arrTime, "HH:mm:ss").add(lastFootConnection?.durationInMinutes ?? 0, "minutes")
  const arrTime = arrDateTime.format("HH:mm:ss")
  return (
    <li className="path">
      <h2>{firstStop?.name} - {lastStop?.name} {(depTime && arrTime && `(${depTime} - ${arrTime})`)}</h2>
      <ul>
        {path?.map((connection) =>
          showConnection(connection)
        )}
      </ul>
    </li>
  );
}

export default Path;
