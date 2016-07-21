package br.com.lett.crawlernode.base;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.util.Logging;

public class ExecutionParameters {

	private static final Logger logger = LoggerFactory.getLogger(ExecutionParameters.class);

	public static final String ENVIRONMENT_DEVELOPMENT	= "development";
	public static final String ENVIRONMENT_PRODUCTION	= "production";

	public static final String MODE_DISCOVERY = "discovery";
	public static final String MODE_NORMAL = "normal";

	private Options options;
	private String environment;
	private Boolean debug;
	private String mode;
	private String[] args;

	public ExecutionParameters(String[] args) {
		this.args = args;
		options = new Options();
		debug = null;
	}

	public void setUpExecutionParameters() {
		this.createOptions();
		this.parseCommandLineOptions();

		Logging.printLogDebug(logger, this.toString());
	}

	public Boolean getDebug() {
		return this.debug;
	}

	public String getEnvironment() {
		return this.environment;
	}

	private void createOptions() {

		options.addOption("h", "help", false, "Show help");
		options.addOption("debug", false, "Debug mode for logging debug level messages on console");
		options.addOption("environment", true, "Environment [development, production]");
		options.addOption("mode", true, "Environment [development, production]");

	}

	private void parseCommandLineOptions() {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);

			// Debug mode
			debug = cmd.hasOption("debug");

			// Environment
			if (cmd.hasOption("environment")) 	environment = cmd.getOptionValue("environment"); else help(); // required

			// Mode
			if (cmd.hasOption("mode")) 	mode = cmd.getOptionValue("mode"); else help(); // required

		} catch (ParseException e) {
			Logging.printLogError(logger, " Failed to parse comand line properties.");
			Logging.printLogError(logger, e.getMessage());
			help();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("Debug: " + this.debug + "\n");
		sb.append("Environment: " + this.environment + "\n");

		return sb.toString();
	} 

	private void help() {
		new HelpFormatter().printHelp("Main", this.options);
		System.exit(0);
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}	


}
