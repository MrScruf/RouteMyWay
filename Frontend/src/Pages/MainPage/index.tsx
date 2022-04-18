import { ChangeEvent, useEffect, useRef, useState } from "react"
import StopsText from "../../Components/SearchInput"
import { Paths, Stop, Vehicle } from "../../Entities"
import "./index.css"
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import SearchResultView from "../../Components/SearchResultView";
import "./SearchResult.css"
import axios from "axios";
import Multiselect from 'multiselect-react-dropdown';
import PathsView from "../../Components/PathsView";
const SEARCH_DELAY_MS = 400

interface MainPageProps {

}

function MainPage(props: MainPageProps) {

  const [from, setFrom] = useState<Stop | null>(null)
  const [to, setTo] = useState<Stop | null>(null)
  const [wheelchairAccessible, setWheelchairAccessible] = useState<boolean>(false)
  const [numberOfPaths, setNumberOfPaths] = useState<number>(1)
  const [bikesAllowed, setBikesAllowed] = useState<boolean>(false)
  const [vehiclesAllowed, setVehiclesAllowed] = useState<Array<Vehicle>>([])
  const [fromStops, setFromStops] = useState<Stop[]>([])
  const [toStops, setToStops] = useState<Stop[]>([])
  const [searchTermFrom, setSearchTermFrom] = useState<string>('')
  const [searchTermTo, setSearchTermTo] = useState<string>('')
  const [fromSearchFocused, setFromSearchFocused] = useState<boolean>(false)
  const [toSearchFocused, setToSearchFocused] = useState<boolean>(false)
  const [departureTime, setDepartureTime] = useState<Date>(new Date())
  const [paths, setPaths] = useState<Paths | null>(null)
  const [vehicles, setVehicles] = useState<Array<Vehicle>>([])
  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      console.log(searchTermFrom)
      axios.get(`/stops?name=${searchTermFrom}`, {}).then(response => response.data).then(json => setFromStops(json)).catch(e => toast.error("Could not load stops"))
    }, SEARCH_DELAY_MS)
    return () => {
      clearTimeout(delayDebounceFn)
    }
  }, [searchTermFrom])

  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      console.log(searchTermTo)
      axios.get(`/stops?name=${searchTermTo}`, {}).then(response => response.data).then(json => setToStops(json)).catch(e => toast.error("Could not load stops"))
    }, SEARCH_DELAY_MS)
    return () => clearTimeout(delayDebounceFn)
  }, [searchTermTo])

  function selectFromStop(index: number) {
    setFrom(fromStops[index])
    setSearchTermFrom(fromStops[index].name)
    setFromSearchFocused(false)
  }
  function selectToStop(index: number) { 
    setTo(toStops[index])
    setSearchTermTo(toStops[index].name)
    setToSearchFocused(false)
  }
  function focusSearch(focusFrom: boolean, focusTo: boolean) {
    setFromSearchFocused(focusFrom)
    setToSearchFocused(focusTo)
  }
  function loadVehicles(){
    axios.get("/vehicles")
    .then(response=>response.data)
    .then(data=>setVehicles(data.map((v:any)=>({name: v.name, id: v.routeTypeId}))))
    .catch(e=>toast.error("Could not load vehicles"))
  }
  useEffect(()=>{
    loadVehicles()
  },[])
  function convertDate(){
    return (new Date(departureTime.getTime() - departureTime.getTimezoneOffset() * 60000).toISOString()).slice(0, -8);
  }

  function sendRequest(){
    const params = new URLSearchParams({})
    params.append("departureStopId", from?.stopId ?? "")
    params.append("arrivalStopId", to?.stopId ?? "")
    params.append("departureTime", convertDate())
    params.append("numberOfPaths", numberOfPaths.toString())
    params.append("bikesAllowed", bikesAllowed + "")
    params.append("wheelChairsAllowed", wheelchairAccessible + "")
    vehiclesAllowed.length > 0 && params.append("vehiclesAllowed", vehiclesAllowed.map(e=>e.id).join(","))
    axios.get("/path?"+ params,{}).then(response => response.data as Paths).then(json=>setPaths(json)).catch(e => e.response && toast.error(e.response.data.message))
  }
  return (
    <main id="main">
      <ToastContainer position="top-right"
        autoClose={2500}
        hideProgressBar={false}
        closeOnClick
        pauseOnFocusLoss />
      <form>
        <div id="setup">
          <label htmlFor="wheelchaitAccessible">Wheelchair accesible</label>
          <input type="checkbox" id="wheelchaitAccessible" checked={wheelchairAccessible} onChange={(e)=>setWheelchairAccessible(e.target.checked)}></input>
          <label htmlFor="bikesAllowed">Bikes allowed</label>
          <input type="checkbox" id="bikesAllowed" checked={bikesAllowed} onChange={(e)=>setBikesAllowed(e.target.checked)}></input>
          <label htmlFor="numberOfPaths">Number of consequent paths</label>
          <input type="number" id="numberOfPaths" value={numberOfPaths} onChange={e=>setNumberOfPaths(parseInt(e.target.value))}></input>
        </div>
        <div id="select">
          <label htmlFor="vehicles">Allowed vehicles</label>
          <Multiselect id="vehicles" options={vehicles} onSelect={setVehiclesAllowed} onRemove={setVehiclesAllowed} displayValue="name"></Multiselect>
        </div>
        <div id="searchWrapper">
          <section className="search" onBlur={() => focusSearch(false, false)}>
          <input type={"search"} onChange={(e: ChangeEvent<any>) => setSearchTermFrom(e.target.value)} value={searchTermFrom} onFocus={()=>focusSearch(true, false)}/>
            {fromStops.length > 0 && fromSearchFocused && <SearchResultView listClassName="searchResult" listItemClassName="searchResultItem" items={fromStops} selectItem={selectFromStop}></SearchResultView>}
          </section>
          <section className="search" onBlur={() => focusSearch(false, false)}>
          <input type={"search"} onChange={(e: ChangeEvent<any>) => setSearchTermTo(e.target.value)} value={searchTermTo} onFocus={()=>focusSearch(false, true)}/>
            {toStops.length > 0 && toSearchFocused && <SearchResultView listClassName="searchResult" listItemClassName="searchResultItem" items={toStops} selectItem={selectToStop} ></SearchResultView>}
          </section>
          <input className="dateTimePicker" type="datetime-local" value={convertDate()} onChange={(e) => setDepartureTime(new Date(Date.parse(e.target.value) || new Date().getTime()))} />
          <button id="sendButton" onClick={()=>sendRequest()} type="button">Send</button>
        </div>
      </form>
      <PathsView paths={paths}></PathsView>
    </main>
  )
}

export default MainPage