package br.com.lett.crawlernode.test;

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

public class TestExecutionParameters {

	private static final Logger logger = LoggerFactory.getLogger(TestExecutionParameters.class);
	
	public static final String ENVIRONMENT_DEVELOPMENT	= "development";
	public static final String ENVIRONMENT_PRODUCTION	= "production";
	
	/**
	 * The maximum number of threads that can be used by the crawler
	 */
	private static final String ENV_NTHREADS = "CRAWLER_THREADS";

	private Options options;
	private Boolean debug;
	private String[] args;
	
	/**
	 * Number of threads used by the crawler
	 * Value is passed by an environment variable
	 */
	private int nthreads;

	public TestExecutionParameters(String[] args) {
		this.args = args;
		options = new Options();
		debug = null;
	}

	public void setUpExecutionParameters() {
		this.createOptions();
		this.parseCommandLineOptions();
		
		// get the number of threads on environment variable
		this.nthreads = getEnvNumOfThreads();

		Logging.printLogDebug(logger, this.toString());
	}

	public Boolean getDebug() {
		return this.debug;
	}

	private void createOptions() {
		options.addOption("h", "help", false, "Show help");
		options.addOption("debug", false, "Debug mode for logging debug level messages on console");
	}

	private void parseCommandLineOptions() {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);

			// debug mode
			debug = cmd.hasOption("debug");

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

	public int getNthreads() {
		return nthreads;
	}

	public void setNthreads(int nthreads) {
		this.nthreads = nthreads;
	}

}
