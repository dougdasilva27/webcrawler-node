package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcherNO;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;

/**
 * Date: 30/08/17
 * 
 * @author gabriel
 *
 *         In time crawler was made, there was no rating on any product in this market
 *
 */
public class BrasilFarma22RatingReviewCrawler extends RatingReviewCrawler {

	public BrasilFarma22RatingReviewCrawler(Session session) {
		super(session);
	}


	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(document)) {
			RatingsReviews ratingReviews = new RatingsReviews();
			ratingReviews.setDate(session.getDate());

			JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(document, session);

			if (skuJson.has("productId")) {
				String internalPid = Integer.toString(skuJson.getInt("productId"));

				Document docRating = crawlPageRatings(internalPid);

				Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
				Double avgRating = getTotalAvgRating(docRating);

				ratingReviews.setTotalRating(totalNumOfEvaluations);
				ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
				ratingReviews.setAverageOverallRating(avgRating);

				List<String> idList = crawlIdList(skuJson);
				for (String internalId : idList) {
					RatingsReviews clonedRatingReviews = (RatingsReviews) ratingReviews.clone();
					clonedRatingReviews.setInternalId(internalId);
					ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
				}
			}
		}

		return ratingReviewsCollection;

	}

	/**
	 * Api Ratings
	 * 
	 * @return document
	 */
	private Document crawlPageRatings(String internalPid) {
		Document doc = new Document("");
		String url =
				"http://service.yourviews.com.br/review/GetReview?storeKey=d8f4f406-5164-4042-81aa-a7fe0ec787f0"
						+ "&productStoreId=" + internalPid + "&extendedField=&callback=_jqjsp&_1504115699466=";

		String code = DataFetcherNO.fetchString(DataFetcherNO.GET_REQUEST, session, url, null, cookies);

		if (code.contains("_jqjsp(")) {
			int x = code.indexOf("_jqjsp(") + "_jqjsp(".length();
			int y = code.indexOf(");", x);

			String json = code.substring(x, y).trim();

			if (json.startsWith("{") && json.endsWith("}")) {
				JSONObject html = new JSONObject(json);

				if (html.has("html")) {
					doc = Jsoup.parse(html.getString("html"));
				}
			}
		} else {
			doc = Jsoup.parse(code);
		}

		return doc;
	}

	/**
	 * Number of ratings appear in rating page
	 * 
	 * @param docRating
	 * @return
	 */
	private Integer getTotalNumOfRatings(Document docRating) {
		Integer totalRating = 0;
		Element totalRatingElement = docRating.select("strong[itemprop=ratingCount]").first();

		if (totalRatingElement != null) {
			String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

			if (!totalText.isEmpty()) {
				totalRating = Integer.parseInt(totalText);
			}
		}

		return totalRating;
	}

	/**
	 * Average appear in rating page
	 * 
	 * @param document
	 * @return
	 */
	private Double getTotalAvgRating(Document docRating) {
		Double avgRating = 0d;
		Element rating = docRating.select("meta[itemprop=ratingValue]").first();

		if (rating != null) {
			avgRating = Double.parseDouble(rating.attr("content"));
		}

		return avgRating;
	}

	private List<String> crawlIdList(JSONObject skuJson) {
		List<String> idList = new ArrayList<>();

		if (skuJson.has("skus")) {
			JSONArray skus = skuJson.getJSONArray("skus");

			for (int i = 0; i < skus.length(); i++) {
				JSONObject sku = skus.getJSONObject(i);

				if (sku.has("sku")) {
					idList.add(Integer.toString(sku.getInt("sku")));
				}
			}
		}

		return idList;
	}

	private boolean isProductPage(Document document) {
		return document.select(".productName").first() != null;
	}
}
