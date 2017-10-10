package br.com.lett.crawlernode.aws.sqs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.util.Logging;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

/**
 * This class only performs authentication on the desired Amazon SQS queue and
 * creates the queue instance.
 * When running in development environment, only the development queue is created.
 * Analogous for modes insights and discovery in production environment.
 * 
 * @author Samir Leão
 *
 */
public class QueueHandler {

	protected static final Logger LOGGER = LoggerFactory.getLogger(QueueHandler.class);

	private AmazonSQS sqsClient;

	public QueueHandler() {
		Logging.printLogDebug(LOGGER, "Authenticating on Amazon SQS service...");
		sqsClient = AmazonSQSClientBuilder
				.standard()
				.withRegion(Regions.US_EAST_1)
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.build();
	}

	public AmazonSQS getSqs() {
		return sqsClient;
	}

}
