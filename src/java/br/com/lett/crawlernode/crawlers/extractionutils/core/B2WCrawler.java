package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
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
   protected Map<String, String> listSelectors = getListSelectors();
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

   private Map<String, String> getListSelectors() {
      Map<String, String> listSelectors = new HashMap<>();
      listSelectors.put("selectorSellerName", "p[class^=\"sold-and-delivery__Seller-sc\"]");
      listSelectors.put("selectorSellerId", "a[class^=\"src__ButtonUI-sc\"]");
      listSelectors.put("offers", "div[class^=\"src__Divider\"]");
      listSelectors.put("hasPageOffers", "span[class^=\"more-offers__Text-sc\"]");

      return listSelectors;
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

      JSONObject apolloJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__APOLLO_STATE__ =", null, false, true);
      JSONObject infoProductJson = SaopauloB2WCrawlersUtils.assembleJsonProductWithNewWay(apolloJson);

      if (infoProductJson.has("skus") && session.getOriginalURL().startsWith(this.homePage)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = this.crawlInternalPid(infoProductJson);
         CategoryCollection categories = crawlCategories(infoProductJson);
         String primaryImage = this.crawlPrimaryImage(infoProductJson);
         List<String> secondaryImages = this.crawlSecondaryImages(infoProductJson);
         String description = this.crawlDescription(apolloJson, doc, internalPid);
         RatingsReviews ratingReviews = crawlRatingReviews(infoProductJson);
         List<String> eans = crawlEan(infoProductJson);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-title__Title-sc-1oqsqe9-0", true);

         JSONArray skuOptions = this.crawlSkuOptions(infoProductJson);
         for (int i = 0; i < skuOptions.length(); i++) {
            JSONObject skuJson = skuOptions.optJSONObject(i);
            String internalId = skuJson.optString("id");
            name = skuOptions.length() > 1 || name == null ? skuJson.optString("name") : name;
            boolean available = isAvailable(doc);
            Offers offers = available ? scrapOffers(doc, internalId, internalPid) : new Offers();

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
               .setRatingReviews(ratingReviews)
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

   protected RatingsReviews crawlRatingReviews(JSONObject productJson) {
      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());
      JSONObject ratingInfo = productJson.optJSONObject("rating");
      if (ratingInfo != null && !ratingInfo.isEmpty()) {
         JSONObject reviewStatistics = ratingInfo.optJSONObject("reviews");
         JSONObject ratingAverage = ratingInfo.optJSONObject("rating");
         AdvancedRatingReview advancedRatingReview = reviewStatistics != null ? getTotalStarsFromEachValue(reviewStatistics) : new AdvancedRatingReview();

         Integer totalRating = ratingAverage.optInt("reviews");

         ratingReviews.setAdvancedRatingReview(advancedRatingReview);
         ratingReviews.setTotalRating(totalRating);
         ratingReviews.setTotalWrittenReviews(totalRating);
         ratingReviews.setAverageOverallRating(ratingAverage.optDouble("average"));
      }
      return ratingReviews;
   }

   private AdvancedRatingReview getTotalStarsFromEachValue(JSONObject reviewStatistics) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      JSONArray ratingDistribution = reviewStatistics.optJSONArray("ratingDistribution");
      if (ratingDistribution != null) {
         for (Object object : ratingDistribution) {
            JSONObject rating = (JSONObject) object;
            Integer option = CrawlerUtils.getIntegerValueFromJSON(rating, "ratingValue", 0);

            if (rating.has("ratingValue") && option == 1 && rating.has("count")) {
               star1 = CrawlerUtils.getIntegerValueFromJSON(rating, "count", 0);
            }

            if (rating.has("ratingValue") && option == 2 && rating.has("count")) {
               star2 = CrawlerUtils.getIntegerValueFromJSON(rating, "count", 0);
            }

            if (rating.has("ratingValue") && option == 3 && rating.has("count")) {
               star3 = CrawlerUtils.getIntegerValueFromJSON(rating, "count", 0);
            }

            if (rating.has("ratingValue") && option == 4 && rating.has("count")) {
               star4 = CrawlerUtils.getIntegerValueFromJSON(rating, "count", 0);
            }

            if (rating.has("ratingValue") && option == 5 && rating.has("count")) {
               star5 = CrawlerUtils.getIntegerValueFromJSON(rating, "count", 0);
            }

         }
      }

      return new AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build();
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

   public Document accessOffersPage(String offersPageURL) {
      return Jsoup.parse(fetchPage(offersPageURL, this.dataFetcher, cookies, headers, session));
   }

   protected String scrapSellerIdFromURL(String rawSellerId) {
      String sellerId = null;
      if (rawSellerId != null) {
         sellerId = CommonMethods.getLast(rawSellerId.split("sellerId")).replaceAll("[^0-9]", "").trim();
      }
      return sellerId;
   }


   protected Offers scrapOffers(Document doc, String internalId, String internalPid) throws MalformedPricingException, OfferException {

      Offers offers = new Offers();
      Document sellersDoc = null;
      if (!doc.select(listSelectors.get("hasPageOffers")).isEmpty()) {
         String offersPageUrl = urlPageOffers + internalPid + "?productSku=" + internalId;
         sellersDoc = accessOffersPage(offersPageUrl);
      }

      Elements sellersFromHTML = sellersDoc != null ? sellersDoc.select(listSelectors.get("offers")) : null;

      if (sellersFromHTML != null && !sellersFromHTML.isEmpty()) {

         setOffersForSellersPage(offers, sellersFromHTML, listSelectors, sellersDoc);

      } else {

        /*
               caso sellersFromHTML seja vazio significa que fomos bloqueados
               durante a tentativa de capturar as informações na pagina de sellers
               ou que o produto em questão não possui pagina de sellers.
               Nesse caso devemos capturar apenas as informações da pagina principal.
               */

         scrapAndSetInfoForMainPage(doc, offers);

      }

      return offers;
   }

   protected void scrapAndSetInfoForMainPage(Document doc, Offers offers) throws OfferException, MalformedPricingException {
      JSONObject jsonSeller = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__APOLLO_STATE__ =", null, false, true);
      setOffersForMainPageSeller(offers, jsonSeller);
   }

   private boolean isAvailable(Document doc) {
      return doc.select("strong[class^=\"styles__Title-sc\"]").isEmpty();
   }

   private void setOffersForMainPageSeller(Offers offers, JSONObject jsonSeller) throws OfferException, MalformedPricingException {
      // in this case, get only seller from main page
      Map<String, Double> mapOfSellerIdAndPrice = new HashMap<>();
      JSONObject offersJson = SaopauloB2WCrawlersUtils.getJson(jsonSeller, "OffersResult");

      String keySeller = JSONUtils.getValueRecursive(offersJson, "seller.__ref", String.class);

      JSONObject jsonInfoSeller = jsonSeller.optJSONObject(keySeller);
      String name = jsonInfoSeller.optString("name");
      String internalSellerId = jsonInfoSeller.optString("id");

      Pricing pricing = scrapPricing(offersJson, internalSellerId, mapOfSellerIdAndPrice);

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
            // The Business logic is: if we have more than 1 seller is buy box
            boolean isBuyBox = sellers.size() > 1;
            String sellerName = CrawlerUtils.scrapStringSimpleInfo(sellerInfo, listSelectors.get("selectorSellerName"), false);
            String rawSellerId = CrawlerUtils.scrapStringSimpleInfoByAttribute(sellerInfo, listSelectors.get("selectorSellerId"), "href");
            String sellerId = scrapSellerIdFromURL(rawSellerId);
            if (sellers.size() == 1 && sellerId == null) {
               JSONObject jsonSeller = CrawlerUtils.selectJsonFromHtml(sellersDoc, "script", "window.__APOLLO_STATE__ =", null, false, true);
               JSONObject offersJson = SaopauloB2WCrawlersUtils.getJson(jsonSeller, "OffersResult");
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

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(sellerInfo, "div[class^=\"src__ListPrice\"] span", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(sellerInfo, ".priceSales", null, false, ',', session);
      BankSlip bt = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCardsForSellersPage(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bt)
         .build();
   }

   protected CreditCards scrapCreditCardsForSellersPage(Double spotlightPrice) throws MalformedPricingException {
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

   protected Pricing scrapPricing(JSONObject info, String internalSellerId, Map<String, Double> mapOfSellerIdAndPrice)
      throws MalformedPricingException {

      JSONObject paymentOptions = SaopauloB2WCrawlersUtils.getJson(info, "paymentOptions");
      JSONArray installmentMin = SaopauloB2WCrawlersUtils.getJsonArrayInstallment(paymentOptions);
      Double priceFrom = scrapPriceFrom(info);
      Double spotlightPrice = JSONUtils.getValueRecursive(installmentMin, "0.total", Double.class);
      if (spotlightPrice == null) {
         Integer priceInt = JSONUtils.getValueRecursive(installmentMin, "0.total", Integer.class);
         spotlightPrice = priceInt != null ? Double.valueOf(priceInt) : null;
      }
      CreditCards creditCards = scrapCreditCards(paymentOptions, spotlightPrice);

      if (priceFrom != null) {
         mapOfSellerIdAndPrice.put(internalSellerId, priceFrom);
      } else {
         mapOfSellerIdAndPrice.put(internalSellerId, spotlightPrice);
      }

      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private Double scrapPriceFrom(JSONObject info) {
      return JSONUtils.getDoubleValueFromJSON(info, "salesPrice", false);
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

   protected CreditCards scrapCreditCards(JSONObject paymentOptions, Double spotlightPrice) throws MalformedPricingException {
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

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());


      return creditCards;
   }

   private Installment scrapInstallment(JSONObject installmentJson) throws MalformedPricingException {
      Integer quantity = installmentJson.optInt("quantity");
      Double value = installmentJson.optDouble("value");
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

     Element datasheet = doc.selectFirst("[class*=\"info-drawer__Description\"]");

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


}
