package br.com.lett.crawlernode.core.models;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;

public class Ranking {
	
	private int marketId;
	private List<RankingProducts> products = new ArrayList<>();
	private ArrayList<Document> productsResult = new ArrayList<>();
	private String location;
	private String rankType;
	private Timestamp date;
	private String lmt;
	private RankingStatistics statistics = new RankingStatistics();

	public Document getDocument(){		
		
		for(RankingProducts r : this.products){
			this.productsResult.add(r.getDocument());
		}
		
		Date date = new Date( );
		SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd");
		
		return new Document()
			.append("market", 		this.marketId)
			.append("date",			ft.format(date))
			.append("lmt", 			this.lmt)
			.append("rank_type", 	this.rankType)
			.append("location", 	this.location)
			.append("products", 	this.productsResult)
			.append("statistics", 	this.statistics.getDocument());
		
	}
	
	public Document getDocumentUpdate(){		
		
		for(RankingProducts r : this.products){
			this.productsResult.add(r.getDocument());
		}
		
		Document doc = new Document();
		doc.put("lmt", 			this.lmt);
		doc.put("statistics", 	this.statistics.getDocument());
		doc.put("products", 	this.productsResult);
	
		return doc;
	}

	public int getMarketId() {
		return marketId;
	}


	public void setMarketId(int marketId) {
		this.marketId = marketId;
	}


	public String getLocation() {
		return location;
	}


	public void setLocation(String location) {
		this.location = location;
	}


	public String getRankType() {
		return rankType;
	}


	public void setRankType(String rankType) {
		this.rankType = rankType;
	}

	public List<RankingProducts> getProducts() {
		return products;
	}

	public void setProducts(List<RankingProducts> products) {
		this.products = products;
	}

	public Timestamp getDate() {
		return date;
	}

	public void setDate(Timestamp date) {
		this.date = date;
	}

	public String getLmt() {
		return lmt;
	}

	public void setLmt(String lmt) {
		this.lmt = lmt;
	}

	public RankingStatistics getStatistics() {
		return statistics;
	}

	public void setStatistics(RankingStatistics statistics) {
		this.statistics = statistics;
	}

	public ArrayList<Document> getProductsResult() {
		return productsResult;
	}

	public void setProductsResult(ArrayList<Document> productsResult) {
		this.productsResult = productsResult;
	}

}
