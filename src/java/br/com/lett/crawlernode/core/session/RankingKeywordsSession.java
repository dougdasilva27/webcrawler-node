package br.com.lett.crawlernode.core.session;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;

public class RankingKeywordsSession extends Session {
	
	private String keyword;
	
	public RankingKeywordsSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

}
