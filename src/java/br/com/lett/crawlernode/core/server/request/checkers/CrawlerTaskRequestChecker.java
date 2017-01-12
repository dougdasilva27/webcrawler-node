package br.com.lett.crawlernode.core.server.request.checkers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.server.ServerHandler;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.queue.QueueName;
import br.com.lett.crawlernode.util.Logging;

public class CrawlerTaskRequestChecker {

	protected static final Logger logger = LoggerFactory.getLogger(CrawlerTaskRequestChecker.class);

	public static boolean checkRequest(Request request) {
		if (request.getQueueName() == null) {
			return false;
		}
		if (QueueName.IMAGES.equals(request.getQueueName())) {
			return checkImageTaskRequest(request);
		}
		if (request.getMarketName() == null) {
			Logging.printLogError(logger, "Request is missing field marketName");
			return false;
		}
		if (request.getCityName() == null) {
			Logging.printLogError(logger, "Request is missing field city");
			return false;
		}
		if (QueueName.INSIGHTS.equals(request.getQueueName())) {
			if (request.getProcessedId() == null) {
				Logging.printLogError(logger, "Request is missing processedId");
				return false;
			}
			if (request.getInternalId() == null) {
				Logging.printLogError(logger, "Request is missing internalId");
				return false;
			}
		}

		return true;
	}
	
	public static boolean checkRequestMethod(Request request) {		
		if (ServerHandler.POST.equals(request.getRequestMethod())) {
			return true;
		} 
		return false;
	}

	private static boolean checkImageTaskRequest(Request request) {
		if (request.getMarketName() == null) {
			Logging.printLogError(logger, "Request is missing marketName");
			return false;
		}
		if (request.getType() == null) {
			Logging.printLogError(logger, "Request is missing image type");
			return false;
		}
		if (request.getCityName() == null) {
			Logging.printLogError(logger, "Request is missing city");
			return false;
		}
		if (request.getProcessedId() == null) {
			Logging.printLogError(logger, "Request is missing processedId");
			return false;
		}
		if (request.getInternalId() == null) {
			Logging.printLogError(logger, "Request is missing internalId");
			return false;
		}
		if (request.getNumber() == null) {
			Logging.printLogError(logger, "Request is missing image number");
			return false;
		}

		return true;
	}

}
