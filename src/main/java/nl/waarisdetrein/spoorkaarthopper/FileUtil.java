package nl.waarisdetrein.spoorkaarthopper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {
	
	private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);
	
	public static List<String> parseMapping(String mappingFile) {
		List<String> output = new ArrayList<String>();
		List<String> lines = null;
		try {
			lines = FileUtils.readLines(new File(mappingFile), "utf-8");
			LOG.info("Number of lines: " + lines.size());
		} catch (IOException e) {	
			LOG.error("Couldn't read mapping file");
		}
		if (lines != null) {
			for (String l : lines) {
				// Unfortunately checking for headers is hard here, so just ignore check 
				String[] split = l.split(",");
				StringBuffer sb = new StringBuffer();
				for (String s : split) {
					sb.append(s.toLowerCase());
					sb.append("|");
				}
				output.add(sb.toString());
			}
		}
		LOG.info("Loaded " + output.size() + " mappings");
		return output;
	}

	public static Map<String, Double[]> parseLocations(String locationFile) {
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

	public static void parseLocationLine(Map<String, Double[]> output, String l) {
		String[] split = l.split(",");
		Double[] location = new Double[] { Double.parseDouble(split[1]), Double.parseDouble(split[2]) };
		output.put(split[0].toLowerCase(), location);
	}

	public static void saveOutput(List<List<String>> output) {
		File f = new File("output.csv");
		List<String> lines = new ArrayList<String>();
		for (List<String> l : output) {
			// Note: This should be a join type function
			lines.add(l.get(0)+','+l.get(1)+','+l.get(2));
		}
		try {
			FileUtils.writeLines(f, lines);
		} catch (IOException e) {
			LOG.error("Failed to write file", e);
		}
	}
}
