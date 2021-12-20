package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Pricing.PricingBuilder;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.Normalizer;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class B2WCrawler extends Crawler {
   protected Map<String, String> headers = new HashMap<>();
   private static final String MAIN_B2W_NAME_LOWER = "b2w";
   private static final Card DEFAULT_CARD = Card.VISA;
   protected String sellerNameLower;
   protected String sellerNameLowerFromHTML; // Americanas // Submarino
   protected List<String> subSellers;
   protected String homePage;
   protected String urlPageOffers;
   protected Map<String, String> listSelectors;
   protected Set<String> cards = Sets.newHashSet(DEFAULT_CARD.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public B2WCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
      this.setHeaders();
   }

   protected void setHeaders() {
      headers.put(HttpHeaders.REFERER, this.homePage);
      headers.put(
         HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3"
      );
      headers.put(HttpHeaders.CACHE_CONTROL, "max-age=0");
      headers.put(HttpHeaders.CONNECTION, "keep-alive");
      headers.put(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
      headers.put(HttpHeaders.ACCEPT_ENCODING, "");
      headers.put("Upgrade-Insecure-Requests", "1");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-user", "?1");
      headers.put("sec-fetch-site", "none");
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   @Override
   protected Document fetch() {
      return Jsoup.parse(fetchPage(session.getOriginalURL(), this.dataFetcher, cookies, headers, session));
   }

   public static String fetchPage(String url, DataFetcher df, List<Cookie> cookies, Map<String, String> headers, Session session) {

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
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


         request.setHeaders(headers);
         content = new FetcherDataFetcher().get(session, request).getBody();
      }

      return content;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      // Json da pagina principal
      JSONObject apolloJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__APOLLO_STATE__ =", null, false, true);
      // Pega só o que interessa do json da api
      JSONObject infoProductJson = SaopauloB2WCrawlersUtils.assembleJsonProductWithNewWay(apolloJson);
      // verifying if url starts with home page because on crawler seed,
      // some seeds can be of another store
      if (infoProductJson.has("skus") && session.getOriginalURL().startsWith(this.homePage)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = this.crawlInternalPid(infoProductJson);
         CategoryCollection categories = crawlCategories(infoProductJson);
         String primaryImage = this.crawlPrimaryImage(infoProductJson);
         List<String> secondaryImages = this.crawlSecondaryImages(infoProductJson);
         String description = this.crawlDescription(apolloJson, doc, internalPid); //fix
         //    RatingsReviews ratingReviews = crawlRatingReviews(infoProductJson);
         List<String> eans = crawlEan(infoProductJson);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-title__Title-sc-1oqsqe9-0", true); // opt name

         JSONArray skuOptions = this.crawlSkuOptions(infoProductJson);
         for (int i = 0; i < skuOptions.length(); i++) {
            JSONObject skuJson = skuOptions.optJSONObject(i);
            String internalId = skuJson.optString("id");
            name = skuOptions.length() > 1 || name == null ? skuJson.optString("name") : name;
            Offers offers = scrapOffers(doc, internalId, internalPid, i);

            setMainRetailer(offers);

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setOffers(offers)
               //      .setRatingReviews(ratingReviews)
               .setEans(eans)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private void setMainRetailer(Offers offers) {
      if (offers.containsSeller(MAIN_B2W_NAME_LOWER)) {
         Offer offer = offers.getSellerByName(MAIN_B2W_NAME_LOWER);
         offer.setIsMainRetailer(true);
      } else if (offers.containsSeller(MAIN_B2W_NAME_LOWER.toUpperCase())) {
         Offer offer = offers.getSellerByName(MAIN_B2W_NAME_LOWER.toUpperCase());
         offer.setIsMainRetailer(true);
      } else if (offers.containsSeller(sellerNameLower)) {
         Offer offer = offers.getSellerByName(sellerNameLower);
         offer.setIsMainRetailer(true);
      } else if (sellerNameLowerFromHTML != null && offers.containsSeller(sellerNameLowerFromHTML)) {
         Offer offer = offers.getSellerByName(sellerNameLowerFromHTML);
         offer.setIsMainRetailer(true);
      } else {
         for (String seller : subSellers) {
            if (offers.containsSeller(seller)) {
               Offer offer = offers.getSellerByName(seller);
               offer.setIsMainRetailer(true);
               break;
            } else if (offers.containsSeller(seller.toLowerCase())) {
               Offer offer = offers.getSellerByName(seller.toLowerCase());
               offer.setIsMainRetailer(true);
               break;
            } else if (offers.containsSeller(seller.toUpperCase())) {
               Offer offer = offers.getSellerByName(seller.toUpperCase());
               offer.setIsMainRetailer(true);
               break;
            }
         }
      }
   }

   /**
    * Crawl rating and reviews stats using the bazaar voice endpoint. To get only the stats summary we
    * need at first, we only have to do one request. If we want to get detailed information about each
    * review, we must perform pagination.
    * <p>
    * The RatingReviews crawled in this method, is the same across all skus variations in a page.
    *
    * @param
    * @return
    */
   protected RatingsReviews crawlRatingReviews(JSONObject productJson) {
      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());
      JSONObject ratingInfo = productJson.optJSONObject("rating");
      JSONObject reviewStatistics = ratingInfo.optJSONObject("review");
      AdvancedRatingReview advancedRatingReview = getTotalStarsFromEachValue(reviewStatistics);

      Integer totalRating = getTotalReviewCount(reviewStatistics);

      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setTotalRating(totalRating);
      ratingReviews.setTotalWrittenReviews(totalRating);
      ratingReviews.setAverageOverallRating(getAverageOverallRating(reviewStatistics));

      return ratingReviews;
   }

   private AdvancedRatingReview getTotalStarsFromEachValue(JSONObject reviewStatistics) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      if (reviewStatistics.has("RatingDistribution")) {
         JSONArray ratingDistribution = reviewStatistics.getJSONArray("RatingDistribution");
         for (Object object : ratingDistribution) {
            JSONObject rating = (JSONObject) object;
            Integer option = CrawlerUtils.getIntegerValueFromJSON(rating, "RatingValue", 0);

            if (rating.has("RatingValue") && option == 1 && rating.has("Count")) {
               star1 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
            }

            if (rating.has("RatingValue") && option == 2 && rating.has("Count")) {
               star2 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
            }

            if (rating.has("RatingValue") && option == 3 && rating.has("Count")) {
               star3 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
            }

            if (rating.has("RatingValue") && option == 4 && rating.has("Count")) {
               star4 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
            }

            if (rating.has("RatingValue") && option == 5 && rating.has("Count")) {
               star5 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
            }

         }
      }

      return new AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build();
   }

   private Integer getTotalReviewCount(JSONObject reviewStatistics) {
      Integer totalReviewCount = null;
      if (reviewStatistics.has("TotalReviewCount")) {
         totalReviewCount = reviewStatistics.getInt("TotalReviewCount");
      }
      return totalReviewCount;
   }

   private Double getAverageOverallRating(JSONObject reviewStatistics) {
      Double avgOverallRating = null;
      if (reviewStatistics.has("AverageOverallRating")) {
         avgOverallRating = reviewStatistics.getDouble("AverageOverallRating");
      }
      return avgOverallRating;
   }

   /**
    * e.g: http://api.bazaarvoice.com/data/reviews.json?apiversion=5.4
    * &passkey=oqu6lchjs2mb5jp55bl55ov0d &Offset=0 &Limit=5 &Sort=SubmissionTime:desc
    * &Filter=ProductId:113048617 &Include=Products &Stats=Reviews
    * <p>
    * Endpoint request parameters:
    * <p>
    * &passKey: the password used to request the bazaar voice endpoint. This pass key e crawled inside
    * the html of the sku page, inside a script tag. More details on how to crawl this passKey
    * </p>
    * <p>
    * &Offset: the number of the chunk of data retrieved by the endpoint. If we want the second chunk,
    * we must add this value by the &Limit parameter.
    * </p>
    * <p>
    * &Limit: the number of reviews that a request will return, at maximum.
    * </p>
    * <p>
    * The others parameters we left as default.
    * <p>
    * Request Method: GET
    */
   private String assembleBazaarVoiceEndpointRequest(String skuInternalPid, String bazaarVoiceEnpointPassKey, Integer offset, Integer limit) {

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
    * Crawl the bazaar voice endpoint passKey on the sku page. The passKey is located inside a script
    * tag, which contains a json object is several metadata, including the passKey.
    *
    * @param embeddedJSONObject
    * @return
    */
   private String crawlBazaarVoiceEndpointPassKey(JSONObject embeddedJSONObject) {
      String passKey = null;
      if (embeddedJSONObject != null) {
         if (embeddedJSONObject.has("configuration")) {
            JSONObject configuration = embeddedJSONObject.getJSONObject("configuration");

            if (configuration.has("bazaarvoicePasskey")) {
               passKey = configuration.getString("bazaarvoicePasskey");
            }
         }
      }
      return passKey;
   }

   private JSONObject getReviewStatisticsJSON(JSONObject ratingReviewsEndpointResponse, String skuInternalPid) {
      if (ratingReviewsEndpointResponse.has("Includes")) {
         JSONObject includes = ratingReviewsEndpointResponse.getJSONObject("Includes");

         if (includes.has("Products")) {
            JSONObject products = includes.getJSONObject("Products");

            if (products.has(skuInternalPid)) {
               JSONObject product = products.getJSONObject(skuInternalPid);

               if (product.has("ReviewStatistics")) {
                  return product.getJSONObject("ReviewStatistics");
               }
            }
         }
      }

      return new JSONObject();
   }

   private List<String> crawlEan(JSONObject infoProductJson) {
      List<String> eans = new ArrayList<>();
      if (infoProductJson.has("skus")) {
         JSONArray skusArray = infoProductJson.getJSONArray("skus");
         for (Object object : skusArray) {
            JSONObject skus = (JSONObject) object;

            if (skus.has("eans")) {
               JSONArray eansArray = skus.getJSONArray("eans");

               for (Object eansObject : eansArray) {
                  if (eansObject instanceof String) {
                     String ean = (String) eansObject;
                     eans.add(ean);
                  }
               }
            }
         }
      }

      return eans;
   }

   private Offers scrapOffersnewWay(JSONObject skuJson, JSONObject offersJson, String sku) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      JSONArray offersArray = offersJson.has(sku) ? offersJson.getJSONArray(sku) : skuJson.optJSONArray("offers");

      boolean twoPositions = false;

      if (offersArray != null && !offersArray.isEmpty()) {
         Map<String, Double> mapOfSellerIdAndPrice = new HashMap<>();

         for (int i = 0; i < offersArray.length(); i++) {
            JSONObject info = (JSONObject) offersArray.get(i);

            boolean isBuyBox = offersArray.length() > 1;
            String sellerName = info.optQuery("/seller/name").toString().trim();
            String sellerId = info.optQuery("/seller/id").toString();

            Integer mainPagePosition = i == 0 ? 1 : null;
            Integer sellersPagePosition = i == 0 ? 1 : null;

            if (i > 0 && sellerName.equalsIgnoreCase("b2w")) {
               sellersPagePosition = 2;
               twoPositions = true;
            }

            JSONObject pricesJson = info.has("defaultPrice") ? info : SaopauloB2WCrawlersUtils.extractPricesJson(info);

            Pricing pricing = scrapPricing(pricesJson, i, sellerId, mapOfSellerIdAndPrice, true);

            Offer offer = OfferBuilder.create()
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

         if (offers.size() > 1) {
            // Sellers page positios is order by price, in this map, price is the value
            Map<String, Double> sortedMap = sortMapByValue(mapOfSellerIdAndPrice);
            int position = 1;

            for (Entry<String, Double> entry : sortedMap.entrySet()) {
               for (Offer offer : offers.getOffersList()) {
                  if (offer.getInternalSellerId().equals(entry.getKey())) {
                     offer.setSellersPagePosition(position);
                     position++;
                  }
               }
            }
         }
      }

      return offers;
   }


   public Document accessOffersPage(String offersPageURL) {
      return Jsoup.parse(fetchPage(offersPageURL, this.dataFetcher, cookies, headers, session));
   }

   protected String scrapSellerIdFromURL(String rawSellerId) {
      String sellerId = "";
      if (rawSellerId != null) {
         sellerId = CommonMethods.getLast(rawSellerId.split("sellerId")).replaceAll("[^0-9]", "").trim();
      }
      return sellerId;
   }


   protected Offers scrapOffers(Document doc, String internalId, String internalPid, int arrayPosition) throws MalformedPricingException, OfferException {

      Offers offers = new Offers();
      String offersPageUrl = urlPageOffers + internalPid + "?productSku=" + internalId;
      Document sellersDoc = accessOffersPage(offersPageUrl);
      Elements sellersFromHTML = sellersDoc.select(listSelectors.get("offers"));

      if (!sellersFromHTML.isEmpty()) {

         setOffersForSellersPage(offers, sellersFromHTML, listSelectors, sellersDoc);

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

   protected void scrapAndSetInfoForMainPage(Document doc, Offers offers, String internalId, String internalPid, int arrayPosition) throws OfferException, MalformedPricingException {
      JSONObject jsonSeller = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__APOLLO_STATE__ =", null, false, true);
      setOffersForMainPageSeller(offers, jsonSeller);
   }

   private void setOffersForMainPageSeller(Offers offers, JSONObject jsonSeller) throws OfferException, MalformedPricingException {
      Map<String, Double> mapOfSellerIdAndPrice = new HashMap<>();
      JSONObject offersJson = getJson(jsonSeller, "OffersResult");

      String keySeller = JSONUtils.getValueRecursive(offersJson, "seller.__ref", String.class);

      JSONObject jsonInfoSeller = jsonSeller.optJSONObject(keySeller);
      String name = jsonInfoSeller.optString("name");
      String internalSellerId = jsonInfoSeller.optString("id");

      Pricing pricing = scrapPricing(offersJson, 1, internalSellerId, mapOfSellerIdAndPrice, false);

      Offer offer = Offer.OfferBuilder.create()
         .setInternalSellerId(internalSellerId)
         .setSellerFullName(name)
         .setMainPagePosition(1)
         .setSellersPagePosition(1)
         .setPricing(pricing)
         .setIsBuybox(false)
         .setIsMainRetailer(false)
         .build();

      offers.add(offer);

   }

   protected void setOffersForSellersPage(Offers offers, Elements sellers, Map<String, String> listSelectors, Document sellersDoc) throws MalformedPricingException, OfferException {

      if (sellers.size() > 0) {

         for (int i = 0; i < sellers.size(); i++) {
            Element sellerInfo = sellers.get(i);
            boolean isBuyBox = sellers.size() > 1;
            String sellerName = CrawlerUtils.scrapStringSimpleInfo(sellerInfo, listSelectors.get("selectorSellerName"), false);
            String rawSellerId = CrawlerUtils.scrapStringSimpleInfoByAttribute(sellerInfo, listSelectors.get("selectorSellerId"), "href");
            String sellerId = scrapSellerIdFromURL(rawSellerId);
            if (sellers.size() == 1 && sellerId == null) {
               JSONObject jsonSeller = CrawlerUtils.selectJsonFromHtml(sellersDoc, "script", "window.__APOLLO_STATE__ =", null, false, true);
               JSONObject offersJson = getJson(jsonSeller, "OffersResult");
               String keySeller = JSONUtils.getValueRecursive(offersJson, "seller.__ref", String.class);
               JSONObject jsonInfoSeller = jsonSeller.optJSONObject(keySeller);
               sellerId = jsonInfoSeller.optString("id");
            }
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

   protected Pricing scrapPricingForOffersPage(Element sellerInfo)
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

   protected CreditCards scrapCreditCardsForSellersPage(Element sellerInfo, Double spotlightPrice) throws MalformedPricingException {
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

   protected Pricing scrapPricing(JSONObject info, int offerIndex, String internalSellerId, Map<String, Double> mapOfSellerIdAndPrice, boolean newWay)
      throws MalformedPricingException {

      JSONObject paymentOptions = SaopauloB2WCrawlersUtils.getJson(info, "paymentOptions");
      JSONArray installmentMin = SaopauloB2WCrawlersUtils.getJsonArrayInstallment(paymentOptions);
      Double priceFrom = scrapPriceFrom(info);
      CreditCards creditCards = scrapCreditCards(paymentOptions);
      Double spotlightPrice = JSONUtils.getValueRecursive(installmentMin, "0.total", Double.class);
      BankSlip bt = scrapBankTicket(info);

      if (priceFrom != null) {
         mapOfSellerIdAndPrice.put(internalSellerId, priceFrom);
      } else {
         mapOfSellerIdAndPrice.put(internalSellerId, spotlightPrice);
      }

      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bt)
         .build();
   }

   private Double scrapPriceFrom(JSONObject info) {
      return JSONUtils.getDoubleValueFromJSON(info, "salesPrice", false);
   }

   protected BankSlip scrapBankTicket(JSONObject info) throws MalformedPricingException {

      if (info.has("bankSlip")) {
         return BankSlipBuilder.create()
            .setFinalPrice(JSONUtils.getDoubleValueFromJSON(info, "bankSlip", true))
            .setOnPageDiscount(JSONUtils.getDoubleValueFromJSON(info, "bankSlipDiscount", true))
            .build();
      } else {
         return BankSlipBuilder.create()
            .setFinalPrice(JSONUtils.getDoubleValueFromJSON(info, "defaultPrice", true))
            .build();
      }
   }

   private Double scrapSpotlightPrice(JSONObject info, CreditCards creditCards, int offerIndex, boolean newWay) {
      Double featuredPrice = null;

      if (!newWay || offerIndex == 0) {

         Double spotlightPrice = info.optDouble("salesPrice");

         if (spotlightPrice != null && !spotlightPrice.isNaN()) {
            featuredPrice = spotlightPrice;

            return featuredPrice;
         }

         Double price1x = creditCards.getCreditCard(DEFAULT_CARD.toString()).getInstallments().getInstallmentPrice(1);
         Double bankTicket = CrawlerUtils.getDoubleValueFromJSON(info, "bakTicket", true, false);
         Double defaultPrice = CrawlerUtils.getDoubleValueFromJSON(info, "defaultPrice", true, false);


         if (offerIndex + 1 <= 3) {
            for (Double value : Arrays.asList(price1x, bankTicket, defaultPrice)) {
               if (featuredPrice == null || (value != null && value < featuredPrice)) {
                  featuredPrice = value;
               }
            }
         } else {

            if (defaultPrice != null) {
               featuredPrice = defaultPrice;
            } else if (price1x != null) {
               featuredPrice = price1x;
            } else if (bankTicket != null) {
               featuredPrice = bankTicket;
            }
         }
      } else {
         featuredPrice = info.optDouble("defaultPrice");
      }

      return featuredPrice;
   }

   /**
    * Sort map by Value
    *
    * @param map
    * @return
    */
   protected Map<String, Double> sortMapByValue(final Map<String, Double> map) {
      return map.entrySet()
         .stream()
         .sorted(Map.Entry.comparingByValue())
         .collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
               LinkedHashMap::new));
   }

   /*******************************
    * Product page identification *
    *******************************/

   private String crawlInternalPid(JSONObject assembleJsonProduct) {
      String internalPid = null;

      if (assembleJsonProduct.has("internalPid")) {
         internalPid = assembleJsonProduct.getString("internalPid").trim();
      }

      return internalPid;
   }

   private JSONArray crawlSkuOptions(JSONObject infoProductJson) {
      JSONArray skuMap = new JSONArray();

      if (infoProductJson.has("skus")) {
         JSONArray skus = infoProductJson.getJSONArray("skus");

         for (int i = 0; i < skus.length(); i++) {
            JSONObject sku = skus.getJSONObject(i);

            if (sku.has("internalId")) {
               JSONObject skuJson = new JSONObject();

               String internalId = sku.getString("internalId");
               StringBuilder name = new StringBuilder();

               String variationName = "";
               if (sku.has("variationName")) {
                  variationName = sku.getString("variationName");
               }

               String varationNameWithoutVolts = variationName.replace("volts", "").trim();

               name.append(sku.getString("name"));

               if (!name.toString().toLowerCase().contains(varationNameWithoutVolts.toLowerCase())) {
                  name.append(" " + variationName);
               }

               skuJson.put("name", name.toString());
               skuJson.put("id", internalId);

               if (sku.has("offers")) {
                  skuJson.put("offers", sku.optJSONArray("offers"));
               }

               skuMap.put(skuJson);
            }
         }
      }
      return skuMap;
   }

   protected CreditCards scrapCreditCards(JSONObject paymentOptions) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      JSONObject installmentsObject = SaopauloB2WCrawlersUtils.getJsonArray(paymentOptions);

      if (installmentsObject.has("quantity") && installmentsObject.has("value")) {
         installments.add(scrapInstallment(installmentsObject));
      }

      for (String flag : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(flag)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }

   private Installment scrapInstallment(JSONObject installmentJson) throws MalformedPricingException {
      Integer quantity = installmentJson.getInt("quantity");
      Double value = installmentJson.getDouble("value");
      Double interest = JSONUtils.getDoubleValueFromJSON(installmentJson, "taxedInterestRate", true);

      if (interest == null || interest == 0d) {
         interest = JSONUtils.getDoubleValueFromJSON(installmentJson, "interestRate", true);
      }

      Double finalPrice = JSONUtils.getDoubleValueFromJSON(installmentJson, "total", true);

      JSONObject discountJson = installmentJson.optJSONObject("discount");
      Double discount = discountJson != null ? discountJson.optDouble("rate", 0d) / 100d : 0d;

      return InstallmentBuilder.create()
         .setInstallmentNumber(quantity)
         .setInstallmentPrice(value)
         .setFinalPrice(finalPrice)
         .setAmOnPageInterests(interest)
         .setOnPageDiscount(discount)
         .build();
   }

   /*******************
    * General methods *
    *******************/

   private String crawlPrimaryImage(JSONObject infoProductJson) {
      String primaryImage = null;

      if (infoProductJson.has("images")) {
         JSONObject images = infoProductJson.getJSONObject("images");

         if (images.has("primaryImage")) {
            primaryImage = images.getString("primaryImage");
         }
      }

      return primaryImage;
   }

   private List<String> crawlSecondaryImages(JSONObject infoProductJson) {
      List<String> secondaryImages = new ArrayList<>();

      JSONArray secondaryImagesArray = new JSONArray();

      if (infoProductJson.has("images")) {
         JSONObject images = infoProductJson.getJSONObject("images");

         if (images.has("secondaryImages")) {
            secondaryImagesArray = images.getJSONArray("secondaryImages");
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toList().stream().map(Object::toString).collect(Collectors.toList());
      }

      return secondaryImages;
   }

   /**
    * @param document
    * @return
    */
   private CategoryCollection crawlCategories(JSONObject document) {
      CategoryCollection categories = new CategoryCollection();

      JSONArray categoryList = document.getJSONArray("categories");

      for (int i = categoryList.length() - 1; i >= 0; i--) { // Invert the Loop since the categorys in the JSONArray come reversed
         String cat = (categoryList.getJSONObject(i).get("name")).toString();

         if (!cat.isEmpty()) {
            categories.add(cat);
         }
      }

      return categories;
   }

   protected String crawlDescription(JSONObject apolloJson, Document doc, String internalPid) {
      StringBuilder description = new StringBuilder();

      boolean alreadyCapturedHtmlSlide = false;

      Element datasheet = doc.selectFirst("#info-section");
      if (datasheet != null) {
         Element iframe = datasheet.selectFirst("iframe");

         if (iframe != null) {
            Document docDescriptionFrame = Jsoup.parse(fetchPage(iframe.attr("src"), dataFetcher, cookies, headers, session));
            if (docDescriptionFrame != null) {
               description.append(docDescriptionFrame.html());
            }
         }

         // https://www.shoptime.com.br/produto/8421276/mini-system-mx-hs6500-zd-bluetooth-e-funcao-karaoke-bivolt-preto-samsung
         // alreadyCapturedHtmlSlide as been moved here because of links like these.

         alreadyCapturedHtmlSlide = true;
         datasheet.select("iframe, h1.sc-hgHYgh").remove();
         description.append(datasheet.html().replace("hidden", ""));
      }

      if (internalPid != null) {
         Element desc2 = doc.select(".info-description-frame-inside").first();

         if (desc2 != null && !alreadyCapturedHtmlSlide) {
            String urlDesc2 = homePage + "product-description/acom/" + internalPid;
            Document docDescriptionFrame = Jsoup.parse(fetchPage(urlDesc2, dataFetcher, cookies, headers, session));
            if (docDescriptionFrame != null) {
               description.append(docDescriptionFrame.html());
            }
         }

         Element elementProductDetails = doc.select(".info-section").last();
         if (elementProductDetails != null) {
            elementProductDetails.select(".info-section-header.hidden-md.hidden-lg").remove();
            description.append(elementProductDetails.html());
         }
      }
      if (description.length() == 0) {
         Object apolloDescription = apolloJson.optQuery("/ROOT_QUERY/product({\"productId\":\"" + internalPid + "\"})/description/content");
         if (apolloDescription != null) {
            description.append((String) apolloDescription);
         }
      }

      return Normalizer.normalize(description.toString(), Normalizer.Form.NFD).replaceAll("[^\n\t\r\\p{Print}]", "");
   }

   public static JSONObject getJson(JSONObject jsonSeller, String type) {
      for (Iterator<String> it = jsonSeller.keys(); it.hasNext(); ) {
         String key = it.next();
         if (key.contains(type)) {
            return jsonSeller.optJSONObject(key);
         }
      }
      return new JSONObject();
   }

}
