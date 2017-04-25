package br.com.lett.crawlernode.core.server.request;

public class CrawlerRankingKeywordsRequest extends Request {
	
	private String keyword;
	
	public CrawlerRankingKeywordsRequest() {
		super();
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	

}
