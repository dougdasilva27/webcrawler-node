package br.com.lett.crawlernode.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.Logging;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;

/**
 * This class only performs authentication on the desired Amazon SQS queue and
 * creates the queue instance.
 * When running in development environment, only the development queue is created.
 * Analogous for modes insights and discovery in production environment.
 * @author Samir Leao
 *
 */
public class QueueHandler {

	protected static final Logger logger = LoggerFactory.getLogger(QueueHandler.class);

	private final String AWS_ACCESS_KEY = "AKIAJ73Z3NTUDN2IF7AA";
	private final String SECRET_KEY = "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";
	
	public static final String DEVELOPMENT = "crawler-development";
	public static final String INSIGHTS = "crawler-insights";
	public static final String INSIGHTS_DEAD = "crawler-insights-dead";
	public static final String DISCOVER = "crawler-discover";
	public static final String DISCOVER_DEAD = "crawler-discover-dead";
	public static final String SEED = "crawler-seed";
	public static final String SEED_DEAD = "crawler-seed-dead";

	/** Amazon sqs queue to be used only in production mode */
	private AmazonSQS sqsInsights;
	
	/** Dead letter messages from Insights queue */
	private AmazonSQS sqsInsightsDead;
	
	private AmazonSQS sqsDiscovery;
	private AmazonSQS sqsDiscoveryDead;
	
	private AmazonSQS sqsSeed;
	private AmazonSQS sqsSeedDead;
	
	/** Amazon sqs queue to be used only in development mode */
	private AmazonSQS sqsDevelopment;
	
	
	/**
	 * Default constructor for QueueHandler.
	 * Perform authentication on Amazon services and creates an instance
	 * of the appropriate SQS queue, according to environment and execution mode.
	 */
	public QueueHandler() {
		AWSCredentials credentials = null;
		try {
			credentials = new BasicAWSCredentials(AWS_ACCESS_KEY, SECRET_KEY);
		} catch (Exception e) {
			throw new AmazonClientException("Cannot create credentials", e);
		}
		
		Region usEast1 = Region.getRegion(Regions.US_EAST_1);

		// creating queues for environment production
		if (Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION)) {
			
			Logging.printLogDebug(logger, "Authenticating on " + INSIGHTS + " queue...");
			sqsInsights = new AmazonSQSClient(credentials);
			sqsInsights.setRegion(usEast1);
			
			Logging.printLogDebug(logger, "Authenticating on " + INSIGHTS_DEAD + " queue...");
			sqsInsightsDead = new AmazonSQSClient(credentials);
			sqsInsightsDead.setRegion(usEast1);
			
			Logging.printLogDebug(logger, "Authenticating on " + DISCOVER + " queue...");
			sqsDiscovery = new AmazonSQSClient(credentials);
			sqsDiscovery.setRegion(usEast1);
			
			Logging.printLogDebug(logger, "Authenticating on " + DISCOVER_DEAD + " queue...");
			sqsDiscoveryDead = new AmazonSQSClient(credentials);
			sqsDiscoveryDead.setRegion(usEast1);
			
			Logging.printLogDebug(logger, "Authenticating on " + SEED + " queue...");
			sqsSeed = new AmazonSQSClient(credentials);
			sqsSeed.setRegion(usEast1);
			
			Logging.printLogDebug(logger, "Authenticating on " + SEED_DEAD + " queue...");
			sqsSeedDead = new AmazonSQSClient(credentials);
			sqsSeedDead.setRegion(usEast1);
			
		}

		// creating queue for environment development
		else {
			sqsDevelopment = new AmazonSQSClient(credentials);
			sqsDevelopment.setRegion(usEast1);
		}

	}
	
	/**
	 * Get AmazonSQS queue according to it's name
	 * 
	 * @param queueName
	 * @return the desired AmazonSQS
	 */
	public AmazonSQS getQueue(String queueName) {
		if (queueName.equals(INSIGHTS)) return sqsInsights;
		if (queueName.equals(INSIGHTS_DEAD)) return sqsInsightsDead;
		
		if (queueName.equals(DISCOVER)) return sqsDiscovery;
		if (queueName.equals(DISCOVER_DEAD)) return sqsDiscoveryDead;
		
		if (queueName.equals(SEED)) return sqsSeed;
		if (queueName.equals(SEED_DEAD)) return sqsSeedDead;
		
		if (queueName.equals(DEVELOPMENT)) return sqsDevelopment;
		
		Logging.printLogError(logger, "Unrecognized queue.");
		return null;
	}
	
	

}
