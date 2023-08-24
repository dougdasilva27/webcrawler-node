package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

public abstract class CNOVANewCrawler extends Crawler {

   protected Map<String, String> headers = new HashMap<>();
   private static final Card DEFAULT_CARD = Card.VISA;
   protected String homePage;

   protected Set<String> cards = Sets.newHashSet(DEFAULT_CARD.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public CNOVANewCrawler(Session session) {
      super(session);
      super.dataFetcher = new JsoupDataFetcher();

      homePage = "https://www." + getStore() + ".com.br";
   }

   protected abstract String getStore();

   protected abstract String getInitials();

   protected abstract List<String> getSellerName();


   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   @Override
   protected Object fetch() {
      return Jsoup.parse(fetchPage(session.getOriginalURL()).getBody());
   }

   protected String encodeUrlPath(String url) {
      StringBuilder sb = new StringBuilder();

      try {
         URL u = new URL(url);
         String path = u.getPath();

         sb.append(u.getProtocol() + "://" + u.getHost());
         for (String subPath : path.split("/")) {
            if (subPath.isEmpty()) continue;

            sb.append("/" + URLEncoder.encode(subPath, StandardCharsets.UTF_8.toString()));
         }
      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
      }

      return sb.toString();
   }

   protected Response fetchPage(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("accept-encoding", "");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
      headers.put("cache-control", "no-cache");
      headers.put("pragma", "no-cache");
      headers.put("sec-fetch-dest", "document");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-site", "none");
      headers.put("sec-fetch-user", "?1");
      headers.put("upgrade-insecure-requests", "1");

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setSendUserAgent(true)
         .setHeaders(headers)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY
            )
         )
         .build();

