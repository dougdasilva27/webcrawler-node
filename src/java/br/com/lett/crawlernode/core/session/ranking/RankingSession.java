package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingCategoriesRequest;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;

public class RankingSession extends Session {

	private String location;
	private Boolean takeAScreenshot;

	public RankingSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);

		if(request instanceof CrawlerRankingKeywordsRequest) {
			this.location = ((CrawlerRankingKeywordsRequest) request).getLocation();
			this.takeAScreenshot = ((CrawlerRankingKeywordsRequest) request).getTakeAScreenshot();
		} else if(request instanceof CrawlerRankingCategoriesRequest) {
			this.location = ((CrawlerRankingCategoriesRequest) request).getLocation();
			this.takeAScreenshot = ((CrawlerRankingCategoriesRequest) request).getTakeAScreenshot();
		}
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Boolean mustTakeAScreenshot() {
		return takeAScreenshot;
	}

	public void setTakeAScreenshot(Boolean takeAScreenshot) {
		this.takeAScreenshot = takeAScreenshot;
	}

}
