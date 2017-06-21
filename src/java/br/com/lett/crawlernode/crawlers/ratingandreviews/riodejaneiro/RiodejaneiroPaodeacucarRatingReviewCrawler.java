package br.com.lett.crawlernode.crawlers.ratingandreviews.riodejaneiro;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;

public class RiodejaneiroPaodeacucarRatingReviewCrawler extends RatingReviewCrawler {

	public RiodejaneiroPaodeacucarRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	public void handleCookiesBeforeFetch() {

		// Criando cookie da loja 7 = Rio de Janeiro capital
		BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", "7");
		cookie.setDomain(".paodeacucar.com.br");
		cookie.setPath("/");
		this.cookies.add(cookie);

	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(session.getOriginalURL())) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			RatingsReviews ratingsReviews = new RatingsReviews();

			ratingsReviews.setDate(session.getDate());
			ratingsReviews.setInternalId( crawlInternalId(document) );

			Integer totalOfEvaluations = 0;
			Integer totalRating = 0;
			Elements evaluationLines = document.select("ul.product-rating.reset--list.product-rating--stacked li");
			for (Element evaluationLine : evaluationLines) {
				Integer star = getStarFromEvaluationLine(evaluationLine);

				if (star != null) {
					Integer starEvaluations = getStarQuantityFromEvaluationLine(evaluationLine);
					totalOfEvaluations += starEvaluations;
					totalRating += (star * starEvaluations);
				}
			}

			if (totalOfEvaluations > 0) {
				Double averageOverallRating = new Double(totalRating) / new Double(totalOfEvaluations);

				ratingsReviews.setAverageOverallRating(averageOverallRating);
				ratingsReviews.setTotalRating(totalOfEvaluations);
			}

			ratingReviewsCollection.addRatingReviews(ratingsReviews);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return ratingReviewsCollection;
	}

	private Integer getStarFromEvaluationLine(Element evaluationLine) {
		Integer star = null;
		Element starElement = evaluationLine.select("span.product-rating__label").first();
		if (starElement != null) {
			String text = starElement.text().trim();
			if (!text.isEmpty()) {
				star = Integer.parseInt(text);
			}
		}
		return star;
	}

	private Integer getStarQuantityFromEvaluationLine(Element evaluationLine) {
		Integer starQuantity = 0;
		Element starQuantityElement = evaluationLine.select("span.product-rating__info.inline--middle").first();
		if (starQuantityElement != null) {
			String text = starQuantityElement.text().trim();
			if (!text.isEmpty()) {
				starQuantity = Integer.parseInt(text);
			}
		}
		return starQuantity;
	}

	private String crawlInternalId(Document document) {
		String internalId = null;
		Element elementInternalId = document.select("input[name=productId]").first();
		if (elementInternalId != null) {
			internalId = elementInternalId.attr("value").trim();
		}
		return internalId;
	}

	private boolean isProductPage(String url) {
		return url.contains("/produto/");
	}

}
