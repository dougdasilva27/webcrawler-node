package br.com.lett.crawlernode.core.models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;

public class RankingDiscoverStats extends Ranking {

	private List<RankingProductsDiscover> productsDiscover = new ArrayList<>();
	
	@Override
	public Document getDocument(){		
		List<Document> productsResult = new ArrayList<>();
		for(RankingProductsDiscover r : this.productsDiscover){
			productsResult.add(r.getDocument());
		}
		
		Date date = new Date( );
		SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd");
		
		return new Document()
			.append("market", 		this.marketId)
			.append("date",			ft.format(date))
			.append("lmt", 			this.lmt)
			.append("rank_type", 	this.rankType)
			.append("location", 	this.location)
			.append("products", 	productsResult)
			.append("statistics", 	this.statistics.getDocument());
		
	}
	
	@Override
	public Document getDocumentUpdate(){		
		List<Document> productsResult = new ArrayList<>();
		
		for(RankingProductsDiscover r : this.productsDiscover){
			productsResult.add(r.getDocument());
		}
		
		Document doc = new Document();
		doc.put("lmt", 			this.lmt);
		doc.put("statistics", 	this.statistics.getDocument());
		doc.put("products", 	productsResult);
	
		return doc;
	}
	
	public List<RankingProductsDiscover> getProductsDiscover() {
		return productsDiscover;
	}

	public void setProductsDiscover(List<RankingProductsDiscover> products) {
		this.productsDiscover = products;
	}
}
