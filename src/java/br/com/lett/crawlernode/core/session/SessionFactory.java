package br.com.lett.crawlernode.core.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.queue.QueueName;
import br.com.lett.crawlernode.util.Logging;

public class SessionFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(SessionFactory.class);
	
	public static Session createSession(Request request, String queueName, Markets markets) {
		if (queueName.equals(QueueName.AUTO_GENERATED) || queueName.equals(QueueName.INSIGHTS_DEVELOPMENT)) {
			return new InsightsCrawlerSession(request, queueName, markets);
		}
		else if (queueName.equals(QueueName.SEED) || queueName.equals(QueueName.SEED_DEAD)) {
			return new SeedCrawlerSession(request, queueName, markets);
		}
		else if (queueName.equals(QueueName.DISCOVER) || queueName.equals(QueueName.DISCOVER_DEVELOPMENT)) {
			return new DiscoveryCrawlerSession(request, queueName, markets);
		}
		else if (queueName.equals(QueueName.RATING_REVIEWS) || queueName.equals(QueueName.RATING_REVIEWS_DEVELOPMENT)) {
			return new RatingReviewsCrawlerSession(request, queueName, markets);
		}
		else if (queueName.equals(QueueName.IMAGES) || queueName.equals(QueueName.IMAGES_DEAD)) {
			return new ImageCrawlerSession(request, queueName, markets);
		}
		else if (queueName.equals(QueueName.DEVELOPMENT)) {
			return new InsightsCrawlerSession(request, queueName, markets);
			//return new ImageCrawlerSession(message, queueName, markets);
		}
		else {
			Logging.printLogDebug(logger, "Queue name not recognized.");
			return null;
		}
	}
	
	public static Session createTestSession(String url, Market market) {
		return new TestCrawlerSession(url, market);
	}

}
