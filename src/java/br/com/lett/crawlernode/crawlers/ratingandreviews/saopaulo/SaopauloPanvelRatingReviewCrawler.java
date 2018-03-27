package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

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
			ratingReviews.setInternalId(crawlInternalId(document));
			
			Element numRatingsElement = document.select("div.avaliacao.box_rounded span.num_votantes strong").first();
			if (numRatingsElement != null) {
				String text = numRatingsElement.text();
				List<String> parsedNumbers = MathUtils.parseNumbers(text);
				
				if (!parsedNumbers.isEmpty()) {
					Integer numRatings = Integer.parseInt(parsedNumbers.get(0));
					
					if (numRatings != null) {
						Double totalRating = crawlTotalRating(document);
						Double avgRating = MathUtils.normalizeTwoDecimalPlaces(totalRating);
						
						ratingReviews.setTotalRating(numRatings);
						ratingReviews.setAverageOverallRating(avgRating);
					}
				}
			}
			
			ratingReviewsCollection.addRatingReviews(ratingReviews);
			
		}
		

		return ratingReviewsCollection;
	}
	
	private Double crawlTotalRating(Document document) {
		Double totalRating = 0.0;
		Elements ratingElements = document.select("div.avaliacao.box_rounded span.aval-atual table.table-ranking tr");
		
		for (Element ratingElement : ratingElements) {
			Element starElement = ratingElement.select("td").first();
			Element starRatingElement = ratingElement.select("td").last();
			
			if (starElement != null && starRatingElement != null) {
				Integer starNumber = crawlStarNumber(starElement);
				
				
				if (starNumber != null) {
					Double starRating = crawlStarRating(starRatingElement);					
					totalRating = totalRating + (starNumber * starRating);
				}
			}
		}
		
		return totalRating;
	}	
	
	private Integer crawlStarNumber(Element tdElement) {
		Integer starNumber = null;
		String starText = tdElement.select("img").first().attr("alt");
		if (!starText.isEmpty()) {
			List<String> parsedNumbers = MathUtils.parseNumbers(starText);
			if (!parsedNumbers.isEmpty()) {
				starNumber = Integer.parseInt(parsedNumbers.get(0));
			}
		}
		return starNumber;
	}
	
	private Double crawlStarRating(Element tdElement) {
		Double starRating = 0.0;
		
		String text = tdElement.text().trim();
		List<String> parsedNumbers = MathUtils.parseNumbers(text);
		if (!parsedNumbers.isEmpty()) {
			starRating = Integer.parseInt(parsedNumbers.get(0))/100.0;
		}
		
		return starRating;
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
