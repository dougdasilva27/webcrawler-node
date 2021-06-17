package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SaopauloB2WCrawlersUtils;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SaopauloAmericanasCrawler extends B2WCrawler {

   private static final String HOME_PAGE = "https://www.americanas.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "americanas.com";
   private static final String MAIN_SELLER_NAME_LOWER_FROM_HTML = "Americanas";
   private static final int RATING_API_VERSION = 1;
   private static final String KEY_SHA_256 = "291cd512e18fb8148bb39aa57d389741fd588346b0fd8ce2260a21c3a34b6598";

   public SaopauloAmericanasCrawler(Session session) {
      super(session);
      super.subSellers = Arrays.asList("b2w", "lojas americanas", "lojas americanas mg", "lojas americanas rj", "lojas americanas sp", "lojas americanas rs");
      super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
      super.sellerNameLowerFromHTML = MAIN_SELLER_NAME_LOWER_FROM_HTML;
      super.homePage = HOME_PAGE;
      super.config.setFetcher(FetchMode.FETCHER);
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

   private void scrapAndSetInfoForMainPage(Document doc, Offers offers, String internalId, String internalPid, int arrayPosition) throws OfferException, MalformedPricingException {
      JSONObject jsonSeller = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__PRELOADED_STATE__ =", null, false, true);
      JSONObject offersJson = SaopauloB2WCrawlersUtils.newWayToExtractJsonOffers(jsonSeller,internalPid, arrayPosition);
      setOffersForMainPageSeller(offers, offersJson, internalId);
   }

   @Override
   protected Offers scrapOffers(Document doc, String internalId, String internalPid, int arrayPosition) throws MalformedPricingException, OfferException {

      Offers offers = new Offers();

         String offersPageUrl = "https://www.americanas.com.br/parceiros/"+ internalPid +"?productSku=" + internalId;

         Document sellersDoc = acessOffersPage(offersPageUrl);
         Elements sellersFromHTML = sellersDoc.select(".src__Background-sc-1y5gtgz-1 .src__Card-sc-1y5gtgz-3 > div");

         if (sellersFromHTML.isEmpty()){
               /*
               caso sellersFromHTML seja vazio significa que fomos bloqueados
               durante a tentativa de capturar as informações na pagina de sellers
               ou que o produto em questão não possui pagina de sellers.
               Nesse caso devemos capturar apenas as informações da pagina principal.
               */

            scrapAndSetInfoForMainPage(doc,offers,internalId,internalPid, arrayPosition);
         } else {

            setOffersForSellersPage(offers, sellersFromHTML);
         }

      return offers;
   }

   private Document acessOffersPage(String offersPageURL) {
      Request request = Request.RequestBuilder.create().setUrl(offersPageURL).setProxyservice(
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

      return Jsoup.parse(content);
   }


   private void setOffersForMainPageSeller (Offers offers ,JSONObject offersJson, String internalId) throws OfferException, MalformedPricingException {
      Map<String, Double> mapOfSellerIdAndPrice = new HashMap<>();
      boolean twoPositions = false;

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
                  sellersPagePosition = 1;
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
   }


   private void setOffersForSellersPage(Offers offers, Elements sellers) throws MalformedPricingException, OfferException {

      if(sellers.size() > 0){

         for(int i = 0; i < sellers.size(); i++){
            Element sellerInfo = sellers.get(i);
            boolean isBuyBox = sellers.size() > 1;
            String sellerName = CrawlerUtils.scrapStringSimpleInfo(sellerInfo,".seller-card__SellerInfo-pf2gd6-2  p:nth-child(2)",false);
            String rawSellerId = CrawlerUtils.scrapStringSimpleInfoByAttribute(sellerInfo,".seller-card__ButtonBox-pf2gd6-4 a","href");
            String sellerId = scrapSellerIdFromURL(rawSellerId);
            Integer mainPagePosition = i == 0 ? 1: null;
            Integer sellersPagePosition = i + 1;
            Pricing pricing = scrapPricingForOffersPage(sellerInfo);

            Offer offer = Offer.OfferBuilder.create()
               .setInternalSellerId(sellerId)
               .setSellerFullName(sellerName)
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

   private String scrapSellerIdFromURL(String rawSellerId){
      String sellerId = "";
      if(rawSellerId != null){
         sellerId = CommonMethods.getLast(rawSellerId.split("sellerId")).replaceAll("[^0-9]","").trim();
      }
      return sellerId;
   }

   private Pricing scrapPricingForOffersPage(Element sellerInfo)
      throws MalformedPricingException {

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(sellerInfo, ".src__ListPrice-sc-1jvw02c-2",null,false,',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(sellerInfo,".src__BestPrice-sc-1jvw02c-5",null,false,',', session);
      BankSlip bt = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCardsForSellersPage(sellerInfo,spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bt)
         .build();
   }

   private CreditCards scrapCreditCardsForSellersPage(Element sellerInfo, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }
}
