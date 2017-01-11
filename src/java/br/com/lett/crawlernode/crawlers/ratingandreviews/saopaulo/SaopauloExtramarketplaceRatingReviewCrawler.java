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
import br.com.lett.crawlernode.util.MathCommonsMethods;

/**
 * Date: 14/12/16
 * @author gabriel
 *
 */
public class SaopauloExtramarketplaceRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloExtramarketplaceRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(document)) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			RatingsReviews ratingReviews = new RatingsReviews();			
			ratingReviews.setDate(session.getDate());

			Integer totalNumOfEvaluations = getTotalNumOfRatings(document);			
			Double avgRating = getTotalAvgRating(document, totalNumOfEvaluations);

			ratingReviews.setTotalReviews(totalNumOfEvaluations);
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
	 * Example: 
	 * (baseado em 60 avaliações)
	 * Distribuição das Notas
	 * 5 Estrelas (32)
	 * 4 Estrelas (9)
	 * 3 Estrelas (3)
	 * 2 Estrelas (3)
	 * 1 Estrela (13)
	 * 
	 * @param Double
	 * @return
	 */
	private Double getTotalAvgRating(Document docRating, Integer totalRating) {
		Double avgRating = null;
		Elements rating = docRating.select("#pr-snapshot-histogram .pr-ratings-histogram-content > li");

		if (totalRating != null && totalRating > 0) {
			Double total = 0.0;

			for (Element e : rating) {
				Element totalStar = e.select(".pr-histogram-count span").first();

				if (totalStar != null) {
					Integer totalVotes = Integer.parseInt(totalStar.text().replaceAll("[^0-9]", "").trim());
					
					if(e.hasClass("pr-histogram-5Stars")){
						total += totalVotes * 5;
					} else if(e.hasClass("pr-histogram-4Stars")){
						total += totalVotes * 4;
					} else if(e.hasClass("pr-histogram-3Stars")){
						total += totalVotes * 3;
					} else if(e.hasClass("pr-histogram-2Stars")){
						total += totalVotes * 2;
					} else if(e.hasClass("pr-histogram-1Stars")){
						total += totalVotes * 1;
					}
				}
			}

			avgRating = MathCommonsMethods.normalizeTwoDecimalPlaces(total / totalRating);
		}

		return avgRating;
	}


	private Integer getTotalNumOfRatings(Document doc) {
		Integer totalRating = null;
		Element totalRatingElement = doc.select(".pr-snapshot-average-based-on-text .count").first();

		if(totalRatingElement != null){
			totalRating = Integer.parseInt(totalRatingElement.text());
		}

		return totalRating;
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
