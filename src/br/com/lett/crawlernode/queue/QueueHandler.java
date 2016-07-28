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

public class QueueHandler {
	
	protected static final Logger logger = LoggerFactory.getLogger(QueueHandler.class);

	private final String AWS_ACCESS_KEY = "AKIAITCJTG6OHBYGUWLA";
	private final String SECRET_KEY = "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";
	
	private AmazonSQS sqs;

	public QueueHandler() {
		AWSCredentials credentials = null;
		try {
			credentials = new BasicAWSCredentials(AWS_ACCESS_KEY, SECRET_KEY);
		} catch (Exception e) {
			throw new AmazonClientException("Cannot create credentials", e);
		}

		sqs = new AmazonSQSClient(credentials);
		Region usEast1 = Region.getRegion(Regions.US_EAST_1);
		sqs.setRegion(usEast1);
	}
	
	public AmazonSQS getSQS() {
		return sqs;
	}	

}
