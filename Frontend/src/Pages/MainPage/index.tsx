import { ChangeEvent, useEffect, useState } from "react"
import { PathEntity, Stop, Vehicle } from "../../Entities"
import "./index.css"
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import SearchResultView from "../../Components/SearchResultView";
import "./SearchResult.css"
import axios from "axios";
import Multiselect from 'multiselect-react-dropdown';
import PathsView from "../../Components/PathsView";
import ReactModal from 'react-modal';
import ReactLoading from "react-loading";
import { MdClose } from "react-icons/md";
import { findPath, loadStopsByName, loadVehiclesFromServer, sendFileToServer } from "../../Services/DataService";
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
  const [paths, setPaths] = useState<Array<PathEntity> | null>(null)
  const [vehicles, setVehicles] = useState<Array<Vehicle>>([])
  const [modalIsOpen, setModalIsOpen] = useState<boolean>(false);
  const [dataUploading, setDataUploading] = useState<boolean>(false);
  const [fileToUpload, setFileToUpload] = useState<File|null>()
  const [pathLoading, setPathLoading] = useState<boolean>(false); 
  const [password, setPassword] = useState<string>(""); 
  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      loadStopsByName(searchTermFrom).then(stops=>setFromStops(stops)).catch(e => toast.error("Could not load stops"))
    }, SEARCH_DELAY_MS)
    return () => {
      clearTimeout(delayDebounceFn)
    }
  }, [searchTermFrom])

  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      loadStopsByName(searchTermTo).then(json => setToStops(json)).catch(e => toast.error("Could not load stops"))
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
  useEffect(()=>{
    loadVehiclesFromServer().then(vehicles=>setVehicles(vehicles))
    .catch(e=>toast.error("Could not load vehicles"))
  },[])

  function convertDate(){
    return (new Date(departureTime.getTime() - departureTime.getTimezoneOffset() * 60000).toISOString()).slice(0, -8);
  }

function sendFile(){
  if(!fileToUpload){
    toast.error("No file selected")
    return
  }
  setDataUploading(true)
  sendFileToServer(fileToUpload, password)
  .then(mess=>toast.success(mess))
  .catch(e=>{if(e.response){
    if(e.response.status == 401)toast.error("Unauthorized")
    else toast.error(e.response.message)
  }}  )
  .finally(()=>setDataUploading(false))
}

  function sendRequest(){
    setPathLoading(true)
    findPath(from?.stopId ?? "", to?.stopId ?? "", convertDate(), numberOfPaths, bikesAllowed, wheelchairAccessible, vehiclesAllowed.map(e=>e.id))
    .then(paths=>setPaths(paths)).catch(e => e.response && toast.error(e.response.data.message) && setPaths(null)).finally(()=>setPathLoading(false))
  }
  ReactModal.setAppElement('#root');
  return (
    <main id="main">
      <button className="niceButton" onClick={()=>setModalIsOpen(true)}>Upload data</button>
      <ToastContainer position="top-right" autoClose={2500} hideProgressBar={false} closeOnClick pauseOnFocusLoss />
        <ReactModal isOpen={modalIsOpen} contentLabel="GTFS Upload" className="modal" shouldCloseOnOverlayClick={true} overlayClassName="overlay" onRequestClose={()=>setModalIsOpen(false)}>
          <button className="iconButton" onClick={()=>setModalIsOpen(false)}><MdClose /></button>
          <form>
            <input type="file" disabled={dataUploading} onChange={(e)=> setFileToUpload(e.target.files?.item(0))}/>
            <label htmlFor="password">Password</label>
            <input type="password" onChange={(e)=>setPassword(e.target.value)} id="password"></input>
            <button className="niceButton" disabled={dataUploading} onClick={(e)=>sendFile()}>Send</button>
            {dataUploading && <ReactLoading type="bubbles" color="blue" height="1rem" width="4rem" />}
          </form>
        </ReactModal>
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
          <button id="sendButton" onClick={()=>sendRequest()} type="button" disabled={dataUploading}>Send</button>
        </div>
      </form>
      {(!pathLoading && <PathsView paths={paths}></PathsView>) || (pathLoading && <ReactLoading type="spinningBubbles" color="blue" height="1rem" width="4rem" />)}
    </main>
  )
}

export default MainPage