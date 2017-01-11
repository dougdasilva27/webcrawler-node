package br.com.lett.crawlernode.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private AmazonSQS sqs;

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

		Logging.printLogDebug(logger, "Authenticating on Amazon SQS service...");
		sqs = new AmazonSQSClient(credentials);
		sqs.setRegion(usEast1);
	}

	public AmazonSQS getSqs() {
		return sqs;
	}

}
