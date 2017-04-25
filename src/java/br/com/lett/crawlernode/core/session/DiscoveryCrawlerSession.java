package br.com.lett.crawlernode.core.session;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;

public class DiscoveryCrawlerSession extends Session {
	
	public DiscoveryCrawlerSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);
	}

}
