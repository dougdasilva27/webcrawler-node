package br.com.lett.crawlernode.main;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.server.PoolExecutor;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class ExecutionParameters {

	private static final Logger logger = LoggerFactory.getLogger(ExecutionParameters.class);

	public static final String ENVIRONMENT_DEVELOPMENT	= "development";
	public static final String ENVIRONMENT_PRODUCTION	= "production";
	public static final String DEFAULT_CRAWLER_VERSION = "-1";
	
	public static final String SEED = "seed";
	public static final String INSIGHTS = "insights";
	public static final String DISCOVER = "discover";
	public static final String IMAGES = "images";
	public static final String RATING = "rating";

	private static final String ENV_NTHREADS 			= "CRAWLER_THREADS";
	private static final String ENV_CORE_THREADS 		= "CRAWLER_CORE_THREADS";
	private static final String ENV_DEBUG				= "DEBUG";
	private static final String ENV_TMP_IMG_FOLDER		= "TMP_IMG_FOLDER";
	private static final String ENV_ENVIRONMENT			= "ENVIRONMENT";

	private Options options;
	private String environment;
	private String version;
	private Boolean debug;

	/** 
	 * In case we want to force image update on Amazon bucket, when downloading images
	 * In some cases the crawler must update the redimensioned versions of images, and we must use
	 * this option in case we want to force this, even if the image on market didn't changed. 
	 */
	private boolean forceImageUpdate;

	private String tmpImageFolder;
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

		// get the number of core threads on environment variable
		this.coreThreads = getEnvCoreThreads();

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
		options.addOption("force_image_update", false, "Force image updates on Amazon bucket");
		options.addOption("version", true, "Crawler node version");

	}

	private void parseCommandLineOptions() {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);

			debug = getEnvDebug();
			forceImageUpdate = cmd.hasOption("force_image_update");
			environment = getEnvEnvironment();
			tmpImageFolder = getEnvTmpImagesFolder();

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
		sb.append("Debug: " + this.debug);
		sb.append("\n");
		sb.append("Environment: " + this.environment);
		sb.append("\n");
		sb.append("Force image update: " + this.forceImageUpdate);
		sb.append("\n");
		sb.append("Version: " + this.version);
		sb.append("\n");

		return sb.toString();
	} 

	private void help() {
		new HelpFormatter().printHelp("Main", this.options);
		System.exit(0);
	}
	
	private boolean getEnvDebug() {
		String debugEnv = System.getenv(ENV_DEBUG);
		if (debugEnv != null) {
			return true;
		}
		return false;
	}
	
	private String getEnvTmpImagesFolder() {
		return System.getenv(ENV_TMP_IMG_FOLDER);
	}
	
	private String getEnvEnvironment() {
		return System.getenv(ENV_ENVIRONMENT);
	}

	private int getEnvNumOfThreads() {
		String nThreads = System.getenv(ENV_NTHREADS);
		if (nThreads == null) {
			return PoolExecutor.DEFAULT_NTHREADS;
		}
		return Integer.parseInt(nThreads);
	}

	private int getEnvCoreThreads() {
		String coreThreads = System.getenv(ENV_CORE_THREADS);
		if (coreThreads == null) {
			return PoolExecutor.DEFAULT_NTHREADS;
		}
		return Integer.parseInt(coreThreads);
	}

	public boolean mustForceImageUpdate() {
		return forceImageUpdate;
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

	public String getTmpImageFolder() {
		return this.tmpImageFolder;
	}

	public void setTmpImageFolder(String tmpImageFolder) {
		this.tmpImageFolder = tmpImageFolder;
	}
}
