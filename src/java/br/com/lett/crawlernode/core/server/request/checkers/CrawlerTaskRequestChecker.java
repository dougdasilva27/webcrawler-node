package br.com.lett.crawlernode.core.server.request.checkers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.server.ServerHandler;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.ImageCrawlerRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.queue.QueueName;
import br.com.lett.crawlernode.util.Logging;

public class CrawlerTaskRequestChecker {

	protected static final Logger logger = LoggerFactory.getLogger(CrawlerTaskRequestChecker.class);

	public static boolean checkRequest(Request request) {
		if (request.getQueueName() == null) {
			Logging.printLogError(logger, "Request is missing queue name");
			return false;
		}
		
		if (Main.markets.getMarket(request.getMarketId()) == null) {
			Logging.printLogError(logger, "Market " + request.getMarketId() + " doesn't exist.");
			return false;
		}
		
		if (request instanceof ImageCrawlerRequest) {
			return checkImageTaskRequest(request);
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
		
		if(QueueName.RANKING_KEYWORDS.equals(request.getQueueName()) && ((CrawlerRankingKeywordsRequest)request).getKeyword() == null) {
			Logging.printLogError(logger, "Request is missing keyword");
			return false;
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
		ImageCrawlerRequest imageCrawlerRequest = (ImageCrawlerRequest) request;
				
		if (imageCrawlerRequest.getImageType() == null) {
			Logging.printLogError(logger, "Request is missing image type");
			return false;
		}
		if (imageCrawlerRequest.getProcessedId() == null) {
			Logging.printLogError(logger, "Request is missing processedId");
			return false;
		}
		if (imageCrawlerRequest.getInternalId() == null) {
			Logging.printLogError(logger, "Request is missing internalId");
			return false;
		}
		if (imageCrawlerRequest.getImageNumber() == null) {
			Logging.printLogError(logger, "Request is missing image number");
			return false;
		}

		return true;
	}

}
