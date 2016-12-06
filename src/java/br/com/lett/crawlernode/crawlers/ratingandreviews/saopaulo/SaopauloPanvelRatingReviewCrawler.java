package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.crawler.RatingReviewCrawler;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;

public class SaopauloPanvelRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloPanvelRatingReviewCrawler(Session session) {
		super(session);
	}
	
	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
		
		if (isProductPage(session.getOriginalURL())) {
			RatingsReviews ratingReviews = new RatingsReviews();
			
			ratingReviews.setDate(session.getDate());
			
			
			
			ratingReviewsCollection.addRatingReviews(ratingReviews);
			
		}
		

		return ratingReviewsCollection;
	}
	
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element elementInternalId = document.select(".cod-produto").first();
		if (elementInternalId != null) {
			internalId = elementInternalId.text().split(":")[1].trim();
		}
		return internalId;
	}
	
	private boolean isProductPage(String url) {
		return 	url.startsWith("http://www.panvel.com/panvel/visualizarProduto") 
				|| url.startsWith("http://www.panvel.com/panvel/produto") 
				|| url.startsWith("https://www.panvel.com/panvel/visualizarProduto") 
				|| url.startsWith("https://www.panvel.com/panvel/produto");
	}

}
