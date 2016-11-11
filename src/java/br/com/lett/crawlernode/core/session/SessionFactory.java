package br.com.lett.crawlernode.core.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.model.Message;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.server.QueueName;
import br.com.lett.crawlernode.util.Logging;

public class SessionFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(SessionFactory.class);
	
	public static Session createSession(Message message, String queueName, Markets markets) {
		if (queueName.equals(QueueName.INSIGHTS) || queueName.equals(QueueName.INSIGHTS_DEVELOPMENT)) {
			return new InsightsCrawlerSession(message, queueName, markets);
		}
		else if (queueName.equals(QueueName.SEED) || queueName.equals(QueueName.SEED_DEAD)) {
			return new SeedCrawlerSession(message, queueName, markets);
		}
		else if (queueName.equals(QueueName.DISCOVER) || queueName.equals(QueueName.DISCOVER_DEVELOPMENT)) {
			return new DiscoveryCrawlerSession(message, queueName, markets);
		}
		else if (queueName.equals(QueueName.DEVELOPMENT)) {
			return new InsightsCrawlerSession(message, queueName, markets);
			//return new ImageCrawlerSession(message, queueName, markets);
		}
		else if (queueName.equals(QueueName.RATING_REVIEWS) || queueName.equals(QueueName.RATING_REVIEWS_DEVELOPMENT)) {
			return new RatingReviewsCrawlerSession();
		}
		else if (queueName.equals(QueueName.IMAGES) || queueName.equals(QueueName.IMAGES_DEAD)) {
			return new ImageCrawlerSession(message, queueName, markets);
		}
		else {
			Logging.printLogDebug(logger, "Queue name not recognized.");
			return null;
		}
	}
	
	public static Session createSession(String url, Market market) {
		return new TestCrawlerSession(url, market);
	}

}
