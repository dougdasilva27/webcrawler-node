package br.com.lett.crawlernode.core.models;

import org.bson.Document;

public class RankingStatistics {

	private int pageSize;
	private int totalSearch;
	private int totalFetched;
	
	public Document getDocument(){
		Document document = new Document();
		
		document.append("page_size", this.pageSize)
				.append("total_fetched", this.totalFetched);
		
		if(totalSearch > 0){
			document.append("total_search", this.totalSearch);
		}
		
		return document;
	}
	
	
	public int getPageSize() {
		return pageSize;
	}
	
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	
	public int getTotalSearch() {
		return totalSearch;
	}
	
	public void setTotalSearch(int totalSearch) {
		this.totalSearch = totalSearch;
	}
	
	public int getTotalFetched() {
		return totalFetched;
	}
	
	public void setTotalFetched(int totalFetched) {
		this.totalFetched = totalFetched;
	}
}
