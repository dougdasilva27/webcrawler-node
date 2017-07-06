package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloUltrafarmaRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloUltrafarmaRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
		
		if (isProductPage(doc, session.getOriginalURL())) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			RatingsReviews ratingReviews = crawlRatingReviews(doc);
			ratingReviews.setInternalId(crawlInternalId(session.getOriginalURL()));
			
			ratingReviewsCollection.addRatingReviews(ratingReviews);
			
		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return ratingReviewsCollection;
	}
	
	private boolean isProductPage(Document doc, String url) {
		return url.startsWith("http://www.ultrafarma.com.br/produto/detalhes");
	}
	
	private String crawlInternalId(String url) {
		String id = url.split("/")[4];
		
		return id.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").trim();
	}

	private RatingsReviews crawlRatingReviews(Document doc) {
		RatingsReviews ratingReviews = new RatingsReviews();
		
		ratingReviews.setDate(session.getDate());
		ratingReviews.setTotalRating(computeTotalReviewsCount(doc));
		ratingReviews.setAverageOverallRating(crawlAverageOverallRating(doc));
		
		return ratingReviews;
	}
	
	private Integer computeTotalReviewsCount(Document doc) {		
		return doc.select(".desc_inf_prod.cont_btn_avalie").size();
	}
	
	private Double crawlAverageOverallRating(Document document) {
		Double avgOverallRating = null;
		
		Element percentageElement = document.select("#avaliacao img").first();
		if (percentageElement != null) {
			try {
				String[] tokens = percentageElement.attr("src").split("/");

				avgOverallRating = Double.parseDouble(tokens[tokens.length-1].replaceAll("[^0-9]", ""));
			} catch(Exception e) {
				Logging.printLogError(logger, CommonMethods.getStackTrace(e));
			}
		}
		
		return avgOverallRating;
	}
}
