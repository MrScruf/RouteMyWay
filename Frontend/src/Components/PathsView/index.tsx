import { Paths } from "../../Entities";
import Path from "./Path";

interface PathsViewProps {
  paths: Paths | null;
}

function PathsView(props: PathsViewProps) {
  return <section>
    <ul>
      {props.paths?.paths.map((el,index)=><Path paths={props.paths} index={index}></Path>)}
    </ul>
  </section>;
}

export default PathsView;
