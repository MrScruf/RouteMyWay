import axios from "axios"
import { PathEntity, Stop, Vehicle } from "../../Entities"

export function loadStopsByName(name: string): Promise<Array<Stop>> {
    console.log(name)
    return axios.get(`/stops?name=${name}`, {}).then(response => response.data)
}

export function loadVehiclesFromServer(): Promise<Array<Vehicle>> {
    return axios.get("/vehicles")
        .then(response => response.data)
        .then(data => data.map((v: any) => ({ name: v.name, id: v.routeTypeId })))

}

export function sendFileToServer(file: File, password: string): Promise<string> {
    const formData = new FormData()
    formData.append("file", file)
    formData.append("password", password)
    return axios.post("/load", formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    }).then(response => response.data)
}

export function findPath(fromStopId: string, toStopId: string, departureTime: string, numberOfPaths: number,
    bikesAllowed: boolean, wheelchairAccessible: boolean, vehiclesAllowed: Array<number>): Promise<Array<PathEntity>> {
    const params = new URLSearchParams({})
    params.append("departureStopId", fromStopId)
    params.append("arrivalStopId", toStopId)
    params.append("departureTime", departureTime)
    params.append("numberOfPaths", numberOfPaths.toString())
    params.append("bikesAllowed", bikesAllowed + "")
    params.append("wheelChairsAllowed", wheelchairAccessible + "")
    vehiclesAllowed.length > 0 && params.append("vehiclesAllowed", vehiclesAllowed.join(","))
    return axios.get("/path/json?" + params, {}).then(response => response.data as Array<PathEntity>)
}