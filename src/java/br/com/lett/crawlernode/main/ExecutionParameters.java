package br.com.lett.crawlernode.main;

import br.com.lett.crawlernode.core.server.PoolExecutor;
import br.com.lett.crawlernode.util.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionParameters {

   private static final Logger logger = LoggerFactory.getLogger(ExecutionParameters.class);

   public static final String ENVIRONMENT_DEVELOPMENT = "development";
   public static final String ENVIRONMENT_PRODUCTION = "production";
   public static final String DEFAULT_CRAWLER_VERSION = "-1";


   /**
    * In case we want to force image update on Amazon bucket, when downloading images In some cases the
    * crawler must update the redimensioned versions of images, and we must use this option in case we
    * want to force this, even if the image on market didn't changed.
    */
   private boolean forceImageUpdate;

   private int hikariCpMinIdle;
   private int hikariCpMaxPoolSize;
   private int hikariCpValidationTimeout;
   private int hikariCpConnectionTimeout;
   private int hikariCpIdleTimeout;

   private String queueUrlFirstPart;
   private String fetcherUrl;
   private String replicatorUrl;
   private String tmpImageFolder;
   private String phantomjsPath;
   private int nthreads;
   private int coreThreads;
   private String environment;
   private String version;
   private Boolean debug;
   private Boolean useFetcher;
   private String kinesisStream;
   private String kinesisRatingStream = null;
   private String newAwsAccessKey;
   private String newAwsSecretKey;
   private Boolean sendToKinesis;

   private String logsBucketName;

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
      kinesisStream = getEnvKinesisStream();
      kinesisRatingStream = getEnvKinesisRatingStream();
      sendToKinesis = getEnvSendToKinesis();
      logsBucketName = getEnvLogsBucketName();
      newAwsAccessKey = getEnvNewAwsAccessKey();
      newAwsSecretKey = getEnvNewAwsSecretKey();
      setQueueUrlFirstPart(getEnvQueueUrlFirstPart());
      setFetcherUrl(getEnvFetcherUrl());
      setPhantomjsPath(getEnvPhantomjsPath());
      setHikariCpConnectionTimeout();
      setHikariCpIDLETimeout();
      setHikariCpMaxPoolSize();
      setHikariCpMinIDLE();
      setHikariCpValidationTimeout();
      setUseFetcher(getEnvUseFetcher());
      setReplicatorUrl(getEnvReplicatorUrl());
      version = DEFAULT_CRAWLER_VERSION;

      Logging.printLogDebug(logger, this.toString());
   }


   private String getEnvNewAwsSecretKey() {
      return System.getenv(EnvironmentVariables.NEW_AWS_SECRET_KEY);
   }

   private String getEnvNewAwsAccessKey() {
      return System.getenv(EnvironmentVariables.NEW_AWS_ACCESS_KEY);
   }

   public Boolean mustSendToKinesis() {
      return sendToKinesis;
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
      sb.append("Use Fetcher: " + this.useFetcher);
      sb.append("\n");
      sb.append("Version: " + this.version);
      sb.append("\n");

      return sb.toString();
   }

   private boolean getEnvDebug() {
      String debugEnv = System.getenv(EnvironmentVariables.ENV_DEBUG);
      return debugEnv != null;
   }

   private boolean getEnvSendToKinesis() {
      String sendToKinesis = System.getenv(EnvironmentVariables.SEND_TO_KINESIS);
      return Boolean.TRUE.toString().equals(sendToKinesis);
   }

   private String getEnvReplicatorUrl() {
      return System.getenv(EnvironmentVariables.REPLICATOR_URL);
   }

   private String getEnvPhantomjsPath() {
      return System.getenv(EnvironmentVariables.ENV_PHANTOMJS_PATH);
   }

   private String getEnvQueueUrlFirstPart() {
      return System.getenv(EnvironmentVariables.QUEUE_URL_FIRST_PART);
   }

   private String getEnvFetcherUrl() {
      return System.getenv(EnvironmentVariables.FETCHER_URL);
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
      return forceImgUpdate != null;
   }

   private String getEnvKinesisStream() {
      return System.getenv(EnvironmentVariables.KINESIS_STREAM);
   }

   private String getEnvKinesisRatingStream() {
      return System.getenv(EnvironmentVariables.KINESIS_RATING_STREAM);
   }

   private boolean getEnvUseFetcher() {
      String useFetcher = System.getenv(EnvironmentVariables.USE_FETCHER);
      return useFetcher != null && useFetcher.equals("true");
   }

   private int getEnvCoreThreads() {
      String coreThreadsString = System.getenv(EnvironmentVariables.ENV_CORE_THREADS);
      if (coreThreadsString == null) {
         return PoolExecutor.DEFAULT_NTHREADS;
      }
      return Integer.parseInt(coreThreadsString);
   }

   private String getEnvLogsBucketName() {
      String logsBucketName = System.getenv(EnvironmentVariables.LOGS_BUCKET_NAME);
      if (logsBucketName == null || logsBucketName.isEmpty()) {
         Logging.logWarn(logger, null, null, "LOGS_BUCKET_NAME not set");

         // Return empty string to avoid null pointers
         return "";
      }

      return logsBucketName;
   }

   public boolean mustForceImageUpdate() {
      return forceImageUpdate;
   }

   public int getCoreThreads() {
      return this.coreThreads;
   }

   public String getKinesisStream() {
      return kinesisStream;
   }

   public String getKinesisRatingStream() {
      return kinesisRatingStream;
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

   public Boolean getUseFetcher() {
      return useFetcher;
   }

   public void setUseFetcher(Boolean useFetcher) {
      this.useFetcher = useFetcher;
   }

   public int getHikariCpMinIDLE() {
      return hikariCpMinIdle;
   }

   public void setHikariCpMinIDLE() {
      this.hikariCpMinIdle = Integer.parseInt(System.getenv(EnvironmentVariables.HIKARI_CP_MIN_IDLE));
   }

   public int getHikariCpMaxPoolSize() {
      return hikariCpMaxPoolSize;
   }

   public void setHikariCpMaxPoolSize() {
      this.hikariCpMaxPoolSize = Integer.parseInt(System.getenv(EnvironmentVariables.HIKARI_CP_MAX_POOL_SIZE));
   }

   public int getHikariCpValidationTimeout() {
      return hikariCpValidationTimeout;
   }

   public void setHikariCpValidationTimeout() {
      this.hikariCpValidationTimeout = Integer.parseInt(System.getenv(EnvironmentVariables.HIKARI_CP_VALIDATION_TIMEOUT));
   }

   public int getHikariCpConnectionTimeout() {
      return hikariCpConnectionTimeout;
   }

   public void setHikariCpConnectionTimeout() {
      this.hikariCpConnectionTimeout = Integer.parseInt(System.getenv(EnvironmentVariables.HIKARI_CP_CONNECTION_TIMEOUT));
   }

   public int getHikariCpIDLETimeout() {
      return hikariCpIdleTimeout;
   }

   public void setHikariCpIDLETimeout() {
      this.hikariCpIdleTimeout = Integer.parseInt(System.getenv(EnvironmentVariables.HIKARI_CP_IDLE_TIMEOUT));
   }

   public String getQueueUrlFirstPart() {
      return queueUrlFirstPart;
   }

   public void setQueueUrlFirstPart(String queueUrlFirstPart) {
      this.queueUrlFirstPart = queueUrlFirstPart;
   }

   public String getFetcherUrl() {
      return fetcherUrl;
   }

   public String getNewAwsAccessKey() {
      return newAwsAccessKey;
   }

   public String getNewAwsSecretKey() {
      return newAwsSecretKey;
   }

   public void setFetcherUrl(String fetcherUrl) {
      this.fetcherUrl = fetcherUrl;
   }

   public String getReplicatorUrl() {
      return replicatorUrl;
   }

   public void setReplicatorUrl(String replicatorUrl) {
      this.replicatorUrl = replicatorUrl;
   }

   public String getLogsBucketName() {
      return logsBucketName;
   }

   public void setLogsBucketName(String logsBucketName) {
      this.logsBucketName = logsBucketName;
   }
}
