package br.com.lett.crawlernode.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.server.PoolExecutor;
import br.com.lett.crawlernode.util.Logging;

public class ExecutionParameters {

	private static final Logger logger = LoggerFactory.getLogger(ExecutionParameters.class);

	public static final String ENVIRONMENT_DEVELOPMENT	= "development";
	public static final String ENVIRONMENT_PRODUCTION	= "production";
	public static final String DEFAULT_CRAWLER_VERSION = "-1";
	

	/** 
	 * In case we want to force image update on Amazon bucket, when downloading images
	 * In some cases the crawler must update the redimensioned versions of images, and we must use
	 * this option in case we want to force this, even if the image on market didn't changed. 
	 */
	private boolean forceImageUpdate;
	

	private String tmpImageFolder;
	private String phantomjsPath;
	private int nthreads;
	private int coreThreads;
	private String environment;
	private String version;
	private Boolean debug;

	public ExecutionParameters() {
		debug = null;
	}

	public void setUpExecutionParameters() {
		nthreads = getEnvNumOfThreads();
		coreThreads = getEnvCoreThreads();
		debug = getEnvDebug();
		forceImageUpdate = getEnvForceImgUpdate();
		environment = getEnvEnvironment();
		tmpImageFolder = getEnvTmpImagesFolder();
		setPhantomjsPath(getEnvPhantomjsPath());
		version = DEFAULT_CRAWLER_VERSION;

		Logging.printLogDebug(logger, this.toString());
	}

	public Boolean getDebug() {
		return debug;
	}

	public String getEnvironment() {
		return environment;
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
		sb.append("PhantomjsPath: " + this.phantomjsPath);
		sb.append("\n");
		sb.append("Version: " + this.version);
		sb.append("\n");

		return sb.toString();
	}

	private boolean getEnvDebug() {
		String debugEnv = System.getenv(EnvironmentVariables.ENV_DEBUG);
		if (debugEnv != null) {
			return true;
		}
		return false;
	}
	
	private String getEnvPhantomjsPath() {
		return System.getenv(EnvironmentVariables.ENV_PHANTOMJS_PATH);
	}

	private String getEnvTmpImagesFolder() {
		return System.getenv(EnvironmentVariables.ENV_TMP_IMG_FOLDER);
	}

	private String getEnvEnvironment() {
		return System.getenv(EnvironmentVariables.ENV_ENVIRONMENT);
	}

	private int getEnvNumOfThreads() {
		String nThreads = System.getenv(EnvironmentVariables.ENV_NTHREADS);
		if (nThreads == null) {
			return PoolExecutor.DEFAULT_NTHREADS;
		}
		return Integer.parseInt(nThreads);
	}

	private boolean getEnvForceImgUpdate() {
		String forceImgUpdate = System.getenv(EnvironmentVariables.ENV_FORCE_IMG_UPDATE);
		if (forceImgUpdate != null) {
			return true;
		}
		return false;
	}

	private int getEnvCoreThreads() {
		String coreThreadsString = System.getenv(EnvironmentVariables.ENV_CORE_THREADS);
		if (coreThreadsString == null) {
			return PoolExecutor.DEFAULT_NTHREADS;
		}
		return Integer.parseInt(coreThreadsString);
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

	public String getPhantomjsPath() {
		return phantomjsPath;
	}

	public void setPhantomjsPath(String phantomjsPath) {
		this.phantomjsPath = phantomjsPath;
	}
}
