package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class BrasilDolcegustoRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilDolcegustoRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
		
		if (isProductPage(doc, session.getOriginalURL())) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			RatingsReviews ratingReviews = crawlRatingReviews(doc);
			ratingReviews.setInternalId(crawlInternalId(doc));
			
			ratingReviewsCollection.addRatingReviews(ratingReviews);
			
		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return ratingReviewsCollection;
	}
	
	private boolean isProductPage(Document doc, String url) {
		String[] tokens = url.split("/");
		return !doc.select(".product-essential").isEmpty() && tokens.length == 4 && !url.endsWith("/");
	}
	
	private String crawlInternalId(Document doc) {
		String internalId = null;
		Element elementInternalId = doc.select(".no-display [name=product]").first();
		
		if (elementInternalId != null) {
			internalId = elementInternalId.attr("value");
		}

		return internalId;
	}

	private RatingsReviews crawlRatingReviews(Document doc) {
		RatingsReviews ratingReviews = new RatingsReviews();
		
		ratingReviews.setDate(session.getDate());
		ratingReviews.setTotalReviews(computeTotalReviewsCount(doc));
		ratingReviews.setAverageOverallRating(crawlAverageOverallRating(doc));
		
		return ratingReviews;
	}
	
	private Integer computeTotalReviewsCount(Document doc) {
		Integer totalReviewsCount = 0;
		Element total = doc.select("meta[itemprop=reviewCount]").first();
		
		if(total != null) {
			try {
				totalReviewsCount = Integer.parseInt(total.attr("content"));
			} catch(Exception e) {
				Logging.printLogError(logger, CommonMethods.getStackTrace(e));
			}
		}
		
		return totalReviewsCount;
	}
	
	private Double crawlAverageOverallRating(Document document) {
		Double avgOverallRating = null;
		
		Element percentageElement = document.select("meta[itemprop=ratingValue]").first();
		if (percentageElement != null) {
			try {
				Double percentage = Double.parseDouble(percentageElement.attr("content"));
				
				if(percentage > 0f) {
					avgOverallRating = MathCommonsMethods.normalizeTwoDecimalPlaces(5 * (percentage/100f));
				}
				
			} catch(Exception e) {
				Logging.printLogError(logger, CommonMethods.getStackTrace(e));
			}
		}
		
		return avgOverallRating;
	}
}
