package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;

public class TestRankingCategoriesSession extends TestRankingSession {

	public TestRankingCategoriesSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);

	}

	public TestRankingCategoriesSession(Market market, String categorieUrl) {
		super(market, categorieUrl);
	}

}
