package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingCategoriesRequest;
import br.com.lett.crawlernode.core.server.request.Request;

public class RankingCategoriesSession extends RankingSession {

	private String categoryUrl;
	
	public RankingCategoriesSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);
		this.categoryUrl = ((CrawlerRankingCategoriesRequest)request).getCategoryUrl();
	}
	
	public String getCategoryUrl() {
		return categoryUrl;
	}

	public void setCategoryUrl(String categoryUrl) {
		this.categoryUrl = categoryUrl;
	}

}
