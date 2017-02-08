package br.com.lett.crawlernode.core.session;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.Request;

public class DiscoverKeywordsSession extends Session {
	
	private String keyword;
	
	public DiscoverKeywordsSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);
		
		this.keyword = ((CrawlerRankingKeywordsRequest)request).getKeyword();
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

}
