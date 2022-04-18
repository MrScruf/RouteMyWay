import { SearchResult } from "../../Entities"
interface SearchResultViewProps {
    items: SearchResult[],
    listClassName?: string,
    listItemClassName?: string,
    selectItem: (index: number) => void,
}

function SearchResultView(props: SearchResultViewProps) {
    return <ul className={props.listClassName ?? ""} >
        {props.items.filter(it=>it.name.length>0).map((it, index) => <li className={props.listItemClassName ?? ""} key={it.id} onMouseDown={(e) => props.selectItem(index)}>{it.name}</li>)}
    </ul>
}

export default SearchResultView