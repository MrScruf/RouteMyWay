import { ChangeEvent } from "react"
import { Stop } from "../../Entities"

interface StopsTextProps {
    value: string ,
    onChange: (e: ChangeEvent<any>) => void,
    onFocus: (e: boolean) => void
}

function StopsText(props: StopsTextProps) {

    return (
        <input type={"search"} onChange={(e) => props.onChange(e)} value={props.value} onFocus={() => props?.onFocus(true)}></input>
    )
}

export default StopsText