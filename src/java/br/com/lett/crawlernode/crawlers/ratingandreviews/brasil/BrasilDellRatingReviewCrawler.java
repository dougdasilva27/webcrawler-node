package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;

/**
 * Date: 25/07/17
 * @author gabriel
 *
 */
public class BrasilDellRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilDellRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(session.getOriginalURL())) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			RatingsReviews ratingReviews = new RatingsReviews();			
			ratingReviews.setDate(session.getDate());

			String internalId = crawlInternalId(document);

			Document docRating = crawlDocRating(document);
			
			Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);			
			Double avgRating = getTotalAvgRating(docRating);

			ratingReviews.setInternalId(internalId);
			ratingReviews.setTotalRating(totalNumOfEvaluations);
			ratingReviews.setAverageOverallRating(avgRating);

			ratingReviewsCollection.addRatingReviews(ratingReviews);
		}

		return ratingReviewsCollection;

	}

	private String crawlInternalId(Document doc){
		String internalId = null;
		
		if(!isSpecialProduct(session.getOriginalURL())){
			Element internalIdElements = doc.select("meta[name=currentOcId]").first();
	
			if(internalIdElements != null) {
				internalId = internalIdElements.attr("content");
			}
		} else {
			Element internalIdElement = doc.select("td.para_small").first();
			
			if(internalIdElement !=  null) {
				String text = internalIdElement.ownText().trim();
				int x = text.indexOf('|')+1;
				
				String code = text.substring(x);
				int y = code.indexOf(':')+1;
				
				internalId = code.substring(y).trim();
			}
		}

		return internalId;
	}

	boolean isSpecialProduct(String url) {
		return url.contains("productdetail.aspx");
	}
	
	
	/**
	 * Rating is in another html page
	 * @param doc
	 * @return
	 */
	private Document crawlDocRating(Document doc) {
		Document docRating = new Document("");
		Element docUrl = doc.select("#BVUrl").first();

		if(docUrl != null) {
			String url = docUrl.ownText().trim();
			
			docRating = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
		} else {
			docUrl = doc.select("#BVdefaultURL a").first();
			
			if(docUrl != null) {
				String url = docUrl.attr("href").trim() + "?format=embedded";
				
				docRating = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
			} 
		}
		
		return docRating;
	}
	
	/**
	 * Avg appear in html element
	 * 
	 * @param document
	 * @return
	 */
	private Double getTotalAvgRating(Document doc) {
		Double avgRating = 0d;
		Element rating = doc.select("span[itemprop=ratingValue].BVRRRatingNumber").first();

		if (rating != null) {
			String text = rating.ownText().replaceAll("[^0-9,]", "").replace(",", ".").trim();
			
			if(!text.isEmpty()) {
				avgRating = Double.parseDouble(text);
			}
		}

		return avgRating;
	}

	/**
	 * Number of ratings appear in html element 
	 * @param doc
	 * @return
	 */
	private Integer getTotalNumOfRatings(Document doc) {
		Integer ratingTotal = 0;
		Element total = doc.select("meta[itemprop=reviewCount]").first();
		
		if(total != null) {
			String text = total.attr("content").trim();
			
			if(!text.isEmpty()) {
				ratingTotal = Integer.parseInt(text);
			}
		}
		
		return ratingTotal;
	}


	private boolean isProductPage(String url) {
		return url.contains("/p/") || url.contains("productdetail");
	}

}
