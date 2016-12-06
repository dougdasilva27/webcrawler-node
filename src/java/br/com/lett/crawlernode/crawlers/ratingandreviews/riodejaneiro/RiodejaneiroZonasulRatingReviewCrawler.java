package br.com.lett.crawlernode.crawlers.ratingandreviews.riodejaneiro;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.RatingReviewCrawler;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class RiodejaneiroZonasulRatingReviewCrawler extends RatingReviewCrawler {

	public RiodejaneiroZonasulRatingReviewCrawler(Session session) {
		super(session);
	}
	
	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(session.getOriginalURL())) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			RatingsReviews ratingsReviews = new RatingsReviews();

			ratingsReviews.setDate(session.getDate());
			ratingsReviews.setInternalId( crawlInternalId(document) );

			Elements evaluationLines = document.select("div.box_avaliacao tr");
			Double totalRating = 0.0;
			Integer totalNumberOfEvaluations = 0;
			for (int i = 1; i < evaluationLines.size(); i++) { // skip the first element because it is the table header
				Element evaluationLine = evaluationLines.get(i);
				Element starElement = evaluationLine.select("td").first();
				Element starQuantityElement = evaluationLine.select("td").last();
				
				if (starElement != null && starQuantityElement != null) {
					Integer star = Integer.parseInt(starElement.text().trim());
					
					String starQuantityText = starQuantityElement.text();
					starQuantityText = starQuantityText.replace('(', ' ').replace(')', ' ').trim();
					Integer starQuantity = Integer.parseInt(starQuantityText);
					
					totalNumberOfEvaluations += starQuantity;
					totalRating += star * starQuantity;
				}
				
				if (totalNumberOfEvaluations > 0) {
					Double avgRating = MathCommonsMethods.normalizeTwoDecimalPlaces(totalRating / totalNumberOfEvaluations);
					
					ratingsReviews.setTotalReviews(totalNumberOfEvaluations);
					ratingsReviews.setAverageOverallRating(avgRating);
				}
			}

			ratingReviewsCollection.addRatingReviews(ratingsReviews);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return ratingReviewsCollection;
	}
	
	private boolean isProductPage(String url) {
		return url.startsWith("http://www.zonasulatende.com.br/Produto/");
	}
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element elementId = document.select(".codigo").first();
		if (elementId != null) {
			if(!elementId.text().trim().isEmpty()){
				internalId = Integer.toString(Integer.parseInt(elementId.text().replaceAll("[^0-9,]+", ""))).trim();
			}
		}
		return internalId;
	}

}
