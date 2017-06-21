package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
public class SaopauloCasasbahiaRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloCasasbahiaRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(document)) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			RatingsReviews ratingReviews = new RatingsReviews();			
			ratingReviews.setDate(session.getDate());

			Integer totalNumOfEvaluations = getTotalRating(document);			
			Double avgRating = getTotalAvgRating(document);

			ratingReviews.setTotalRating(totalNumOfEvaluations);
			ratingReviews.setAverageOverallRating(avgRating);

			List<String> idList = crawlInternalIds(document);
			for (String internalId : idList) {
				RatingsReviews clonedRatingReviews = (RatingsReviews)ratingReviews.clone();
				clonedRatingReviews.setInternalId(internalId);
				ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
			}
		}

		return ratingReviewsCollection;

	}

	/**
	 * 
	 * @param doc
	 * @return
	 */
	private Integer getTotalRating(Document doc) {
		Integer total = null;
		
		Element rating = doc.select(".rating .rating-count").first();
		
		if(rating != null) {
			total = Integer.parseInt(rating.ownText().replaceAll("[^0-9]", ""));
		}
	
		return total;
	}
	
	/**
	 * @param Double
	 * @return
	 */
	private Double getTotalAvgRating(Document doc) {
		Double avgRating = null;	
		
		Element avg = doc.select(".rating .rating-value").first();
		
		if(avg != null) {
			avgRating = Double.parseDouble(avg.ownText().replace(",", "."));
		}
		
		return avgRating;
	}

	private List<String> crawlInternalIds(Document doc){
		List<String> ids = new ArrayList<>();
		String internalPid = crawlInternalPid(doc);
		
		if(hasProductVariations(doc)) {
			Elements skuOptions = doc.select(".produtoSku option[value]:not([value=\"\"])");
			
			for(Element e : skuOptions){
				ids.add(internalPid + "-" + e.attr("value"));
			}
			
		} else {
			Element elementDataSku = doc.select("#ctl00_Conteudo_hdnIdSkuSelecionado").first();

			if(elementDataSku != null) {
				ids.add(internalPid + "-" + elementDataSku.attr("value"));
			}
		}

		return ids;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Elements elementInternalId = document.select("script[type=text/javascript]");

		String idenfyId = "idProduct";

		for(Element e : elementInternalId){
			String script = e.outerHtml();

			if(script.contains(idenfyId)){
				script = script.replaceAll("\"", "");

				int x = script.indexOf(idenfyId);
				int y = script.indexOf(',', x + idenfyId.length());

				internalPid = script.substring(x + idenfyId.length(), y).replaceAll("[^0-9]", "").trim();
			}
		}


		return internalPid;
	}
	
	private boolean hasProductVariations(Document document) {
		Elements skuChooser = document.select(".produtoSku option[value]:not([value=\"\"])");

		if (skuChooser.size() > 1) {
			if(skuChooser.size() == 2){
				String prodOne = skuChooser.get(0).text();
				if(prodOne.contains("|")){
					prodOne = prodOne.split("\\|")[0].trim();
				}

				String prodTwo = skuChooser.get(1).text();
				if(prodTwo.contains("|")){
					prodTwo = prodTwo.split("\\|")[0].trim();
				}


				if(prodOne.equals(prodTwo)){
					return false;
				}
			}
			return true;
		} 

		return false;

	}
	
	private boolean isProductPage(Document doc) {
		Element productElement = doc.select(".produtoNome h1 span").first();

		if (productElement != null){
			return true;
		}
		
		return false;
	}

}
