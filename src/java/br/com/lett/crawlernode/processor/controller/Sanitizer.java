package br.com.lett.crawlernode.processor.controller;

import java.text.Normalizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.processor.base.ReplacementMaps;
import br.com.lett.crawlernode.util.Logging;

public class Sanitizer {
	
	private static final Logger logger = LoggerFactory.getLogger(Sanitizer.class);
	
	private Map<String, String> identifiersReplaceMap;
	private Map<String, String> unitsReplaceMap;
	private Map<String, String> recipientsReplaceMap;
	private Map<String, String> brandsReplaceMap;
	private List<String> blackList;
	
	private boolean logActivated;

	public Sanitizer(boolean logActivated) {
		super();
		
		this.logActivated = logActivated;
		
		Logging.printLogDebug(logger, "Creating Sanitizer and assembling replace maps...");
		
		this.identifiersReplaceMap = new LinkedHashMap<String, String>();
		this.unitsReplaceMap = new LinkedHashMap<String, String>();
		this.recipientsReplaceMap = new LinkedHashMap<String, String>();
		this.brandsReplaceMap = new LinkedHashMap<String, String>();
		this.blackList = new ArrayList<String>();
		
		this.blackList.add("para");
		this.blackList.add("com");
		this.blackList.add("de");
		
			
		// Create brands replace map		
		this.unitsReplaceMap = ReplacementMaps.unitsReplaceMap;
		
		
		// Create recipients replace map		
		this.recipientsReplaceMap = ReplacementMaps.recipientsReplaceMap;
		
		Logging.printLogDebug(logger, "success!");

	}

	public String sanitizeName (String paramName) {		
		// This words must be replaced not as a blacklist, but on every situation
		String name = paramName.replace("-", " ");
		name = name.replace("'", " ");
		name = name.replace("+", " ");
		
		// Basic
		name = name.toLowerCase();
		name = name.trim();
		
		// BlackList
		for (String str : this.blackList) {
			// If in beginning
			Pattern beginning = Pattern.compile("^" + Pattern.quote(str));
			Matcher matcherBeginning = beginning.matcher(name);
			while (matcherBeginning.find()) {
				if (logActivated) Logging.printLogDebug(logger, "-- Found BLACKWORD ON BEGINNING: " + str + "\n    '" + name + "' -> '");
				name = " " + name.substring(matcherBeginning.end());
				if (logActivated) Logging.printLogDebug(logger, name + "'");
			}
			
			// If on ending of string
			Pattern ending = Pattern.compile(Pattern.quote(str) + "$");
			Matcher matcherEnding = ending.matcher(name);
			while (matcherEnding.find()) {
				if (logActivated) Logging.printLogDebug(logger, "-- Found BLACKWORD ON ENDING: " + str + "\n    '" + name + "' -> '");
				name = name.substring(0, matcherEnding.start()) + " ";
				if (logActivated) Logging.printLogDebug(logger, name + "'");
			}
			
			// If in the middle of string
			if (name.contains(" " + str + " ")) {
		    	if (logActivated) Logging.printLogDebug(logger, "-- Found BLACKWORD MIDDLE: " + str + "\n    '" + name + "' -> '");
		    	name = name.replaceAll(" " + str + " ", " ");
		    	if (logActivated) Logging.printLogDebug(logger, name + "'");
		    }
		}
		
		// Remove double spaces
		while (name.contains("  ")) {
			if (logActivated) Logging.printLogDebug(logger, "-- Remove DOUBLE space:\n    '" + name + "' -> '");
			name = name.replace("  ", " ");
			if (logActivated) Logging.printLogDebug(logger, name + "'");
		}
		name = name.trim();
		
		// Identifiers replace, always the begin of string
		for (Map.Entry<String, String> entry : this.identifiersReplaceMap.entrySet()) {
			Pattern pattern = Pattern.compile("^" + Pattern.quote(entry.getKey()));
		    Matcher matcher = pattern.matcher(name);
		    while (matcher.find()) {
		    	if (logActivated) Logging.printLogDebug(logger, "-- Found IDENTIFIER ON BEGINNING: " + entry.getKey() + "\n    '" + name + "' -> '");
		    	name = entry.getValue() + " " + name.substring(matcher.end());
		    	if (logActivated) Logging.printLogDebug(logger, name + "'");
		    }
		    while (name.contains("  "))  name = name.replace("  ", " ");
			name = name.trim();
		}
		
		// Brands replace
		for (Map.Entry<String, String> entry : this.brandsReplaceMap.entrySet()) {
			
			// if in beginning
			Pattern beginning = Pattern.compile("^" + Pattern.quote(entry.getKey()));
			Matcher matcherBeginning = beginning.matcher(name);
			while (matcherBeginning.find()) {
				if (logActivated) Logging.printLogDebug(logger, "-- Found BRAND ON BEGINNING: " + entry.getKey() + "\n    '" + name + "' -> '");
				name = entry.getValue() + " " + name.substring(matcherBeginning.end());
				if (logActivated) Logging.printLogDebug(logger, name + "'");
			}
			
			// if on ending of string
			Pattern ending = Pattern.compile(Pattern.quote(entry.getKey()) + "$");
			Matcher matcherEnding = ending.matcher(name);
			while (matcherEnding.find()) {
				if (logActivated) Logging.printLogDebug(logger, "-- Found BRAND ON ENDING: " + entry.getKey() + "\n    '" + name + "' -> '");
				name = name.substring(0, matcherEnding.start()) + " " + entry.getValue();
				if (logActivated) Logging.printLogDebug(logger, name + "'");
			}
			
			// if in the middle of string
			if (name.contains(" " + entry.getKey() + " ")) {
		    	if (logActivated) Logging.printLogDebug(logger, "-- Found BRAND MIDDLE: " + entry.getKey() + "\n    '" + name + "' -> '");
		    	name = name.replaceAll(" " + entry.getKey() + " ", " " + entry.getValue() + " ");
		    	if (logActivated) Logging.printLogDebug(logger, name + "'");
		    }
		}
		
		// Units
		for (Map.Entry<String, String> entry : this.unitsReplaceMap.entrySet()) {
			Pattern pattern = Pattern.compile("[0-9]" + Pattern.quote(entry.getKey()));
		    Matcher matcher = pattern.matcher(name);
		    while (matcher.find()) {
		        String newName = "";
		        if (matcher.start() != 0) newName = name.substring(0, matcher.start() + 1) + " " + entry.getValue();
		        if (matcher.end() != name.length()) newName = newName + name.substring(matcher.end(), name.length());
		        
		        if (logActivated) Logging.printLogDebug(logger, "-- Found UNIT WITH PATTERN: " + entry.getKey() + "\n    '" + name + "' -> '" + newName + "'");
		        
		        name = newName;
		    }
		    
		    if (name.contains(" " + entry.getKey() + " ")) {
		    	if (logActivated) Logging.printLogDebug(logger, "-- Found UNIT MIDDLE: " + entry.getKey() + "\n    '" + name + "' -> '");
		    	name = name.replaceAll(" " + entry.getKey() + " ", " " + entry.getValue() + " ");
		    	if (logActivated) Logging.printLogDebug(logger, name + "'");
		    }
		}
		
		return name;
	}

