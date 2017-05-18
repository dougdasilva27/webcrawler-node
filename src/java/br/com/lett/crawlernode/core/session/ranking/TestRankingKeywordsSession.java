package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;

public class TestRankingKeywordsSession extends TestRankingSession {

	public TestRankingKeywordsSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);

	}

	public TestRankingKeywordsSession(Market market, String keyword) {
		super(market, keyword);

	}

}
