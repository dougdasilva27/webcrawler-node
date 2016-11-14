package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.RatingReviewCrawler;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class BrasilCarrefourRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilCarrefourRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
		
		if (isProductPage(session.getOriginalURL())) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			RatingsReviews ratingReviews = crawlRatingReviews(document);
			ratingReviewsCollection.addRatingReviews(ratingReviews);
			
		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return ratingReviewsCollection;
	}
	
	private boolean isProductPage(String url) {
		if ((url.contains("/p/"))) return true;
		return false;
	}

	private RatingsReviews crawlRatingReviews(Document document) {
		RatingsReviews ratingReviews = new RatingsReviews();

		Elements ratingLineElements = document.select("div.tab-review ul.block-list-starbar li");
		for (Element ratingLine : ratingLineElements) {
			Element ratingStarElement = ratingLine.select("div").first();
			Element ratingStarCount = ratingLine.select("div").last();

			if (ratingStarElement != null && ratingStarCount != null) {
				String ratingStarText = ratingStarElement.text();
				String ratingCountText = ratingStarCount.attr("data-star");

				List<String> parsedNumbers = MathCommonsMethods.parseNumbers(ratingStarText);
				if (parsedNumbers.size() > 0 && !ratingCountText.isEmpty()) {
					ratingReviews.addRating(parsedNumbers.get(0), Integer.parseInt(ratingCountText));
				}
			}
		}
		
		return ratingReviews;
	}



}
