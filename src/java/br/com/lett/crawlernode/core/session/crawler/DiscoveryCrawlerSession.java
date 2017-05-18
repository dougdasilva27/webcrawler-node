package br.com.lett.crawlernode.core.session.crawler;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;

public class DiscoveryCrawlerSession extends Session {
	
	public DiscoveryCrawlerSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);
	}

}
