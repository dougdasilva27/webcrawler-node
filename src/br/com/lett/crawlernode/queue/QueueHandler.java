package br.com.lett.crawlernode.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;

import br.com.lett.crawlernode.kernel.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.Logging;

public class QueueHandler {

	protected static final Logger logger = LoggerFactory.getLogger(QueueHandler.class);

	private final String AWS_ACCESS_KEY = "AKIAITCJTG6OHBYGUWLA";
	private final String SECRET_KEY = "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";
	
	/**
	 * Amazon sqs queue to be used only in production mode
	 */
	private AmazonSQS sqsProduction;
	
	/**
	 * Amazon sqs queue to be used only in development mode
	 */
	private AmazonSQS sqsDevelopment;

	public QueueHandler() {
		AWSCredentials credentials = null;
		try {
			credentials = new BasicAWSCredentials(AWS_ACCESS_KEY, SECRET_KEY);
		} catch (Exception e) {
			throw new AmazonClientException("Cannot create credentials", e);
		}
		
		/* ************************
		 * Production environment *
		 **************************/
		if (Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION)) {
			Logging.printLogDebug(logger, "Authenticating in production environment SQS...");
			
			sqsProduction = new AmazonSQSClient(credentials);
			Region usEast1 = Region.getRegion(Regions.US_EAST_1);
			sqsProduction.setRegion(usEast1);
		}
		
		/* *************************
		 * Development environment *
		 ***************************/
		else {
			Logging.printLogDebug(logger, "Authenticating in development environment SQS...");
			
			sqsDevelopment = new AmazonSQSClient(credentials);
			Region usEast1 = Region.getRegion(Regions.US_EAST_1);
			sqsDevelopment.setRegion(usEast1);
		}

	}

	public AmazonSQS getSQS() {
		if (Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION)) {
			return sqsProduction;
		}
		return sqsDevelopment;
	}

}
