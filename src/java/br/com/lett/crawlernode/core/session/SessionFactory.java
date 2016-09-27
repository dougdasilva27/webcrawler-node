package br.com.lett.crawlernode.core.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.model.Message;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.server.QueueHandler;
import br.com.lett.crawlernode.util.Logging;

public class SessionFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(SessionFactory.class);
	
	public static CrawlerSession createSession(Message message, String queueName, Markets markets) {
		if (queueName.equals(QueueHandler.INSIGHTS) || queueName.equals(QueueHandler.INSIGHTS_DEAD)) {
			return new InsightsCrawlerSession(message, queueName, markets);
		}
		else if (queueName.equals(QueueHandler.SEED) || queueName.equals(QueueHandler.SEED_DEAD)) {
			return new SeedCrawlerSession(message, queueName, markets);
		}
		else if (queueName.equals(QueueHandler.DISCOVER) || queueName.equals(QueueHandler.DISCOVER_DEAD)) {
			return new DiscoveryCrawlerSession(message, queueName, markets);
		}
		else if (queueName.equals(QueueHandler.DEVELOPMENT)) {
			//return new InsightsCrawlerSession(message, queueName, markets);
			return new ImageCrawlerSession(message, queueName, markets); // TODO remover
		}
		else if (queueName.equals(QueueHandler.IMAGES) || queueName.equals(QueueHandler.IMAGES_DEAD)) {
			return new ImageCrawlerSession(message, queueName, markets);
		}
		else {
			Logging.printLogDebug(logger, "Queue name not recognized.");
			return null;
		}
	}
	
	public static CrawlerSession createSession(String url, Market market) {
		return new TestCrawlerSession(url, market);
	}

}
