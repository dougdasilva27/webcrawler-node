package br.com.lett.crawlernode.core.task.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.ranking.RankingCategoriesSession;
import br.com.lett.crawlernode.core.session.ranking.RankingDiscoverCategoriesSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingCategoriesSession;

public abstract class CrawlerRankingCategories extends CrawlerRanking {

	private static Logger logger = LoggerFactory.getLogger(CrawlerRankingCategories.class);

	private static final String RANK_TYPE = "categories";

	public static final String SCHEDULER_NAME_DISCOVER_CATEGORIES = "discover_categories";


	public CrawlerRankingCategories(Session session) {
		super(session, RANK_TYPE, SCHEDULER_NAME_DISCOVER_CATEGORIES, logger);

		if(session instanceof RankingCategoriesSession) {
			this.location = ((RankingCategoriesSession)session).getLocation();
		} else if(session instanceof TestRankingCategoriesSession) {
			this.location = ((TestRankingCategoriesSession)session).getLocation();
		} else if(session instanceof RankingDiscoverCategoriesSession) {
			this.location = ((RankingDiscoverCategoriesSession)session).getLocation();
		}
	}

}
