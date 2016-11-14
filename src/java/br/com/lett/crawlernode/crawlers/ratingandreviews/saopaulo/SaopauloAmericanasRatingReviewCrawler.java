package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.regex.Pattern;

import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.RatingReviewCrawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloAmericanasRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloAmericanasRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
		
		if (isProductPage(session.getOriginalURL())) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			RatingsReviews ratingReviews = crawlRatingReviews(document);
			ratingReviewsCollection.addRatingReviews(ratingReviews);
			
		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return ratingReviewsCollection;
	}
	
	private boolean isProductPage(String url) {
		if (url.startsWith("http://www.americanas.com.br/produto/")) return true;
		return false;
	}
	
	/**
	 * Crawl rating and reviews stats using the bazaar voice endpoint.
	 * To get only the stats summary we need at first, we only have to do
	 * one request. If we want to get detailed information about each review, we must
	 * perform pagination.
	 *
	 * @param document
	 * @return
	 */
	private RatingsReviews crawlRatingReviews(Document document) {
		RatingsReviews ratingReviews = new RatingsReviews();
		
		String bazaarVoicePassKey = crawlBazaarVoiceEndpointPassKey(document);
		String skuInternalPid = crawlSkuInternalPid(document);
		
		String endpointRequest = assembleBazaarVoiceEndpointRequest(skuInternalPid, bazaarVoicePassKey, 0, 5);
		
		DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, endpointRequest, null, null);
		
		return ratingReviews;
	}
	
	/**
	 * e.g: 
	 * http://api.bazaarvoice.com/data/reviews.json?apiversion=5.4
	 * &passkey=oqu6lchjs2mb5jp55bl55ov0d
	 * &Offset=0
	 * &Limit=5
	 * &Sort=SubmissionTime:desc
	 * &Filter=ProductId:113048617
	 * &Include=Products
	 * &Stats=Reviews
	 * 
	 * Endpoint request parameters:
	 * <p>
	 * &passKey: the password used to request the bazaar voice endpoint.
	 * This pass key e crawled inside the html of the sku page, inside a script tag.
	 * More details on how to crawl this passKey
	 * </p>
	 * <p>
	 * &Offset: the number of the chunk of data retrieved by the endpoint. If
	 * we want the second chunk, we must add this value by the &Limit parameter.
	 * </p>
	 * <p>
	 * &Limit: the number of reviews that a request will return, at maximum.
	 * </p>
	 * 
	 * The others parameters we left as default.
	 * 
	 * Request Method: GET
	 */
	private String assembleBazaarVoiceEndpointRequest(
			String skuInternalPid,
			String bazaarVoiceEnpointPassKey,
			Integer offset,
			Integer limit) {
		
		StringBuilder request = new StringBuilder();
		
		request.append("http://api.bazaarvoice.com/data/reviews.json?apiversion=5.4");
		request.append("&passkey=" + bazaarVoiceEnpointPassKey);
		request.append("&Offset=" + offset);
		request.append("&Limit=" + limit);
		request.append("&Sort=SubmissionTime:desc");
		request.append("&Filter=ProductId:" + skuInternalPid);
		request.append("&Include=Products");
		request.append("&Stats=Reviews");
		
		return request.toString();
	}
	
	/**
	 * Crawl the bazaar voice endpoint passKey on the sku page.
	 * The passKey is located inside a script tag, which contains
	 * a json object is several metadata, including the passKey.
	 * 
	 * e.g:
	 * 
	 * window.__INITIAL_STATE__ = {"configuration":{"warrantyBaseUrl":"http://www.americanas.com.br/garantia","brandId":"02","freight":{"XP":"A Jato"},
	 * "beacon":"http://img.americanas.com.br/mktacom/beacon/beacon.js",
	 * "domain":"www.americanas.com.br",
	 * "neemuGravaUrl":"https://laas.americanas.com.br/acom/grava.php",
	 * "opns":["YSMESP","YYNKZB","YYNKZU","FACEBOOKADS","FACEBOOKDPA","YSMESC"],
	 * "fullstory":{"enabled":true,"cookieNick":"acomNick"},
	 * ...
	 * "bazaarvoicePasskey":"oqu6lchjs2mb5jp55bl55ov0d",
	 * ...
	 * };
	 * 
	 * @param document
	 * @return
	 */
	private String crawlBazaarVoiceEndpointPassKey(Document document) {
		String passKey = null;
		
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject windowInitialStateJson = null;

		for (Element tag : scriptTags) {                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith("window.__INITIAL_STATE__= ")) {
					windowInitialStateJson = new JSONObject
							(node.getWholeData().split(Pattern.quote("window.__INITIAL_STATE__= "))[1] +
							 node.getWholeData().split(Pattern.quote("window.__INITIAL_STATE__= "))[1].split(Pattern.quote("};"))[0]
							);
				}
			}        
		}
		
		if (windowInitialStateJson != null) {
			if (windowInitialStateJson.has("bazaarvoicePasskey")) {
				passKey = windowInitialStateJson.getString("bazaarvoicePasskey");
			}
		}
		
		return passKey;
	}
	
	private String crawlSkuInternalPid(Document document) {
		String skuInternalPid = null;
		
		return skuInternalPid;
	}

}
