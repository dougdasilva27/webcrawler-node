package br.com.lett.crawlernode.main;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.kernel.task.TaskExecutor;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class ExecutionParameters {

	private static final Logger logger = LoggerFactory.getLogger(ExecutionParameters.class);

	public static final String ENVIRONMENT_DEVELOPMENT	= "development";
	public static final String ENVIRONMENT_PRODUCTION	= "production";
	public static final String DEFAULT_CRAWLER_VERSION = "-1";
	
	/**
	 * Crawler will get messages from the tracked skus queue
	 */
	public static final String MODE_INSIGHTS = "insights";
	
	/**
	 * Crawler will get messages from the sku URL suggestions queue
	 */
	public static final String MODE_DISCOVERY = "discovery";
	
	/**
	 * Crawler will get messages from the queue for dead letter
	 */
	public static final String MODE_DEAD_LETTER = "dead";
	
	/**
	 * The maximum number of threads that can be used by the crawler
	 */
	private static final String ENV_NTHREADS = "CRAWLER_THREADS";
	
	private static final String ENV_CORE_THREADS = "CRAWLER_CORE_THREADS";

	private Options options;
	private String environment;
	private String mode;
	private String version;
	private Boolean debug;
	private String[] args;
	
	/**
	 * Number of threads used by the crawler
	 * Value is passed by an environment variable
	 */
	private int nthreads;
	
	private int coreThreads;

	public ExecutionParameters(String[] args) {
		this.args = args;
		options = new Options();
		debug = null;
	}

	public void setUpExecutionParameters() {
		this.createOptions();
		this.parseCommandLineOptions();
		
		// get the number of threads on environment variable
		this.nthreads = getEnvNumOfThreads();
		
		this.coreThreads = getEnvCoreThreads();

		Logging.printLogDebug(logger, this.toString());
	}

	public Boolean getDebug() {
		return this.debug;
	}

	public String getEnvironment() {
		return this.environment;
	}
	
	public String getMode() {
		return this.mode;
	}

	private void createOptions() {

		options.addOption("h", "help", false, "Show help");
		options.addOption("debug", false, "Debug mode for logging debug level messages on console");
		options.addOption("environment", true, "Environment [development, production]");
		options.addOption("mode", true, "Mode [insights, discovery, dead]");
		options.addOption("version", true, "Crawler node version");

	}

	private void parseCommandLineOptions() {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);

			// debug mode
			debug = cmd.hasOption("debug");

			// environment
			if (cmd.hasOption("environment")) {
				environment = cmd.getOptionValue("environment");
				if (!environment.equals(ENVIRONMENT_DEVELOPMENT) && !environment.equals(ENVIRONMENT_PRODUCTION)) {
					Logging.printLogError(logger, "Unrecognized environment.");
					help();
				}
			} else {
				help();
			}

			// mode
			if (cmd.hasOption("mode")) {
				mode = cmd.getOptionValue("mode");
				if (!mode.equals(MODE_INSIGHTS) && !mode.equals(MODE_DISCOVERY) && !mode.equals(MODE_DEAD_LETTER)) {
					Logging.printLogError(logger, "Unrecognized mode.");
					help();
				}
			} else {
				help();
			}
			
			// version
			if (cmd.hasOption("version")) {
				version = cmd.getOptionValue("version");
			} else {
				version = DEFAULT_CRAWLER_VERSION;
			}

		} catch (ParseException e) {
			Logging.printLogError(logger, " Failed to parse comand line properties.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
			help();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("\n");
		sb.append("Debug: " + this.debug + "\n");
		sb.append("Environment: " + this.environment + "\n");
		sb.append("Mode: " + this.mode + "\n");
		sb.append("Version: " + this.version + "\n");

		return sb.toString();
	} 

	private void help() {
		new HelpFormatter().printHelp("Main", this.options);
		System.exit(0);
	}
	
	private int getEnvNumOfThreads() {
		String nThreads = System.getenv(ENV_NTHREADS);
		if (nThreads == null) return TaskExecutor.DEFAULT_NTHREADS;
		return Integer.parseInt(nThreads);
	}
	
	private int getEnvCoreThreads() {
		String coreThreads = System.getenv(ENV_CORE_THREADS);
		if (coreThreads == null) return TaskExecutor.DEFAULT_NTHREADS;
		return Integer.parseInt(coreThreads);
	}
	
	public int getCoreThreads() {
		return this.coreThreads;
	}

	public int getNthreads() {
		return nthreads;
	}

	public void setNthreads(int nthreads) {
		this.nthreads = nthreads;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
