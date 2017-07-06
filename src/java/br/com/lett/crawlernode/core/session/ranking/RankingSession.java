package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingCategoriesRequest;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;

public class RankingSession extends Session {

	private String location;

	public RankingSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);

		if(request instanceof CrawlerRankingKeywordsRequest) {
			this.location = ((CrawlerRankingKeywordsRequest) request).getLocation();
		} else if(request instanceof CrawlerRankingCategoriesRequest) {
			this.location = ((CrawlerRankingCategoriesRequest) request).getLocation();
		}
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

}
