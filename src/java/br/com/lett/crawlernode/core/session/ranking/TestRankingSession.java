package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;

public class TestRankingSession extends Session {
	
	private String location;
	
	public TestRankingSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);
		
		this.location = ((CrawlerRankingRequest)request).getLocation();
	}
	
	public TestRankingSession(Market market, String location) {
		super(market);
		
		// setting Market
		this.market = market;

		// setting location
		this.location = location;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String keyword) {
		this.location = keyword;
	}

}
