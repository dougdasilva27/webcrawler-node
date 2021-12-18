package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SaopauloB2WCrawlersUtils;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SaopauloAmericanasCrawler extends B2WCrawler {

   private static final String HOME_PAGE = "https://www.americanas.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "americanas.com";
   private static final String MAIN_SELLER_NAME_LOWER_FROM_HTML = "Americanas";

   private static final List<String> UserAgent = Arrays.asList(
      "Mozilla/5.0 (iPhone; CPU iPhone OS 14_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/93.0.4577.39 Mobile/15E148 Safari/604.1",
      "Mozilla/5.0 (iPad; CPU OS 14_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/93.0.4577.39 Mobile/15E148 Safari/604.1",
      "Mozilla/5.0 (iPod; CPU iPhone OS 14_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/93.0.4577.39 Mobile/15E148 Safari/604.1",
      "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; SM-A205U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; SM-A102U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; SM-G960U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; LM-X420) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; LM-Q710(FGN)) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36"
   );

   public SaopauloAmericanasCrawler(Session session) {
      super(session);
      super.subSellers = Arrays.asList("b2w", "lojas americanas", "lojas americanas mg", "lojas americanas rj", "lojas americanas sp", "lojas americanas rs");
      super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
      super.sellerNameLowerFromHTML = MAIN_SELLER_NAME_LOWER_FROM_HTML;
      super.homePage = HOME_PAGE;
      super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   protected Document fetch() {
      return Jsoup.parse(fetchPage(session.getOriginalURL(), this.dataFetcher, cookies, headers, session));
   }


   public static Map<String, String> getHeaders() {
      Random random = new Random();

      Map<String, String> headers = new HashMap<>();
      headers.put("sec-ch-ua", "\"Chromium\";v=\"92\", \" Not A;Brand\";v=\"99\", \"Google Chrome\";v=\"92\"");
      headers.put("sec-ch-ua-mobile", "?0");
      headers.put("upgrade-insecure-requests", "1");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,/;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("user-agent", UserAgent.get(random.nextInt(UserAgent.size())));

      return headers;
   }

   public static String fetchPage(String url, DataFetcher df, List<Cookie> cookies, Map<String, String> headers, Session session) {

      Map<String, String> headersAmericanas = getHeaders();

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headersAmericanas)
         .setSendUserAgent(false)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .setForbiddenCssSelector("#px-captcha")
               .build()
         )
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY
            )
         )
         .build();

      Response response = df.get(session, request);
      String content = response.getBody();

      int statusCode = response.getLastStatusCode();

      if ((Integer.toString(statusCode).charAt(0) != '2' &&
         Integer.toString(statusCode).charAt(0) != '3'
         && statusCode != 404)) {


         request.setHeaders(getHeaders());
         content = new FetcherDataFetcher().get(session, request).getBody();
      }

      return content;
   }

   @Override
   protected RatingsReviews crawlRatingReviews(JSONObject frontPageJson, String skuInternalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();
      JSONObject product = JSONUtils.getValueRecursive(frontPageJson, "pages.undefined.queries.productReviews.result.product", JSONObject.class);
      JSONObject reviews = product != null ? product.optJSONObject("reviews") : null;

      if (reviews != null) {

         JSONObject rating = product.optJSONObject("rating");

         if (rating != null) {
            ratingReviews.setTotalWrittenReviews(rating.optInt("reviews"));
            ratingReviews.setTotalRating(rating.optInt("reviews"));
            ratingReviews.setAverageOverallRating(rating.optDouble("average"));
            ratingReviews.setAdvancedRatingReview(scrapAdvancedRatingReview(reviews));
         }
      }

      return ratingReviews;
   }


   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject reviews) {
      int star1 = 0;
      int star2 = 0;
      int star3 = 0;
      int star4 = 0;
      int star5 = 0;

      JSONArray ratingDistribution = reviews.optJSONArray("ratingDistribution");

      if (ratingDistribution != null) {
         for (Object o : ratingDistribution) {

            JSONObject ratingDistributionObject = (JSONObject) o;

            int ratingValue = ratingDistributionObject.optInt("ratingValue");
            int ratingCount = ratingDistributionObject.optInt("count");

            switch (ratingValue) {
               case 5:
                  star5 = ratingCount;
                  break;
               case 4:
                  star4 = ratingCount;
                  break;
               case 3:
                  star3 = ratingCount;
                  break;
               case 2:
                  star2 = ratingCount;
                  break;
               case 1:
                  star1 = ratingCount;
                  break;
               default:
                  break;
            }
         }
      }

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }

   private void scrapAndSetInfoForMainPage(Document doc, Offers offers, String internalId, String internalPid, int arrayPosition) throws OfferException, MalformedPricingException {
      JSONObject jsonSeller = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__PRELOADED_STATE__ =", null, false, true);
      JSONObject offersJson = SaopauloB2WCrawlersUtils.newWayToExtractJsonOffers(jsonSeller, internalPid, arrayPosition);
      setOffersForMainPageSeller(offers, offersJson, internalId);
   }

   @Override
   protected Offers scrapOffers(Document doc, String internalId, String internalPid, int arrayPosition) throws MalformedPricingException, OfferException {

      Offers offers = new Offers();

      String offersPageUrl = "https://www.americanas.com.br/parceiros/" + internalPid + "?productSku=" + internalId;

      Document sellersDoc = acessOffersPage(offersPageUrl);
      Elements sellersFromHTML = sellersDoc.select(".src__Background-sc-1y5gtgz-1 .src__Card-sc-1y5gtgz-3 > div");
      Elements sellersFromHTMLNewWay = sellersDoc.select(".src__OfferList-sc-3rb2gj-4 .src__Card-sc-3rb2gj-3");
      Map<String, String> listSelectors = new HashMap<>();

      if (!sellersFromHTML.isEmpty()) {

         listSelectors.put("selectorSellerName", ".seller-card__SellerInfo-pf2gd6-2 p:nth-child(2)");
         listSelectors.put("selectorSellerId", ".seller-card__ButtonBox-pf2gd6-4 a");

         setOffersForSellersPage(offers, sellersFromHTML, listSelectors);
      } else if (!sellersFromHTMLNewWay.isEmpty()) {

         listSelectors.put("selectorSellerName", ".sold-and-delivery__Seller-sc-1kx2hv4-2:nth-child(2)");
         listSelectors.put("selectorSellerId", ".seller-card__ButtonContainer-nrtn3f-6 a");

         setOffersForSellersPage(offers, sellersFromHTMLNewWay, listSelectors);

      } else {
        /*
               caso sellersFromHTML seja vazio significa que fomos bloqueados
               durante a tentativa de capturar as informações na pagina de sellers
               ou que o produto em questão não possui pagina de sellers.
               Nesse caso devemos capturar apenas as informações da pagina principal.
               */
         scrapAndSetInfoForMainPage(doc, offers, internalId, internalPid, arrayPosition);

      }

      return offers;
   }

   protected Document acessOffersPage(String offersPageURL) {
      return Jsoup.parse(fetchPage(offersPageURL, this.dataFetcher, cookies, headers, session));
   }


   private void setOffersForMainPageSeller(Offers offers, JSONObject offersJson, String internalId) throws OfferException, MalformedPricingException {
      Map<String, Double> mapOfSellerIdAndPrice = new HashMap<>();

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


   private void setOffersForSellersPage(Offers offers, Elements sellers, Map<String, String> listSelectors) throws MalformedPricingException, OfferException {

      if (sellers.size() > 0) {

         for (int i = 0; i < sellers.size(); i++) {
            Element sellerInfo = sellers.get(i);
            boolean isBuyBox = sellers.size() > 1;
            String sellerName = CrawlerUtils.scrapStringSimpleInfo(sellerInfo, listSelectors.get("selectorSellerName"), false);
            String rawSellerId = CrawlerUtils.scrapStringSimpleInfoByAttribute(sellerInfo, listSelectors.get("selectorSellerId"), "href");
            String sellerId = scrapSellerIdFromURL(rawSellerId);
            Integer mainPagePosition = i == 0 ? 1 : null;
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


   private String scrapSellerIdFromURL(String rawSellerId) {
      String sellerId = "";
      if (rawSellerId != null) {
         Pattern pattern = Pattern.compile("sellerId=([0-9]*)");
         Matcher matcher = pattern.matcher(session.getOriginalURL());
         if (matcher.find()) {
            sellerId = matcher.group(1);
         }
      }
      return sellerId;
   }

   private Pricing scrapPricingForOffersPage(Element sellerInfo)
      throws MalformedPricingException {

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(sellerInfo, ".src__ListPrice-sc-1jvw02c-2", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(sellerInfo, ".src__BestPrice-sc-1jvw02c-5", null, false, ',', session);
      BankSlip bt = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCardsForSellersPage(sellerInfo, spotlightPrice);

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
