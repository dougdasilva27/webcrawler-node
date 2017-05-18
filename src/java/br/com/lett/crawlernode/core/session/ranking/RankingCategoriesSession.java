package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;

public class RankingCategoriesSession extends RankingSession {

	public RankingCategoriesSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);
	}

}
