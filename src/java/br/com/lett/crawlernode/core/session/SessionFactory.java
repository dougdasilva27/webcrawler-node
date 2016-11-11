package br.com.lett.crawlernode.core.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.model.Message;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.server.Queue;
import br.com.lett.crawlernode.util.Logging;

public class SessionFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(SessionFactory.class);
	
	public static Session createSession(Message message, String queueName, Markets markets) {
		if (queueName.equals(Queue.INSIGHTS) || queueName.equals(Queue.INSIGHTS_DEAD)) {
			return new InsightsCrawlerSession(message, queueName, markets);
		}
		else if (queueName.equals(Queue.SEED) || queueName.equals(Queue.SEED_DEAD)) {
			return new SeedCrawlerSession(message, queueName, markets);
		}
		else if (queueName.equals(Queue.DISCOVER) || queueName.equals(Queue.DISCOVER_DEAD)) {
			return new DiscoveryCrawlerSession(message, queueName, markets);
		}
		else if (queueName.equals(Queue.DEVELOPMENT)) {
			return new InsightsCrawlerSession(message, queueName, markets);
			//return new ImageCrawlerSession(message, queueName, markets);
		}
		else if (queueName.equals(Queue.IMAGES) || queueName.equals(Queue.IMAGES_DEAD)) {
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
