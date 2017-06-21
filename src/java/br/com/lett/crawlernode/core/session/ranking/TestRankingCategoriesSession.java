package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;

public class TestRankingCategoriesSession extends TestRankingSession {

	public TestRankingCategoriesSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);

	}

	public TestRankingCategoriesSession(Market market, String categoryUrl, String location) {
		super(market, location);
		
		this.categoryUrl = categoryUrl;
	}

	private String categoryUrl;
	
	public String getCategoryUrl() {
		return categoryUrl;
	}

	public void setCategoryUrl(String categoryUrl) {
		this.categoryUrl = categoryUrl;
	}
}
