import { PathEntity } from "../../Entities";
import Path from "./Path";

interface PathsViewProps {
  paths: Array<PathEntity> | null;
}

function PathsView(props: PathsViewProps) {
  return <section>
    <ul>
      {props.paths?.map((el,index)=><Path path={el} index={index} key={index}></Path>)}
    </ul>
  </section>;
}

export default PathsView;
