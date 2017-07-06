package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONObject;
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
public class BrasilMagazineluizaRatingReviewNewCrawler extends RatingReviewCrawler {

	public BrasilMagazineluizaRatingReviewNewCrawler(Session session) {
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
		// Sku info in json on html
		JSONObject skuJsonInfo = crawlFullSKUInfo(doc, "digitalData = ");

		// InternalId
		String internalId = crawlInternalId(skuJsonInfo);
		
		RatingsReviews ratingReviews = crawlRatingReviews(doc);
		ratingReviews.setInternalId(internalId);
		
		return ratingReviews;
	}
	
	/**
	 * Crawl Internal ID 
	 * @param doc
	 * @return
	 */
	private String crawlInternalId(JSONObject skuJson) {
		String internalId = null;

		if(skuJson.has("idSku")) {
			internalId = skuJson.getString("idSku");
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
		Element total = doc.select(".interaction-client__rating-info > span").last();
		
		if (total != null) {
			totalReviewCount = Integer.parseInt(total.ownText().replaceAll("[^0-9]", ""));
		}
		return totalReviewCount;
	}

	private Double getAverageOverallRating(Document doc) {
		Double avgOverallRating = null;
		Element avg = doc.select(".interaction-client__rating-info > span").first();
		
		if (avg != null) {
			avgOverallRating = Double.parseDouble(avg.ownText().replaceAll("[^0-9,]", "").replace(",", "."));
		}
		return avgOverallRating;
	}

	private boolean isProductPage(String url) {
		return url.contains("/p/") || url.contains("/p1/");
	}
	
	/**
	 * eg:
	 * 
	 * "reference":"com Função Limpa Fácil",
	 *	"extendedWarranty":true,
	 *	"idSku":"0113562",
	 *	"idSkuFull":"011356201",
	 *	"salePrice":429,
	 *	"imageUrl":"http://i.mlcdn.com.br//micro-ondas-midea-liva-mtas4-30l-com-funcao-limpa-facil/v/210x210/011356201.jpg",
	 *	"fullName":"micro%20ondas%20midea%20liva%20mtas4%2030l%20-%20com%20funcao%20limpa%20facil",
	 *	"title":"Micro-ondas Midea Liva MTAS4 30L",
	 *	"cashPrice":407.55,
	 *	"brand":"midea",
	 *	"stockAvailability":true
	 * 
	 * @return a json object containing all sku informations in this page.
	 */
	private JSONObject crawlFullSKUInfo(Document document, String token) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJsonProduct = new JSONObject();
		JSONObject skuJson = new JSONObject();

		for (Element tag : scriptTags){                
			String html = tag.outerHtml();

			if(html.contains(token)) {
				int x = html.indexOf(token) + token.length();
				int y = html.indexOf("};", x) + 1;

				String json = html.substring(x, y)
						.replace("window.location.host", "''")
						.replace("window.location.protocol", "''")
						.replace("window.location.pathname", "''")
						.replace("document.referrer", "''")
						.replace("encodeURIComponent(", "")
						.replace("'),", "',");

				skuJson = new JSONObject(json);
			}
		}

		if(skuJson.has("page")){
			JSONObject jsonPage = skuJson.getJSONObject("page");

			if(jsonPage.has("product")){
				skuJsonProduct = jsonPage.getJSONObject("product");
			}
		}

		return skuJsonProduct;
	}

}