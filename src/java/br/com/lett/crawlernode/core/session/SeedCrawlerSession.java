package br.com.lett.crawlernode.core.session;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;

public class SeedCrawlerSession extends Session {
	
	public SeedCrawlerSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);
	}

}