	/**
	 * 
	 * @param paramName - nome do produto ainda não modificado
	 * @return Arraylist name - arraylist com o nome do produto separado por espaços
	 */
	public List<String> preprocessName (String paramName) {
		// This words must be replaced not as a blacklist, but on every situation
		String name = paramName.replace("-", " ");
		name = name.replace("'", " ");
		name = name.replace("+", " ");
		
		// BlackList
		for (String str : this.blackList) {
			// If in beginning
			Pattern beginning = Pattern.compile("^" + Pattern.quote(str));
			Matcher matcherBeginning = beginning.matcher(name);
			while (matcherBeginning.find()) {
				if (logActivated) Logging.printLogDebug(logger, "-- Found BLACKWORD ON BEGINNING: " + str + "\n    '" + name + "' -> '");
				name = " " + name.substring(matcherBeginning.end());
				if (logActivated) Logging.printLogDebug(logger, name + "'");
			}
			
			// If on ending of string
			Pattern ending = Pattern.compile(Pattern.quote(str) + "$");
			Matcher matcherEnding = ending.matcher(name);
			while (matcherEnding.find()) {
				if (logActivated) Logging.printLogDebug(logger, "-- Found BLACKWORD ON ENDING: " + str + "\n    '" + name + "' -> '");
				name = name.substring(0, matcherEnding.start()) + " ";
				if (logActivated) Logging.printLogDebug(logger, name + "'");
			}
			
			// If in the middle of string
			if (name.contains(" " + str + " ")) {
		    	if (logActivated) Logging.printLogDebug(logger, "-- Found BLACKWORD MIDDLE: " + str + "\n    '" + name + "' -> '");
		    	name = name.replaceAll(" " + str + " ", " ");
		    	if (logActivated) Logging.printLogDebug(logger, name + "'");
		    }
		}
		
		// Remove double spaces
		while (name.contains("  ")) {
			if (logActivated) Logging.printLogDebug(logger, "-- Remove DOUBLE space:\n    '" + name + "' -> '");
			name = name.replace("  ", " ");
			if (logActivated) Logging.printLogDebug(logger, name + "'");
		}
		name = name.trim();
		
		// Lower case
		name = name.toLowerCase();
		
		// Special characters
		name = Normalizer.normalize(name, Normalizer.Form.NFD);
		name = name.replaceAll("[^\\p{ASCII}]", "");
		
		// Units
		for (Map.Entry<String, String> entry : this.unitsReplaceMap.entrySet()) {
			Pattern pattern = Pattern.compile("[0-9]" + Pattern.quote(entry.getKey()));
		    Matcher matcher = pattern.matcher(name);
		    while (matcher.find()) {
		        String newName = "";
		        if (matcher.start() != 0) newName = name.substring(0, matcher.start() + 1) + " " + entry.getValue();
		        if (matcher.end() != name.length()) newName = newName + name.substring(matcher.end(), name.length());
		        
		        if (logActivated) Logging.printLogDebug(logger, "-- Found UNIT WITH PATTERN: " + entry.getKey() + "\n    '" + name + "' -> '" + newName + "'");
		        
		        name = newName;
		    }
		    
		    if (name.contains(" " + entry.getKey() + " ")) {
		    	if (logActivated) Logging.printLogDebug(logger, "-- Found UNIT MIDDLE: " + entry.getKey() + "\n    '" + name + "' -> '");
		    	name = name.replaceAll(" " + entry.getKey() + " ", " " + entry.getValue() + " ");
		    	if (logActivated) Logging.printLogDebug(logger, name + "'");
		    }
		}
		
		// Recipient, only when is in the middle of string
		for (Map.Entry<String, String> entry : this.recipientsReplaceMap.entrySet()) {
			if (name.contains(" " + entry.getKey() + " ")) {
	        	if(logActivated) Logging.printLogDebug(logger, "-- Found RECIPIENT: " + entry.getKey() + "\n    '" + name + "'");
	        	name = name.replaceAll(" " + entry.getKey() + " ", " " + entry.getValue() + " ");
	        	if(logActivated) Logging.printLogDebug(logger, "--> '" + name + "'");
	        }
		}
		
		// Brands
		for (Map.Entry<String, String> entry : this.brandsReplaceMap.entrySet()) {
			// If in beginning
			Pattern beginning = Pattern.compile("^" + Pattern.quote(entry.getKey()));
			Matcher matcherBeginning = beginning.matcher(name);
			while (matcherBeginning.find()) {
				if (logActivated) Logging.printLogDebug(logger, "-- Found BRAND ON BEGINNING: " + entry.getKey() + "\n    '" + name + "' -> '");
				name = entry.getValue() + " " + name.substring(matcherBeginning.end());
				if (logActivated) Logging.printLogDebug(logger, name + "'");
			}
			
			// If on ending of string
			Pattern ending = Pattern.compile(Pattern.quote(entry.getKey()) + "$");
			Matcher matcherEnding = ending.matcher(name);
			while (matcherEnding.find()) {
				if (logActivated) Logging.printLogDebug(logger, "-- Found BRAND ON ENDING: " + entry.getKey() + "\n    '" + name + "' -> '");
				name = name.substring(0, matcherEnding.start()) + " " + entry.getValue();
				if (logActivated) Logging.printLogDebug(logger, name + "'");
			}
			
			// If in the middle of string
			if (name.contains(" " + entry.getKey() + " ")) {
		    	if (logActivated) Logging.printLogDebug(logger, "-- Found BRAND MIDDLE: " + entry.getKey() + "\n    '" + name + "' -> '");
		    	name = name.replaceAll(" " + entry.getKey() + " ", " " + entry.getValue() + " ");
		    	if (logActivated) Logging.printLogDebug(logger, name + "'");
		    }
		}
				
		// Identifiers, always the begin of string
		for (Map.Entry<String, String> entry : this.identifiersReplaceMap.entrySet()) {
			Pattern pattern = Pattern.compile("^" + Pattern.quote(entry.getKey()));
		    Matcher matcher = pattern.matcher(name);
		    while (matcher.find()) {
		    	if (logActivated) Logging.printLogDebug(logger, "-- Found IDENTIFIER ON BEGINNING: " + entry.getKey() + "\n    '" + name + "' -> '");
		    	name = entry.getValue() + " " + name.substring(matcher.end());
		    	if (logActivated) Logging.printLogDebug(logger, name + "'");
		    }
		    while (name.contains("  "))  name = name.replace("  ", " ");
			name = name.trim();
		}

		if (logActivated) Logging.printLogDebug(logger, "----> Final word of processer: " + name);
		
		return new ArrayList<>(Arrays.asList(name.split(" ")));
	}
	
	public boolean isLogActivated() {
		return this.logActivated;
	}

	public void setLogActivated(boolean logActivated) {
		this.logActivated = logActivated;
	}	
}
