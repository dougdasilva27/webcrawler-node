package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.RatingReviewCrawler;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

/**
 * Date: 13/12/16
 * @author gabriel
 *
 */
public class BrasilBifarmaRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilBifarmaRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(document)) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			RatingsReviews ratingReviews = new RatingsReviews();			
			ratingReviews.setDate(session.getDate());

			String internalId = crawlInternalId(document);

			if(internalId != null) {
				Integer totalNumOfEvaluations = getTotalNumOfRatings(document);			
				Double avgRating = getTotalAvgRating(document, totalNumOfEvaluations);
				
				ratingReviews.setInternalId(internalId);
				ratingReviews.setTotalReviews(totalNumOfEvaluations);
				ratingReviews.setAverageOverallRating(avgRating);

				ratingReviewsCollection.addRatingReviews(ratingReviews);
			}

		}

		return ratingReviewsCollection;

	}

	private String crawlInternalId(Document doc){
		String internalID = null;
		Element elementID = doc.select(".detalhe_produto_informacao .mini_info .left").first();

		if (elementID != null) {
			String textAll = elementID.text().replaceAll("\\s+", "");
			int begin = textAll.indexOf(':') + 1;

			internalID = textAll.substring(begin).trim();
		}

		return internalID;
	}

	/**
	 * Average is calculate 
	 * Example: 
	 *  img src = ".../star5.png" [percentage bar] 0(number of evaluations of this star)/0,00%(percentage of votes)
	 *  img src = ".../star4.png" [percentage bar] 1(number of evaluations of this star)/100,00%(percentage of votes)
	 * 
	 * All rating checked were without evaluations in time crawler was made
	 * 
	 * @param document
	 * @return
	 */
	private Double getTotalAvgRating(Document docRating, Integer totalRating) {
		Double avgRating = 0.0;
		Elements rating = docRating.select(".avaliacao_estrelas_holder .linha");

		if (totalRating != null && totalRating > 0) {
			Double total = 0.0;

			for (Element e : rating) {
				Element starImg = e.select("> img").first();
				Element totalStar = e.select("> p > span").first();

				if (totalStar != null && starImg != null) {
					try {
						String votes = totalStar.text().split("/")[0].trim();
						String star = starImg.attr("src");
						
						Integer totalVotes = Integer.parseInt(votes);

						if(star.contains("star5")){
							total += totalVotes * 5;
						} else if(star.contains("star4")){
							total += totalVotes * 4;
						} else if(star.contains("star3")){
							total += totalVotes * 3;
						} else if(star.contains("star2")){
							total += totalVotes * 2;
						} else if(star.contains("star1")){
							total += totalVotes * 1;
						}
					} catch (Exception e1) {
					}
				}
			}

			avgRating = MathCommonsMethods.normalizeTwoDecimalPlaces(total / totalRating);
		}

		return avgRating;
	}

	/**
	 * Number of ratings appear in api 
	 * @param docRating
	 * @return
	 */
	private Integer getTotalNumOfRatings(Document docRating) {
		Integer totalRating = null;
		Element totalRatingElement = docRating.select(".avaliacao_estrelas_holder span.gray2").last();

		if(totalRatingElement != null) {
			totalRating = Integer.parseInt(totalRatingElement.ownText().replaceAll("[^0-9]", "").trim());
		}

		return totalRating;
	}


	private boolean isProductPage(Document document) {
		Elements selectedElements;
		String[] productUrlFeatures = {"#conteudo_produto", "#detalhe_produto", ".detalhe_produto_informacao", ".detalhe_produto_informacao .mini_info .left"};
		int size = productUrlFeatures.length;

		for(int i = 0; i < size; i++) {
			selectedElements = document.select( productUrlFeatures[i] );
			if(selectedElements == null || selectedElements.size() == 0) {
				return false;
			}
		}

		return true;
	}

}
