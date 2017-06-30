package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;

/**
 * Date: 14/12/16
 * @author gabriel
 *
 */
public class BrasilRicardoeletroRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilRicardoeletroRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(session.getOriginalURL())) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			ratingReviewsCollection.addRatingReviews(crawlRatingNew(doc));
		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}

		return ratingReviewsCollection;

	} 

	/**
	 * 
	 * @param doc
	 * @return
	 */
	public RatingsReviews crawlRatingNew(Document doc) {
		// InternalId
		String internalId = crawlInternalId(doc);
		
		RatingsReviews ratingReviews = crawlRatingReviews(doc);
		ratingReviews.setInternalId(internalId);
		
		return ratingReviews;
	}
	
	/**
	 * Crawl Internal ID 
	 * @param doc
	 * @return
	 */
	private String crawlInternalId(Document doc) {
		String internalId = null;

		Element elementInternalID = doc.select("#ProdutoDetalhesCodigoProduto").first();
		if (elementInternalID != null) {
			internalId = elementInternalID.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").trim();
		}

		return internalId;
	}

	/**
	 * Crawl rating and reviews stats using the bazaar voice endpoint.
	 * To get only the stats summary we need at first, we only have to do
	 * one request. If we want to get detailed information about each review, we must
	 * perform pagination.
	 * 
	 * The RatingReviews crawled in this method, is the same across all skus variations
	 * in a page.
	 *
	 * @param document
	 * @return
	 */
	private RatingsReviews crawlRatingReviews(Document doc) {
		RatingsReviews ratingReviews = new RatingsReviews();

		ratingReviews.setDate(session.getDate());

		ratingReviews.setTotalRating(getTotalReviewCount(doc));
		ratingReviews.setAverageOverallRating(getAverageOverallRating(doc));

		return ratingReviews;
	}

	private Integer getTotalReviewCount(Document doc) {
		Integer totalReviewCount = null;
		Element total = doc.select(".avaliacoes span[itemprop=ratingCount]").last();
		
		if (total != null) {
			totalReviewCount = Integer.parseInt(total.ownText().replaceAll("[^0-9]", ""));
		}
		return totalReviewCount;
	}

	private Double getAverageOverallRating(Document doc) {
		Double avgOverallRating = null;
		Element avg = doc.select(".avaliacoes span[itemprop=ratingValue]").first();
		
		if (avg != null) {
			avgOverallRating = Double.parseDouble(avg.ownText());
		}
		return avgOverallRating;
	}

	private boolean isProductPage(String url) {
		return url.startsWith("http://www.ricardoeletro.com.br/Produto/");
	}
	
}