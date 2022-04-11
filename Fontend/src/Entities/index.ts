import SearchResult from "../Components/SearchResultView"

export interface SearchResult {
    id: number,
    name: string,
    other: any | null
}


export interface Stop extends SearchResult {

}