      return CrawlerUtils.retryRequest(request, session, dataFetcher, true);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      // Json da pagina principal
      JSONObject loadJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, true);
      Object obj = loadJson.optQuery("/props/initialState/Product");

      JSONObject productJson = obj instanceof JSONObject ? (JSONObject) obj : new JSONObject();
      JSONObject infoProductJson = productJson.optJSONObject("product");
      JSONArray skus = infoProductJson != null ? infoProductJson.optJSONArray("skus") : null;

      if (skus != null && !skus.isEmpty() && session.getOriginalURL().startsWith(this.homePage)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = infoProductJson.optString("id");
         CategoryCollection categories = crawlCategories(infoProductJson);
         String description = crawlDescription(infoProductJson, productJson);
         String productName = infoProductJson.optString("name");
         RatingsReviews ratingReviews = crawlRatingReviews(internalPid);

         for (Object o : skus) {
            JSONObject skuJson = getSkuJson(productJson, (JSONObject) o);

            String name = skus.length() > 1 ? (productName + " " + skuJson.optString("name")).trim() : productName;
            String ean = skuJson.optString("ean");
            List<String> eans = ean.isEmpty() ? new ArrayList<>() : Arrays.asList(ean);

            String internalId = internalPid + "-" + skuJson.optString("id");
            List<String> images = scrapImages(skuJson);
            String primaryImage = images.isEmpty() ? null : images.remove(0);
            Offers offers = scrapOffers(skuJson.optString("id"));

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
               .setSecondaryImages(images)
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

   private JSONObject getSkuJson(JSONObject initialProductJson, JSONObject productSkuJson) {
      JSONObject skuJson = initialProductJson.optJSONObject("sku");
      String skuId = skuJson != null ? skuJson.optString("id") : "";

      if (!skuId.equals(productSkuJson.optString("id"))) {
         String url = session.getOriginalURL().replace(skuId, productSkuJson.optString("id"));

         Document doc = Jsoup.parse(fetchPage(url).getBody());
         JSONObject loadJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, true);
         Object obj = loadJson.optQuery("/props/initialState/Product/sku");

         if (obj instanceof JSONObject) {
            skuJson = (JSONObject) obj;


            skuJson.put("newUrl", url);
         }
      }

      return skuJson == null ? new JSONObject() : skuJson;
   }

   private List<String> scrapImages(JSONObject json) {
      List<String> imgsList = new ArrayList<>();

      JSONArray imgsJson = json.optJSONArray("zoomedImages");

      JSONArray resultList = new JSONArray();

      for (Object img : imgsJson) {
         JSONObject imgJson = (JSONObject) img;

         if (resultList.length() == 0) {
            resultList.put(imgJson);
         }

         if (resultList.toString().contains("\"order\":" + imgJson.optString("order"))) {
            continue;
         }

         resultList.put(imgJson);
      }

      resultList.forEach(el -> {
         if (el instanceof JSONObject) {
            imgsList.add(((JSONObject) el).optString("url"));
         }
      });

      return imgsList;
   }

   protected Offers scrapOffers(String internalId) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      List<String> sellersIdList = new ArrayList<>();
      String url = "https://pdp-api." + getStore() + ".com.br/api/v2/sku/" + internalId + "/price/source/" + getInitials() + "?&take=all";
      JSONObject offersJson = JSONUtils.stringToJson(fetchPage(url).getBody());
      JSONArray sellerInfo = offersJson.optJSONArray("sellers");

      if (sellerInfo != null && offersJson.optBoolean("buyButtonEnabled", false)) {
         // The Business logic is: if we have more than 1 seller is buy box
         boolean isBuyBox = sellerInfo.length() > 1;

         for (int i = 0; i < sellerInfo.length(); i++) {
            JSONObject info = (JSONObject) sellerInfo.get(i);

            if (info.has("name") && !info.isNull("name") && info.has("id") && !info.isNull("id")) {
               String name = info.optString("name");
               String internalSellerId = info.optString("id");

               if (internalSellerId != null) {
                  sellersIdList.add(internalSellerId);
               }

               Integer mainPagePosition = (i + 1) <= 3 ? i + 1 : null;
               Integer sellersPagePosition = i + 1;

               boolean isMainRetailer = false;

               for (String sellerName : getSellerName()) {
                  if (sellerName.equalsIgnoreCase(name)) {
                     isMainRetailer = true;
                  }
               }

               boolean principalSeller = info.optBoolean("elected", false);
               List<String> salesList = principalSeller ? scrapSales(offersJson) : new ArrayList<>();

               Pricing pricing = scrapPricing(offersJson, info, principalSeller);
               salesList.add(CrawlerUtils.calculateSales(pricing));

               if (pricing != null) {
                  Offer offer = OfferBuilder.create()
                     .setInternalSellerId(internalSellerId)
                     .setSellerFullName(name)
                     .setMainPagePosition(mainPagePosition)
                     .setSellersPagePosition(sellersPagePosition)
                     .setPricing(pricing)
                     .setIsBuybox(isBuyBox)
                     .setIsMainRetailer(isMainRetailer)
                     .setSales(salesList)
                     .build();

                  offers.add(offer);
               }
            }
         }
         Map<String, Double> scrapSallersIdAndRating = scrapSallersRating(sellersIdList);

         int position = 1;
         for (Map.Entry<String, Double> entry : scrapSallersIdAndRating.entrySet()) {
            for (Offer offer : offers.getOffersList()) {
               if (offer.getInternalSellerId().equals(entry.getKey())) {
                  offer.setSellersPagePosition(position);
                  position++;
               }
            }
         }
      }

      return offers;
   }

   protected List<String> scrapSales(JSONObject offersJson) {
      List<String> salesList = new ArrayList<>();

      JSONObject sellPrice = offersJson.optJSONObject("sellPrice");
      if (sellPrice != null && sellPrice.optBoolean("showPricePercentageVariation", false)) {
         String sale = sellPrice.optString("pricePercentageVariation");

         if (!sale.isEmpty()) {
            salesList.add("-" + sale + "%");
         }
      }

      return salesList;
   }

   protected Pricing scrapPricing(JSONObject offersJson, JSONObject info, boolean principalSeller)
      throws MalformedPricingException {

      if (principalSeller) {
         JSONObject sellPrice = offersJson.optJSONObject("sellPrice");

         if (sellPrice != null) {
            Double priceFrom = sellPrice.optBoolean("showPriceBefore", false) ? sellPrice.optDouble("priceBefore", 0d) : null;
            Double spotlightPrice = sellPrice.optDouble("priceValue");

            CreditCards creditCards = scrapCreditCards(offersJson);
            BankSlip bt = scrapBankTicket(offersJson, spotlightPrice);

            return PricingBuilder.create()
               .setPriceFrom(priceFrom != null && priceFrom > 0d ? priceFrom : null)
               .setSpotlightPrice(spotlightPrice)
               .setCreditCards(creditCards)
               .setBankSlip(bt)
               .build();
         }
      } else {
         Double price = info.optDouble("sellPrice", 0d);

         if (price > 0d) {
            Installments installments = new Installments();
            installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(price)
               .build());

            CreditCards creditCards = new CreditCards();
            for (String flag : cards) {
               creditCards.add(CreditCardBuilder.create()
                  .setBrand(flag)
                  .setIsShopCard(false)
                  .setInstallments(installments)
                  .build());
            }

            return PricingBuilder.create()
               .setSpotlightPrice(price)
               .setCreditCards(creditCards)
               .setBankSlip(CrawlerUtils.setBankSlipOffers(price, null))
               .build();
         }
      }

      return null;
   }

   private Double scrapSpecialDiscount(JSONObject offersJson, String option) {
      Double discount = null;

      JSONObject discountJson = offersJson.optJSONObject("paymentMethodDiscount");
      if (discountJson != null && discountJson.optBoolean("hasDiscount", false)) {
         String discountOptions = discountJson.optString("discountDescription").toLowerCase();

         if (discountOptions.contains(option)) {
            discount = MathUtils.normalizeTwoDecimalPlaces(MathUtils.parseDoubleWithComma(discountJson.optString("discount")
               .replace("%", "")) / 100d);
         }
      }

      return discount;
   }

   private BankSlip scrapBankTicket(JSONObject offersJson, Double spotlightPrice) throws MalformedPricingException {
      Double discount = scrapSpecialDiscount(offersJson, "boleto");
      Double bankPrice = spotlightPrice;

      if (discount != null) {
         Object o = offersJson.optQuery("/paymentMethodDiscount/sellPriceWithDiscount");

         if (o instanceof Double) {
            bankPrice = (Double) o;
         } else if (o instanceof Integer) {
            bankPrice = ((Integer) o).doubleValue();
         }
      }

      return BankSlipBuilder.create()
         .setFinalPrice(bankPrice)
         .setOnPageDiscount(discount)
         .build();
   }

   private CreditCards scrapCreditCards(JSONObject offersJson) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      JSONArray installmentsArray = offersJson.optJSONArray("installmentOptions");
      if (installmentsArray != null) {

         for (int i = 0; i < installmentsArray.length(); i++) {
            JSONObject installmentOption = installmentsArray.optJSONObject(i);

            JSONArray conditions = installmentOption.optJSONArray("conditions");
            if (conditions != null && !conditions.isEmpty()) {
               Installments installments = new Installments();

               for (int j = 0; j < conditions.length(); j++) {
                  JSONObject installment = conditions.optJSONObject(j);

                  if (installment != null && installment.has("qtyParcels") && installment.has("price")) {
                     installments.add(scrapInstallment(installment, offersJson));
                  } else {
                     installments.add(InstallmentBuilder.create()
                        .setInstallmentNumber(1)
                        .setInstallmentPrice(installment.optDouble("price", 0d))
                        .setFinalPrice(installment.optDouble("price", 0d))
                        .build());
                  }
               }

               if (!installmentOption.optString("type").equalsIgnoreCase("Bandeira")) {
                  for (String flag : cards) {
                     creditCards.add(CreditCardBuilder.create()
                        .setBrand(flag)
                        .setIsShopCard(false)
                        .setInstallments(installments)
                        .build());
                  }
               } else {
                  creditCards.add(CreditCardBuilder.create()
                     .setBrand(Card.SHOP_CARD.toString())
                     .setIsShopCard(true)
                     .setInstallments(installments)
                     .build());
               }
            }
         }

      }

      return creditCards;
   }

   private Installment scrapInstallment(JSONObject installmentJson, JSONObject offersJson) throws MalformedPricingException {
      Integer quantity = installmentJson.optInt("qtyParcels", 0);
      Double value = installmentJson.optDouble("price", 0d);
      Double interest = installmentJson.optDouble("monthlyInterest", 0d);

      Double finalPrice = JSONUtils.getDoubleValueFromJSON(installmentJson, "total", true);

      Double discount = scrapSpecialDiscount(offersJson, quantity + "x no Cartão");

      return InstallmentBuilder.create()
         .setInstallmentNumber(quantity)
         .setInstallmentPrice(value)
         .setFinalPrice(finalPrice)
         .setAmOnPageInterests(interest)
         .setOnPageDiscount(discount)
         .build();
   }

   /**
    * @param productJson
    * @return
    */
   private CategoryCollection crawlCategories(JSONObject productJson) {
      CategoryCollection categories = new CategoryCollection();

      JSONArray categoryList = productJson.getJSONArray("categories");

      for (Object o : categoryList) {
         JSONObject catJson = (JSONObject) o;
         String cat = catJson.optString("description");

         if (!cat.isEmpty()) {
            categories.add(cat);
         }
      }

      return categories;
   }

   protected String crawlDescription(JSONObject infoProductJson, JSONObject productJson) {
      StringBuilder description = new StringBuilder();

      description.append("<div id=\"desciption\">" + infoProductJson.optString("description") + "</div>");

      JSONArray specsGroup = infoProductJson.optJSONArray("specGroups");
      if (specsGroup != null && !specsGroup.isEmpty()) {
         description.append("<div id=\"specs\">");

         for (Object o : specsGroup) {
            JSONObject specGroup = (JSONObject) o;

            JSONArray rows = specGroup.optJSONArray("specs");
            if (rows != null && !rows.isEmpty()) {
               description.append("<table> <h4>" + specGroup.optString("name") + "</h4>");

               for (Object obj : rows) {
                  JSONObject row = (JSONObject) obj;
                  description.append("<tr>");
                  description.append("<td>" + row.optString("name") + "</td>");
                  description.append("<td>" + row.optString("value") + "</td>");
                  description.append("</tr>");
               }

               description.append("</table>");
            }
         }

         description.append("</div>");
      }

      Object id = productJson.optQuery("/sku/id");
      if (id != null) {
         String url = "https://www.pontofrio-imagens.com.br/html/conteudo-produto/73/" + id + "/" + id + ".html";

         Response response = fetchPage(url);
         if (response.getLastStatusCode() == 200) {
            description.append(fetchPage(url).getBody());
         }

      }

      return Normalizer.normalize(description.toString(), Normalizer.Form.NFD).replaceAll("[^\n\t\r\\p{Print}]", "");
   }

   /**
    * @param document
    * @return
    */
   private RatingsReviews crawlRatingReviews(String skuInternalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();

      String url = "https://pdp-api." + getStore() + ".com.br/api/v2/reviews/product/" + skuInternalPid + "/source/" + getInitials() + "?page=0&size=1&orderBy=DATE";
      JSONObject ratingReviewsEndpointResponse = JSONUtils.stringToJson(fetchPage(url).getBody());

      JSONObject review = ratingReviewsEndpointResponse.optJSONObject("review");

      if (review != null) {
         Integer totalRating = review.optInt("ratingQty");

         AdvancedRatingReview advancedRatingReview = getTotalStarsFromEachValue(review, totalRating);

         ratingReviews.setAdvancedRatingReview(advancedRatingReview);
         ratingReviews.setTotalRating(totalRating);
         ratingReviews.setTotalWrittenReviews(totalRating);
         ratingReviews.setAverageOverallRating(CrawlerUtils.extractRatingAverageFromAdvancedRatingReview(advancedRatingReview));
      }

      return ratingReviews;
   }

   private AdvancedRatingReview getTotalStarsFromEachValue(JSONObject reviewStatistics, int totalRating) {
      Map<Integer, Integer> starsMap = new HashMap<>();

      JSONArray ratingDistribution = reviewStatistics.optJSONArray("ratingComposition");

      if (ratingDistribution != null) {
         for (Object object : ratingDistribution) {
            JSONObject rating = (JSONObject) object;

            Integer option = rating.optInt("rating", 0);
            Double percentage = rating.optDouble("percentage", 0d);

            if (percentage != null) {
               int starValue = ((Double) MathUtils.normalizeNoDecimalPlaces(totalRating * (percentage / 100d))).intValue();

               starsMap.put(option, starValue);
            }
         }
      }

      return new AdvancedRatingReview.Builder().allStars(starsMap).build();
   }

   protected Map<String, Double> scrapSallersRating(List<String> sellersIdList) {
      Map<String, Double> SellerIdAndSellerRating = new HashMap<>();
      String sellersIds = sellersIdList.toString()
         .replaceAll("[^0-9 ]", "")
         .trim()
         .replace(" ", "%2C");

      String apiUrl = "https://pdp-api." + getStore() + ".com.br/api/v2/reviews/multiplereviews/source/" + getInitials() + "?sellersId=" + sellersIds;

      JSONArray sellersRatingInfo = JSONUtils.stringToJsonArray(fetchPage(apiUrl).getBody());

      if (sellersRatingInfo != null && sellersRatingInfo.length() > 0) {

         for (int i = 0; i < sellersRatingInfo.length(); i++) {
            JSONObject ratingInfo = sellersRatingInfo.optJSONObject(i);
            SellerIdAndSellerRating.put(ratingInfo.optString("sellerId"), ratingInfo.optDouble("rating"));
         }
      }

      return SellerIdAndSellerRating.entrySet()
         .stream()
         .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
         .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
   }

}
