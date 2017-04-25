package br.com.lett.crawlernode.core.session;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.Request;

public class TestRankingKeywordsSession extends Session {
	
	private String keyword;
	
	public TestRankingKeywordsSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);
		
		this.keyword = ((CrawlerRankingKeywordsRequest)request).getKeyword();
	}
	
	public TestRankingKeywordsSession(Market market, String keyword) {
		super(market);
		
		// setting Market
		this.market = market;

		// setting URL and originalURL
		this.keyword = keyword;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

}
