package nl.waarisdetrein.spoorkaarthopper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperAPI;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.PointList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
	
	private static final Logger LOG = LoggerFactory.getLogger(App.class);  
	
	public static void main(String[] args) {		
		// Check arguments or print help
		if (args.length < 2) {
			LOG.error("Missing arguments");
			LOG.error("Usage: <locations> <location mapping> <graph (optional)> ");
			return;
		}
		
		// Map our variables
		String locationFile = args[0];
		String mappingFile = args[1];
		String graphFile = (args.length >= 3) ? args[2] : null;
		
		// Setup graphhopper - initalizes options, graph and indexes
		GraphHopper gh = setupGraphhopper(graphFile);
		if (gh == null) {
			LOG.error("Couldn't setup GraphHopper, check above for errors");
			return;
		}
		Map<String, Double[]> locations = parseLocations(locationFile);
		Map<String, String> mapping = parseMapping(mappingFile);
		
		int pathsFound = 0;
		int pathsNotFound = 0;
		for (Map.Entry<String, String> entry: mapping.entrySet()) {
			if (!locations.containsKey(entry.getKey()) | !locations.containsKey(entry.getValue())) {
				LOG.error("List of locations doesn't contain " + entry.getKey() + " or " + entry.getValue());
			} else {
				Double[] from = locations.get(entry.getKey());
				Double[] to = locations.get(entry.getValue());
				GHRequest request = new GHRequest(from[0], from[1], to[0], to[1]);
				GHResponse response = gh.route(request);
				PointList points = response.getPoints();
				if (points.getSize() < 1) {
					pathsNotFound = pathsNotFound + 1;
					LOG.error("Probably didn't find path for relation " + entry.getKey() + " - " + entry.getValue());
				} else {
					pathsFound = pathsFound + 1;
					LOG.info("Distance " + response.getDistance() + " in " + response.getTime() + "s");
				}
			}
		}
		
		// Put the object out as a GeoJSON LineString
		/*Map<String, Object> lineString = new HashMap<String, Object>();
		lineString.put("type", new String("LineString"));
		lineString.put("coordinates", points.toGeoJson());
		ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.writeValue(new File("blah.json"), lineString);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	private static Map<String, String> parseMapping(String mappingFile) {
		Map<String, String> output = new HashMap<String, String>();
		List<String> lines = null;
		try {
			lines = FileUtils.readLines(new File(mappingFile), "utf-8");
		} catch (IOException e) {	
			LOG.error("Couldn't read mapping file");
		}
		if (lines != null) {
			for (String l : lines) {
				// Unfortunately checking for headers is hard here, so just ignore check 
				String[] split = l.split(",");
				output.put(split[0].toLowerCase(), split[1].toLowerCase());
			}
		}
		LOG.info("Loaded " + output.size() + " mappings");
		return output;
	}

	private static Map<String, Double[]> parseLocations(String locationFile) {
		Map<String, Double[]> output = new HashMap<String, Double[]>();
		List<String> lines = null;
		try {
			lines = FileUtils.readLines(new File(locationFile), "utf-8");
		} catch (IOException e) {	
			LOG.error("Couldn't read locations file");
		}
		if (lines != null) {
			Boolean first = true;
			for (String l : lines) {
				if (first) { // Check if we have headers
					first = false;
					try {
						Double.parseDouble(l.split(",")[1]);
					} catch (NumberFormatException nfe) {
						continue; // We must have headers, so continue
					}
				} 
				parseLocationLine(output, l);
			}
		}
		LOG.info("Loaded " + output.size() + " locations");
		return output;
	}

	private static void parseLocationLine(Map<String, Double[]> output, String l) {
		String[] split = l.split(",");
		Double[] location = new Double[] { Double.parseDouble(split[1]), Double.parseDouble(split[2]) };
		output.put(split[0].toLowerCase(), location);
	}

	private static GraphHopper setupGraphhopper(String graph) {
		CmdArgs ca = getDefaultArguments(graph);
		
		GraphHopper gh = null;
		try {
			gh = new GraphHopper().init(ca).forDesktop();
		} catch (IOException e1) {
			return null;
		}
		// TODO: Also allow RAIL 
		//gh.setEncodingManager(new EncodingManager("CAR"));
		gh.importOrLoad();
		return gh;
	}

	private static CmdArgs getDefaultArguments(String graph) {
		CmdArgs ca = new CmdArgs();
		
		ca.put("osmreader.osm", (graph != null) ? graph : "spoorkaarthopper.osm");
		ca.put("osmreader.acceptWay", "CAR");
		ca.put("prepare.chShortcuts", "no");
		ca.put("index.highResolution", "100");	
		ca.put("graph.dataaccess", "RAM");
		/*ca.put("routing.defaultAlgorithm", "astarbi");*/
		return ca;
	}

}
