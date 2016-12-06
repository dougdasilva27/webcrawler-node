package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.RatingReviewCrawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class SaopauloNetfarmaRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloNetfarmaRatingReviewCrawler(Session session) {
		super(session);
	}
	
	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
		
		if (isProductPage(document)) {
			RatingsReviews ratingReviews = new RatingsReviews();
			
			ratingReviews.setDate(session.getDate());
			
			JSONObject chaordicMeta = crawlChaordicMeta(document);
			String internalId = crawlInternalId(chaordicMeta);
			String sku = crawlSkuId(chaordicMeta);
			
			JSONObject reviewPage = requestReviewPage(sku, 1);
			
			if (reviewPage.has("totalPaginas") && reviewPage.has("totalRegistros")) {
				Integer numRatings = reviewPage.getInt("totalRegistros");
				Double totalRating = 0.0;
				Integer totalPages = reviewPage.getInt("totalPaginas");
				
				for (int i = 2; i <= totalPages; i++) {
					if (reviewPage.has("avaliacoes")) {
						JSONArray ratings = reviewPage.getJSONArray("avaliacoes");
						
						for (int j = 0; j < ratings.length(); j++) {
							JSONObject rating = ratings.getJSONObject(j);
							if (rating.has("nota")) {
								totalRating = totalRating + new Double(rating.getInt("nota"));
							}
						}
					}
					
					reviewPage = requestReviewPage(sku, i);
				}
				
				Double avgRating;
				if (totalRating.equals(0.0)) {
					avgRating = 0.0;
				} else {
					avgRating = MathCommonsMethods.normalizeTwoDecimalPlaces(totalRating/numRatings);
				}
				
				ratingReviews.setInternalId(internalId);
				ratingReviews.setTotalReviews(numRatings);
				ratingReviews.setAverageOverallRating(avgRating);				
			}
			
			ratingReviewsCollection.addRatingReviews(ratingReviews);
			
		}
		

		return ratingReviewsCollection;
	}
	
	private boolean isProductPage(Document doc) {
		return doc.select(".product-details__code").first() != null;
	}
	
	private String crawlSkuId(JSONObject jsonProduct) {
		String internalId = null;
		if (jsonProduct.has("sku")){
			internalId = jsonProduct.getString("sku").trim();
		}
		return internalId;
	}
	
	private String crawlInternalId(JSONObject jsonProduct) {
		String internalId = null;
		if(jsonProduct.has("pid")) {
			internalId = jsonProduct.getString("pid").trim();
		}
		return internalId;
	}
	
	private JSONObject requestReviewPage(String skuId, Integer pageNumber) {
		String url = "https://www.netfarma.com.br/api/produto/" + skuId + "/avaliacoes/" + pageNumber;
		return DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, null);
	}
	
	/**
	 * Get the window.chaordic_meta script inside the html.
	 * 
	 * window.chaordic_meta = {
	 *	"page": "product",
	 *	"sku": "C02500LRE00",
	 *	"price": 29.90,
	 *	"pid": "36755"
	 *	};
	 * 
	 * @return
	 */
	private JSONObject crawlChaordicMeta(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		String scriptVariableName = "window.chaordic_meta = ";
		JSONObject chaordicMeta = null;
		
		for (Element tag : scriptTags){                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith(scriptVariableName)) {
					chaordicMeta = new JSONObject
							(
							node.getWholeData().split(Pattern.quote(scriptVariableName))[1] +
							node.getWholeData().split(Pattern.quote(scriptVariableName))[1].split(Pattern.quote("};"))[0]
							);
				}
			}        
		}
		
		if (chaordicMeta == null) {
			chaordicMeta = new JSONObject();
		}
		
		return chaordicMeta;
	}
	

}
