package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloOnofreRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloOnofreRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(document)) {
			
			RatingsReviews ratingReviews = crawlRating(document);
			ratingReviews.setInternalId(crawlInternalId(document));

			ratingReviewsCollection.addRatingReviews(ratingReviews);
		}


		return ratingReviewsCollection;
	}

	private RatingsReviews crawlRating(Document doc) {
		RatingsReviews ratingReviews = new RatingsReviews();

		ratingReviews.setDate(session.getDate());
		
		JSONObject chaordic = crawlChaordicJson(doc);
		
		if(chaordic.has("product")) {
			JSONObject product = chaordic.getJSONObject("product");
			
			if(product.has("details")) {
				JSONObject details = product.getJSONObject("details");
				
				if(details.has("rating")) {
					JSONObject rating = details.getJSONObject("rating");
					
					if(rating.has("total") && rating.has("value")) {
						ratingReviews.setTotalRating(Integer.parseInt(rating.getString("total")));
						ratingReviews.setAverageOverallRating(Double.parseDouble(rating.getString("value")));
					}
				}
			}
		}

		return ratingReviews;
	}


	private boolean isProductPage(Document doc) {
		Element id = doc.select("#cphConteudo_hf_id_produto").first();
		
		if(id != null) {
			return true;
		}
		
		return false;
	}

	private String crawlInternalId(Document doc) {
		String internalId = null;
		Element id = doc.select("#cphConteudo_hf_id_produto").first();
		
		if(id != null) {
			internalId = id.val();
		}

		return internalId;
	}

	private JSONObject crawlChaordicJson(Document doc) {
		JSONObject chaordic = new JSONObject();
		Elements scripts = doc.select("script[language=javascript][type=\"text/javascript\"]");
		
		for(Element e : scripts) {
			String script = e.outerHtml().replaceAll(" ", "");
			String index = "chaordic_meta=";
			
			if(script.contains(index)) {
				int x = script.indexOf(index) + index.length();
				int y = script.indexOf("<", x);
				
				String json = script.substring(x, y).replace("newDate()", "\"\""); // some cases has new Date() in json
				
				try {
					chaordic = new JSONObject(json);
				} catch (JSONException ex) {
					Logging.printLogError(logger, CommonMethods.getStackTrace(ex));
				}
				
				break;
			}
		}
		
		return chaordic;
	}
}
