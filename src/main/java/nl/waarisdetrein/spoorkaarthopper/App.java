package nl.waarisdetrein.spoorkaarthopper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperAPI;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.InstructionList;
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

	private static final Pattern SUBLOCATION_PATTERN = Pattern.compile("_");
	
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
		GraphHopper gh = GraphhopperUtil.setupGraphhopper(graphFile);
		if (gh == null) {
			LOG.error("Couldn't setup GraphHopper, check above for errors");
			return;
		}
		
		// Open files
		Map<String, Double[]> locations = FileUtil.parseLocations(locationFile);
		List<String> mapping = FileUtil.parseMapping(mappingFile);
		
		int pathsFound = 0;
		int pathsNotFound = 0;
		
		// Store our output, one entry per line 
		List<List<String>> output = new ArrayList<List<String>>();
		
		String line = "";
		for (String entry : mapping) {
			String[] stations = entry.split("\\|");
			if (!locations.containsKey(stations[0]) | !locations.containsKey(stations[1])) {
				LOG.error("List of locations doesn't contain " + stations[0] + " or " + stations[1]);
			} else {
				Double[] from = locations.get(stations[0]);
				Double[] to = locations.get(stations[1]);
				
				if (stations.length == 2) {
					line = planPartAsPolyline(gh, from, to);
				} else if (stations.length == 3) {
					line = planPart(gh, from, to, locations.get(stations[2]));
				} else { 
					LOG.error("Invalid line: "+entry);
				}
			
				if (line.contentEquals("")) {
					LOG.error("Probably didn't find path for relation " + stations[0] + " - " + stations[1]);
				}
				output.add(makeLine(stations[0], stations[1], line));
			}
		}
		
		// Strip extra quantifiers from location tags that contain _
		output = removeDoubleStations(output);
		
		FileUtil.saveOutput(output);
	}

	
	private static List<List<String>> removeDoubleStations(List<List<String>> input) {
		List<List<String>> outCopy = new ArrayList<List<String>>();
		for (List<String> record : input) {
			// Check if either stations contain the pattern, if so, split on that
			if (record.get(0).contains("_")) {
				record.set(0, record.get(0).split("_")[0]);
			}
			if (record.get(1).contains("_")) {
				record.set(1, record.get(1).split("_")[0]);
			}
			outCopy.add(record);
		}
		return outCopy;
	}


	private static PointList planPart(GraphHopper gh, Double[] from, Double[] to) {
		GHRequest request = new GHRequest(from[0], from[1], to[0], to[1]);
		GHResponse response = gh.route(request);
		if (response.isFound()) {
			return response.getPoints();
		}
		return null;
	}
	
	private static String planPartAsPolyline(GraphHopper gh, Double[] from, Double[] to) {
		PointList points = planPart(gh, from, to);
		if (points != null)
			return PolylineUtil.encodePolyline(points);
		return "";
	}
	
	private static String planPart(GraphHopper gh, Double[] from, Double[] to, Double[] via) {
		PointList partA = planPart(gh, from, via);
		PointList partB = planPart(gh, via, to);
		if (partA != null && partB != null) {
			// Add second part to part A
			for (Double[] b : partB.toGeoJson()) {
				partA.add(b[1], b[0]); // GeoJSON is lon, lat - method is lat, lon
			} 
			return PolylineUtil.encodePolyline(partA);
		}
		return "";
	}
	
	private static List<String> makeLine(String from, String to, String polyline) {
		ArrayList<String> out = new ArrayList<String>();
		// Note: this doesn't check the from/to doesn't already exist, we're assuming input is valid 
		out.add(from);
		out.add(to);
		out.add(polyline);
		return out;
		
	}
}
