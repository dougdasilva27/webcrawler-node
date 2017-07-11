package br.com.lett.crawlernode.core.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.crawler.DiscoveryCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.ImageCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.InsightsCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.RatingReviewsCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.SeedCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.core.session.ranking.RankingCategoriesSession;
import br.com.lett.crawlernode.core.session.ranking.RankingDiscoverCategoriesSession;
import br.com.lett.crawlernode.core.session.ranking.RankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.RankingKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingCategoriesSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingKeywordsSession;
import br.com.lett.crawlernode.queue.QueueName;
import br.com.lett.crawlernode.util.Logging;

public class SessionFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(SessionFactory.class);
	
	public static Session createSession(Request request, Markets markets) {
		String queueName = request.getQueueName();
		
		if ( queueName.equals(QueueName.INSIGHTS) 				|| 
			 queueName.equals(QueueName.INSIGHTS_DEVELOPMENT) 	|| 
			 queueName.equals(QueueName.INTEREST_PROCESSED) 	||
			 queueName.equals(QueueName.TEST_PHANTOMJS) 		||
			 queueName.equals(QueueName.INSIGHTS_TRY_AGAIN) ) {
			
			return new InsightsCrawlerSession(request, queueName, markets);
		}
		else if (queueName.equals(QueueName.SEED)) {
			return new SeedCrawlerSession(request, queueName, markets);
		}
		else if (queueName.equals(QueueName.DISCOVER)) {
			return new DiscoveryCrawlerSession(request, queueName, markets);
		}
		else if (queueName.equals(QueueName.RATING)) {
			return new RatingReviewsCrawlerSession(request, queueName, markets);
		}
		else if (queueName.equals(QueueName.IMAGES)) {
			return new ImageCrawlerSession(request, queueName, markets);
		}
		else if (queueName.equals(QueueName.RANKING_KEYWORDS)) {
			return new RankingKeywordsSession(request, queueName, markets);
		}
		else if (queueName.equals(QueueName.DISCOVER_KEYWORDS)) {
			return new RankingDiscoverKeywordsSession(request, queueName, markets);
		}
		else if (queueName.equals(QueueName.RANKING_CATEGORIES)) {
			return new RankingCategoriesSession(request, queueName, markets);
		}
		else if (queueName.equals(QueueName.DISCOVER_CATEGORIES)) {
			return new RankingDiscoverCategoriesSession(request, queueName, markets);
		}
		else {
			Logging.printLogDebug(logger, "Queue name not recognized.");
			return null;
		}
	}
	
	public static Session createTestSession(String url, Market market) {
		return new TestCrawlerSession(url, market);
	}
	
	public static Session createTestRankingKeywordsSession(String keyword, Market market) {
		return new TestRankingKeywordsSession(market, keyword);
	}
	
	public static Session createTestRankingCategoriesSession(String categorieUrl, Market market, String location) {
		return new TestRankingCategoriesSession(market, categorieUrl, location);
	}
}
