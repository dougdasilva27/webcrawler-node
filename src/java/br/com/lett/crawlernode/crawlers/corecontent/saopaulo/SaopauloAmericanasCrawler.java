package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SaopauloB2WCrawlersUtils;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SaopauloAmericanasCrawler extends B2WCrawler {

   private static final String HOME_PAGE = "https://www.americanas.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "americanas.com";
   private static final int RATING_API_VERSION = 1;
   private static final String KEY_SHA_256 = "291cd512e18fb8148bb39aa57d389741fd588346b0fd8ce2260a21c3a34b6598";

   public SaopauloAmericanasCrawler(Session session) {
      super(session);
      super.subSellers = Arrays.asList("b2w", "lojas americanas", "lojas americanas mg", "lojas americanas rj", "lojas americanas sp", "lojas americanas rs");
      super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
      super.homePage = HOME_PAGE;
      super.config.setFetcher(FetchMode.JSOUP);
   }


   @Override
   protected RatingsReviews crawlRatingReviews(JSONObject frontPageJson, String skuInternalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();
      JSONObject rating = fetchRatingApi(skuInternalPid);

      JSONObject data = rating.optJSONObject("data");

      if (data != null) {

         JSONObject product = data.optJSONObject("product");

         if (product != null) {

            JSONObject ratingInfo = product.optJSONObject("rating");

            if (ratingInfo != null) {
               ratingReviews.setTotalWrittenReviews(ratingInfo.optInt("reviews", 0));
               ratingReviews.setTotalRating(ratingInfo.optInt("reviews", 0));
               ratingReviews.setAverageOverallRating(ratingInfo.optDouble("average", 0d));
            } else {
               ratingReviews.setTotalWrittenReviews(0);
               ratingReviews.setTotalRating(0);
               ratingReviews.setAverageOverallRating(0.0);
            }
         }
      }

      return ratingReviews;
   }

   private JSONObject fetchRatingApi(String internalId) {
      StringBuilder url = new StringBuilder();
      url.append("https://catalogo-bff-v2-americanas.b2w.io/graphql?");

      JSONObject variables = new JSONObject();
      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      variables.put("productId", internalId);
      variables.put("offset", 5);

      persistedQuery.put("version", RATING_API_VERSION);
      persistedQuery.put("sha256Hash", KEY_SHA_256);

      extensions.put("persistedQuery", persistedQuery);

      StringBuilder payload = new StringBuilder();
      payload.append("operationName=productReviews");
      payload.append("&device=desktop");
      payload.append("&oneDayDelivery=undefined");
      try {
         payload.append("&variables=" + URLEncoder.encode(variables.toString(), "UTF-8"));
         payload.append("&extensions=" + URLEncoder.encode(extensions.toString(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
      url.append(payload.toString());

      Request request = Request.RequestBuilder.create()
         .setUrl(url.toString())
         .build();

      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   protected Offers scrapOffers(Document doc, String internalId, String internalPid) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      String scrapUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".offers-box__Wrapper-sc-189v1x3-0.hqboso .more-offers__Touchable-sc-15yqej3-2[href]", "href");
      String offersPageUrl = CrawlerUtils.completeUrl(scrapUrl, "https://", "americanas.com.br");

      JSONObject jsonSeller;

      if (offersPageUrl != null) {
         Request request = Request.RequestBuilder.create().setUrl(offersPageUrl).setProxyservice(
            Arrays.asList(
               ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
            )
         ).build();
         Response response = this.dataFetcher.get(session, request);
         String content = response.getBody();

         int statusCode = response.getLastStatusCode();

         if ((Integer.toString(statusCode).charAt(0) != '2' &&
            Integer.toString(statusCode).charAt(0) != '3'
            && statusCode != 404)) {
            request.setProxyServices(Arrays.asList(
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY));

            content = new JsoupDataFetcher().get(session, request).getBody();
         }

         Document offersDoc = Jsoup.parse(content);

         jsonSeller = CrawlerUtils.selectJsonFromHtml(offersDoc, "script", "window.__PRELOADED_STATE__ =", ";", false, true);
      } else {
         jsonSeller = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__PRELOADED_STATE__ =", null, false, true);

      }

      JSONObject offersJson = SaopauloB2WCrawlersUtils.extractJsonOffers(jsonSeller, internalPid);

      JSONObject pages = jsonSeller.optJSONObject("pages");
      Map<String, Double> mapOfSellerIdAndPrice = new HashMap<>();
      boolean twoPositions = false;

      // Getting informations from sellers.
      if (offersJson.has(internalId)) {
         JSONArray sellerInfo = offersJson.getJSONArray(internalId);

         // The Business logic is: if we have more than 1 seller is buy box
         boolean isBuyBox = sellerInfo.length() > 1;

         for (int i = 0; i < sellerInfo.length(); i++) {
            JSONObject info = (JSONObject) sellerInfo.get(i);
            if (info.has("sellerName") && !info.isNull("sellerName") && info.has("id") && !info.isNull("id")) {
               String name = info.get("sellerName").toString();
               String internalSellerId = info.get("id").toString();
               Integer mainPagePosition = i == 0 ? 1 : null;
               Integer sellersPagePosition = i == 0 ? 1 : null;

               if (i > 0 && name.equalsIgnoreCase("b2w")) {
                  sellersPagePosition = 2;
                  twoPositions = true;
               }

               Pricing pricing = scrapPricing(info, i, internalSellerId, mapOfSellerIdAndPrice, false);

               Offer offer = Offer.OfferBuilder.create()
                  .setInternalSellerId(internalSellerId)
                  .setSellerFullName(name)
                  .setMainPagePosition(mainPagePosition)
                  .setSellersPagePosition(sellersPagePosition)
                  .setPricing(pricing)
                  .setIsBuybox(isBuyBox)
                  .setIsMainRetailer(false)
                  .build();

               offers.add(offer);
            }
         }
      }

      if (offers.size() > 1) {
         // Sellers page positios is order by price, in this map, price is the value
         Map<String, Double> sortedMap = sortMapByValue(mapOfSellerIdAndPrice);

         int position = twoPositions ? 3 : 2;

         for (Map.Entry<String, Double> entry : sortedMap.entrySet()) {
            for (Offer offer : offers.getOffersList()) {
               if (offer.getInternalSellerId().equals(entry.getKey()) && offer.getSellersPagePosition() == null) {
                  offer.setSellersPagePosition(position);
                  position++;
               }
            }
         }
      }
      return offers;
   }
}
