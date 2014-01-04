package nl.waarisdetrein.spoorkaarthopper;

import java.io.IOException;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.CmdArgs;

public class GraphhopperUtil {

	public static GraphHopper setupGraphhopper(String graph) {
		CmdArgs ca = getDefaultArguments(graph);
		
		GraphHopper gh = null;
		try {
			gh = new GraphHopper().init(ca).forServer();
		} catch (IOException e1) {
			return null;
		}
		
		// TODO: Also allow RAIL 
		gh.setEncodingManager(new EncodingManager("CAR"));
		gh.importOrLoad();
		return gh;
	}

	public static CmdArgs getDefaultArguments(String graph) {
		CmdArgs ca = new CmdArgs();
		
		ca.put("osmreader.osm", (graph != null) ? graph : "spoorkaarthopper.osm");
		ca.put("osmreader.acceptWay", "CAR");
		ca.put("prepare.chShortcuts", "yes");
		ca.put("index.highResolution", "1000");	
		ca.put("graph.dataaccess", "RAM");
		ca.put("routing.defaultAlgorithm", "astarbi");
		return ca;
	}

}
