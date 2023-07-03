package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import models.pricing.Pricing.PricingBuilder;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class BrasilMagazineluizaCrawler extends Crawler {

   private static final String SELLER_NAME = "magalu";
   private static final String SELLER_NAME_1 = "magazine luiza";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString(), Card.ELO.toString(), Card.AURA.name());

   public BrasilMagazineluizaCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetch() {
      Map<String, String> headers = new HashMap<>();
      Document doc;
      int attempts = 0;

      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36");
      Response response;

      do {
         Request request = Request.RequestBuilder.create()
            .setUrl(session.getOriginalURL())
            .setProxyservice(Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.BUY,
               ProxyCollection.SMART_PROXY_BR_HAPROXY
            ))
            .setHeaders(headers)
            .build();

         response = this.dataFetcher.get(session, request);
         doc = Jsoup.parse(response.getBody());

         attempts++;

         if (attempts == 3) {
            if (isBlockedPage(doc, response.getLastStatusCode())) {
               Logging.printLogInfo(logger, session, "Blocked after 3 retries.");
            }
            break;
         }
      }
      while (isBlockedPage(doc, response.getLastStatusCode()));

      return doc;
   }

   private boolean isBlockedPage(Document doc, int statusCode) {
      return doc.toString().contains("We are sorry") || statusCode != 200;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();
      JSONObject skuJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, "", false, false);
      JSONObject json = JSONUtils.getValueRecursive(skuJson, "props.pageProps.data.product", JSONObject.class, new JSONObject());
      JSONArray variations = JSONUtils.getValueRecursive(json, "variations", JSONArray.class, new JSONArray());
      String internalPid = crawlInternalPidNewLayout(json);

      if (isProductPage(doc)) {

         if (variations.length() > 0) {

            String previousProductUrl = "";

            for (int i = 0; i < variations.length(); i++) {
               String productUrl = "https://www.magazineluiza.com.br/" + JSONUtils.getValueRecursive(json, "variations." + i + ".path", String.class, "");
               if (!productUrl.equals(previousProductUrl)) {

                  previousProductUrl = productUrl;
                  doc = fetchNewDocument(productUrl);
                  skuJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, "", false, false);
                  json = JSONUtils.getValueRecursive(skuJson, "props.pageProps.data.product", JSONObject.class, new JSONObject());

                  String reference = json.optString("reference");
                  String internalId = JSONUtils.getValueRecursive(json, "variations." + i + ".id", String.class, "");
                  String valueName = JSONUtils.getValueRecursive(json, "variations." + i + ".value", String.class, "");
                  String name = json.optString("title") + " " + (reference != null && !reference.equals("") ? " - " + reference : "") + (!checkIfNameHasValueName(valueName, reference) ? " " + valueName : "");
                  CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "div[data-testid=\"breadcrumb-item-list\"] a span", true);
                  String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("section[style='grid-area:maincontent']"));
                  List<String> imagesVariations = getSecondaryImagesVariations(json, i);
                  String primaryImage = !imagesVariations.isEmpty() ? imagesVariations.remove(0) : null;
                  boolean availableToBuy = json.optBoolean("available");
                  Offers offers = availableToBuy ? scrapOffersNewLayout(json) : new Offers();
                  RatingsReviews ratingsReviews = scrapRatingsReviews(json);

                  // Creating the product
                  Product product = ProductBuilder.create()
                     .setUrl(productUrl)
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setName(name)
                     .setCategories(categories)
                     .setPrimaryImage(primaryImage)
                     .setSecondaryImages(imagesVariations)
                     .setDescription(description)
                     .setRatingReviews(ratingsReviews)
                     .setOffers(offers)
                     .build();

                  products.add(product);
               }
            }
         } else {
            String internalId = crawlInternalPidNewLayout(json);
            String reference = json.optString("reference");
            String name = json.optString("title") + (reference != null && !reference.equals("") ? " - " + reference : "") + crawVariationName(json);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "div[data-testid=\"breadcrumb-item-list\"] a span", true);
            String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("section[style='grid-area:maincontent']"));
            List<String> images = crawlImagesNewLayout(skuJson, json);
            String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
            boolean availableToBuy = json.optBoolean("available");
            Offers offers = availableToBuy ? scrapOffersNewLayout(json) : new Offers();
            RatingsReviews ratingsReviews = scrapRatingsReviews(json);


            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategories(categories)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setRatingReviews(ratingsReviews)
               .setOffers(offers)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private boolean checkIfNameHasValueName(String value, String name) {
      String valueStripAccents = StringUtils.stripAccents(value);
      String nameStripAccents = StringUtils.stripAccents(name);
      return nameStripAccents.toLowerCase(Locale.ROOT).contains(valueStripAccents.toLowerCase(Locale.ROOT));
   }

   private Document fetchNewDocument(String productUrl) {
      Document doc;
      HttpResponse<String> response;
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(productUrl))
            .build();
         response = client.send(request, HttpResponse.BodyHandlers.ofString());
         doc = Jsoup.parse(response.body());
      } catch (Exception e) {
         throw new RuntimeException("Failed in load document: " + productUrl, e);
      }

      return doc;
   }

   private boolean isProductPage(Document doc) {
      return doc.select("div.wrapper-product__content").first() != null || doc.select("div[data-testid='mod-mediagallery']").first() != null;
   }


   private String crawVariationName(JSONObject json) {
      String name = JSONUtils.getValueRecursive(json, "attributes.0.values", String.class);
      return name != null ? " " + name : "";
   }

   private List<String> crawlImagesNewLayout(JSONObject skuJsonInfo, JSONObject json) {
      JSONArray components = JSONUtils.getValueRecursive(skuJsonInfo, "props.pageProps.structure.components", ".", JSONArray.class, new JSONArray());
      String imgWidth = "800";
      String imgHeight = "560";

      for (Object component : components) {
         String name = JSONUtils.getValueRecursive(component, "components.0.name", String.class, "");
         if (name.equals("MediaGallery")) {
            imgWidth = JSONUtils.getValueRecursive(component, "components.0.static.imgWidth", String.class, "800");
            imgHeight = JSONUtils.getValueRecursive(component, "components.0.static.imgHeight", String.class, "560");
            break;
         }
      }

      List<String> images = JSONUtils.jsonArrayToStringList(JSONUtils.getValueRecursive(json, "media.images", ".", JSONArray.class, new JSONArray()));
      List<String> imagesWithSize = new ArrayList<>();

      for (String image : images) {
         imagesWithSize.add(image.replace("{w}", imgWidth).replace("{h}", imgHeight));
      }
      return imagesWithSize;
   }

   private Offers scrapOffersNewLayout(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      String sellerFullName = JSONUtils.getValueRecursive(json, "seller.id", String.class);

      boolean isMainRetailer = sellerFullName.equalsIgnoreCase(SELLER_NAME) || sellerFullName.equalsIgnoreCase(SELLER_NAME_1.replace(" ", ""));
      Pricing pricing = scrapPricingNewLayout(json);
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerFullName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(isMainRetailer)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private String crawlInternalPidNewLayout(JSONObject json) {
      String internalPid = json.optString("variationId");
      if (internalPid == null || internalPid.isEmpty()) {
         internalPid = json.optString("id");
      }

      return internalPid;

   }

   private Pricing scrapPricingNewLayout(JSONObject json) throws MalformedPricingException {
      JSONObject price = json.optJSONObject("price");
      if (price == null) {
         throw new MalformedPricingException("Price is null");
      }

      Double priceFrom = price.optDouble("price", 0.0);
      Double spotlightPrice = price.optDouble("bestPrice", 0.0);

      CreditCards creditCards = scrapCreditCards(json, spotlightPrice);
      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();

   }

   private CreditCards scrapCreditCards(JSONObject json, Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Set<Installment> instsSet = new HashSet<>();
      JSONArray installmentsJson = JSONUtils.getJSONArrayValue(json, "paymentMethods");

      for (Object obj : installmentsJson) {
         JSONObject installmentJson = (JSONObject) obj;
         if (!installmentJson.isEmpty()) {
            JSONArray installmentPlans = JSONUtils.getJSONArrayValue(installmentJson, "installmentPlans");
            for (Object o : installmentPlans) {
               JSONObject installmentObj = (JSONObject) o;
               Integer installmentNumber = installmentObj.optInt("installment", 0);
               Double installmentPrice = installmentObj.optDouble("installmentAmount");
               instsSet.add(Installment.InstallmentBuilder.create()
                  .setInstallmentPrice(installmentPrice)
                  .setInstallmentNumber(installmentNumber)
                  .setFinalPrice(installmentPrice * installmentNumber).build());
            }

         }
      }

      if (instsSet.isEmpty()) {
         instsSet.add(Installment.InstallmentBuilder.create()
            .setInstallmentPrice(price)
            .setInstallmentNumber(1)
            .setFinalPrice(price).build());
      }
      Installments installments = new Installments(instsSet);

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private RatingsReviews scrapRatingsReviews(JSONObject json) {
      RatingsReviews ratingsReviews = new RatingsReviews();

      JSONObject rating = JSONUtils.getJSONValue(json, "rating");

      ratingsReviews.setTotalRating(rating.optInt("count", 0));
      ratingsReviews.setAverageOverallRating(rating.optDouble("score", 0.0));
      ratingsReviews.setTotalWrittenReviews(rating.optInt("count", 0));

      return ratingsReviews;

   }

   private List<String> getSecondaryImagesVariations(JSONObject product, int i) {
      List<String> secondaryImages = new ArrayList<>();
      JSONArray images = JSONUtils.getValueRecursive(product, "variations." + i + ".media.images", JSONArray.class, new JSONArray());
      for (i = 0; i < images.length(); i++) {
         secondaryImages.add(images.optString(i).replace("{w}x{h}", "800x560"));
      }

      return secondaryImages;
   }

}
