package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;

public class RankingSession extends Session {

	private String location;

	public RankingSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);

		this.location = ((CrawlerRankingRequest) request).getLocation();
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

}
