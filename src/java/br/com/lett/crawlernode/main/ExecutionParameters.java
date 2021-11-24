package br.com.lett.crawlernode.main;

import br.com.lett.crawlernode.util.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionParameters {

   public static final String ENVIRONMENT_DEVELOPMENT = "development";
   public static final String ENVIRONMENT_PRODUCTION = "production";
   public static final String DEFAULT_CRAWLER_VERSION = "-1";
   private static final Logger logger = LoggerFactory.getLogger(ExecutionParameters.class);
   /**
    * In case we want to force image update on Amazon bucket, when downloading images In some cases the crawler must update the redimensioned versions of images, and we must use this option in case we
    * want to force this, even if the image on market didn't changed.
    */
   private boolean forceImageUpdate;

   private int hikariCpMinIdle;
   private int hikariCpMaxPoolSize;
   private int hikariCpValidationTimeout;
   private int hikariCpConnectionTimeout;
   private int hikariCpIdleTimeout;

   private int threads;
   private String queueUrlFirstPart;
   private String fetcherUrl;
   private String replicatorUrl;
   private String tmpImageFolder;
   private String environment;
   private String version;
   private Boolean debug;
   private Boolean useFetcher;
   private String kinesisStream;
   private boolean sendToKinesis;
   private String kinesisStreamCatalog;
   private boolean sendToKinesisCatalog;
   private String kinesisStreamRanking;

   private boolean sendToKinesisRanking;

   private String logsBucketName;

   private String imagesBucketName;
   private String imagesBucketNameNew;
   private String s3BatchRemoteLocation;

   private String s3BatchHost;
   private String s3BatchUser;
   private String s3BatchPass;
   private String redisHost;

   private Integer redisPort;
   public ExecutionParameters() {
      debug = null;
   }

   public void setUpExecutionParameters() {
      debug = getEnvDebug();
      forceImageUpdate = getEnvForceImgUpdate();
      environment = getEnvEnvironment();
      tmpImageFolder = getEnvTmpImagesFolder();
      kinesisStream = getEnvKinesisStream();
      kinesisStreamCatalog = getEnvKinesisStreamCatalog();
      kinesisStreamRanking = getEnvKinesisStreamRanking();
      sendToKinesis = getEnvSendToKinesis();
      sendToKinesisCatalog = getEnvSendToKinesisCatalog();
      sendToKinesisRanking = getEnvSendToKinesisRanking();
      logsBucketName = getEnvLogsBucketName();
      imagesBucketName = getEnvImagesBucketName();
      imagesBucketNameNew = getEnvImagesBucketNameNew();
      s3BatchHost = System.getenv(EnvironmentVariables.S3_BATCH_HOST);
      s3BatchRemoteLocation = System.getenv(EnvironmentVariables.S3_BATCH_REMOTE_LOCATION);
      s3BatchUser = System.getenv(EnvironmentVariables.S3_BATCH_USER);
      s3BatchPass = System.getenv(EnvironmentVariables.S3_BATCH_PASS);
      threads = System.getenv(EnvironmentVariables.ENV_CORE_THREADS) != null ? Integer.parseInt(System.getenv(EnvironmentVariables.ENV_CORE_THREADS)) : 20;
      setQueueUrlFirstPart(getEnvQueueUrlFirstPart());
      setFetcherUrl(getEnvFetcherUrl());
      setHikariCpConnectionTimeout();
      setHikariCpIDLETimeout();
      setHikariCpMaxPoolSize();
      setHikariCpMinIDLE();
      setHikariCpValidationTimeout();
      setUseFetcher(getEnvUseFetcher());
      setReplicatorUrl(getEnvReplicatorUrl());
      setChromePath();

      version = DEFAULT_CRAWLER_VERSION;
      setRedisHost();
      setRedisPort();
      Logging.printLogDebug(logger, this.toString());
   }

   public void setRedisHost() {
      redisHost = System.getenv(EnvironmentVariables.REDIS_HOST);
      if (redisHost == null) {
         redisHost = "redis-crawler-prod.2k0spf.0001.use1.cache.amazonaws.com";
      }
   }

   public void setRedisPort() {
      String redisPortEnv = System.getenv(EnvironmentVariables.REDIS_PORT);
      if (redisPortEnv != null) {
         redisPort = Integer.parseInt(redisPortEnv);
      } else {
         redisPort = 6379;
      }
   }

   public void setChromePath() {
      String chromePath = System.getenv(EnvironmentVariables.CHROME_PATH);
      if (chromePath == null) {
         Logging.logWarn(logger, null, null, EnvironmentVariables.CHROME_PATH + " not set");
         System.setProperty("webdriver.chrome.driver", "/home/chrome/chromedriver");
      } else {
         System.setProperty("webdriver.chrome.driver", chromePath);
      }
   }

   public boolean mustSendToKinesis() {
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
      return Boolean.TRUE.toString().equals(System.getenv(EnvironmentVariables.SEND_TO_KINESIS));
   }

   private boolean getEnvSendToKinesisCatalog() {
      return Boolean.TRUE.toString().equals(System.getenv(EnvironmentVariables.SEND_TO_KINESIS_CATALOG));
   }

   private boolean getEnvSendToKinesisRanking() {
      return Boolean.TRUE.toString().equals(System.getenv(EnvironmentVariables.SEND_TO_KINESIS_RANKING));
   }

   private String getEnvReplicatorUrl() {
      return System.getenv(EnvironmentVariables.REPLICATOR_URL);
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

   private boolean getEnvForceImgUpdate() {
      String forceImgUpdate = System.getenv(EnvironmentVariables.ENV_FORCE_IMG_UPDATE);
      return forceImgUpdate != null;
   }

   private String getEnvKinesisStream() {
      return System.getenv(EnvironmentVariables.KINESIS_STREAM);
   }


   private String getEnvKinesisStreamCatalog() {
      return System.getenv(EnvironmentVariables.KINESIS_STREAM_CATALOG);
   }

   private String getEnvKinesisStreamRanking() {
      return System.getenv(EnvironmentVariables.KINESIS_STREAM_RANKING);
   }

   private boolean getEnvUseFetcher() {
      String useFetcher = System.getenv(EnvironmentVariables.USE_FETCHER);
      return useFetcher != null && useFetcher.equals("true");
   }

   public int getThreads() {
      return threads;
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

   private String getEnvImagesBucketName() {
      String logsBucketName = System.getenv(EnvironmentVariables.IMAGES_BUCKET_NAME);
      if (logsBucketName == null || logsBucketName.isEmpty()) {
         Logging.logWarn(logger, null, null, "IMAGES_BUCKET_NAME not set");

         // Return empty string to avoid null pointers
         return "";
      }

      return logsBucketName;
   }

   private String getEnvImagesBucketNameNew() {
      String logsBucketName = System.getenv(EnvironmentVariables.IMAGES_BUCKET_NAME_NEW);
      if (logsBucketName == null || logsBucketName.isEmpty()) {
         Logging.logWarn(logger, null, null, "IMAGES_BUCKET_NAME_NEW not set");

         // Return empty string to avoid null pointers
         return "";
      }

      return logsBucketName;
   }

   public boolean mustForceImageUpdate() {
      return forceImageUpdate;
   }

   public String getKinesisStream() {
      return kinesisStream;
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

   public String getImagesBucketName() {
      return imagesBucketName;
   }

   public void setImagesBucketName(String imagesBucketName) {
      this.imagesBucketName = imagesBucketName;
   }

   public String getImagesBucketNameNew() {
      return imagesBucketNameNew;
   }

   public void setImagesBucketNameNew(String imagesBucketNameNew) {
      this.imagesBucketNameNew = imagesBucketNameNew;
   }

   public String getS3BatchHost() {
      return s3BatchHost;
   }

   public String getS3BatchRemoteLocation() {
      return s3BatchRemoteLocation;
   }

   public String getS3BatchUser() {
      return s3BatchUser;
   }

   public String getS3BatchPass() {
      return s3BatchPass;
   }

   public String getRedisHost() {
      return redisHost;
   }

   public Integer getRedisPort() {
      return redisPort;
   }

   public String getKinesisStreamCatalog() {
      return kinesisStreamCatalog;
   }

   public boolean isSendToKinesisCatalog() {
      return sendToKinesisCatalog;
   }

   public String getKinesisStreamRanking() {
      return kinesisStreamRanking;
   }

   public boolean isSendToKinesisRanking() {
      return sendToKinesisRanking;
   }
}
