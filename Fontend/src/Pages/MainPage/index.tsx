import { ChangeEvent, useEffect, useState } from "react"
import StopsText from "../../Components/SearchInput"
import { Stop } from "../../Entities"
import "./index.css"
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import SearchResultView from "../../Components/SearchResultView";
import "./SearchResult.css"
const SEARCH_DELAY_MS = 400

interface AbordableFetch {
  abort: () => void,
  ready: Promise<any>
}

function abortableFetch(request: string, opts: any): AbordableFetch {
  const controller = new AbortController();
  const signal = controller.signal;

  return {
    abort: () => controller.abort(),
    ready: fetch(request, { ...opts, signal })
  };
}

function getPath(){}

interface MainPageProps {

}

function MainPage(props: MainPageProps) {

  const [from, setFrom] = useState<Stop | null>(null)
  const [to, setTo] = useState<Stop | null>(null)
  const [fromStops, setFromStops] = useState<Stop[]>([])
  const [toStops, setToStops] = useState<Stop[]>([])
  const [searchTermFrom, setSearchTermFrom] = useState<string>('')
  const [searchTermTo, setSearchTermTo] = useState<string>('')
  const [fromSearchFocused, setFromSearchFocused] = useState<boolean>(false)
  const [toSearchFocused, setToSearchFocused] = useState<boolean>(false)
  const [departureTime, setDepartureTime] = useState<Date>(new Date())
  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      console.log(searchTermFrom)
      abortableFetch(`/stops?name=${searchTermFrom}`, {}).ready.then(response => response.json()).then(json => setFromStops(json)).catch(e => e.name != "AbortError" && toast.error("Could not load stops"))
    }, SEARCH_DELAY_MS)
    return () => {
      clearTimeout(delayDebounceFn)
    }
  }, [searchTermFrom])

  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      console.log(searchTermTo)
      abortableFetch(`/stops?name=${searchTermTo}`, {}).ready.then(response => response.json()).then(json => setToStops(json)).catch(e => e.name !== "AbortError" && toast.error("Could not load stops"))
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

  return (
    <main id="main">
      <ToastContainer position="top-right"
        autoClose={2500}
        hideProgressBar={false}
        closeOnClick
        pauseOnFocusLoss />
      <form>
        <section className="search" onBlur={() => focusSearch(false, false)}>
          <StopsText onChange={(e: ChangeEvent<any>) => setSearchTermFrom(e.target.value)} value={searchTermFrom} onFocus={(focus: boolean) => focusSearch(focus, false)} />
          {fromStops.length > 0 && fromSearchFocused && <SearchResultView listClassName="searchResult" listItemClassName="searchResultItem" items={fromStops} selectItem={selectFromStop}></SearchResultView>}
        </section>
        <section className="search" onBlur={() => focusSearch(false, false)}>
          <StopsText onChange={(e: ChangeEvent<any>) => setSearchTermTo(e.target.value)} value={searchTermTo} onFocus={(focus: boolean) => focusSearch(false, focus)} />
          {toStops.length > 0 && toSearchFocused && <SearchResultView listClassName="searchResult" listItemClassName="searchResultItem" items={toStops} selectItem={selectToStop} ></SearchResultView>}
        </section>
        <input className="dateTimePicker" type="datetime-local" value={new Date(departureTime.getTime() + departureTime.getTimezoneOffset() * 60000).toISOString().substring(0, 19)} onChange={(e) => setDepartureTime(new Date(Date.parse(e.target.value)))} />
        <button id="sendButton">Send</button>
      </form>
    </main>
  )
}

export default MainPage