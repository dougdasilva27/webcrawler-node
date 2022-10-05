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
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilMagazineluizaCrawler extends Crawler {

   private static final String SELLER_NAME = "magalu";
   private static final String SELLER_NAME_1 = "magazine luiza";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilMagazineluizaCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetch() {
      Map<String, String> headers = new HashMap<>();
      Document doc = new Document(session.getOriginalURL());
      int attempts = 0;

      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36");
      Response response;

      do {
         Request request = Request.RequestBuilder.create()
            .setUrl(session.getOriginalURL())
            .setProxyservice(Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.SMART_PROXY_BR,
               ProxyCollection.SMART_PROXY_CO,
               ProxyCollection.SMART_PROXY_MX,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY
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
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         products.add(crawlProduct(doc));
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.select("div.wrapper-product__content").first() != null || doc.select("div[data-testid='mod-mediagallery']").first() != null;
   }

   public Product crawlProductFromDefaultLayout(JSONObject skuJsonInfo, Document doc) throws Exception {

      String internalId = crawlInternalId(skuJsonInfo);
      String frontPageName = crawlNameFrontPage(doc, internalId);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      List<String> secondaryImages = crawlSecondaryImages(doc, primaryImage);
      boolean availableToBuy = !doc.select(".button__buy-product-detail").isEmpty();
      Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();
      String description = crawlDescription(doc, internalId);
      RatingsReviews ratingsReviews = scrapRatingsAlternativeWay(doc);

      // Creating the product
      return ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(internalId)
         .setInternalPid(internalId)
         .setName(frontPageName)
         .setCategories(categories)
         .setPrimaryImage(primaryImage)
         .setSecondaryImages(secondaryImages)
         .setDescription(description)
         .setRatingReviews(ratingsReviews)
         .setOffers(offers)
         .build();
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      String sellerFullName = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".seller__indentifier meta", "content");
      boolean isMainRetailer = sellerFullName.equalsIgnoreCase(SELLER_NAME) || sellerFullName.equalsIgnoreCase(SELLER_NAME_1);
      Pricing pricing = scrapPricing(doc);

      offers.add(OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerFullName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(isMainRetailer)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".information-values__product-page .price-template__from", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".information-values__product-page .price-template__text", null, false, ',', session);

      CreditCards creditCards = scrapCreditCardsFromProductPage(doc, spotlightPrice);
      BankSlip bankSlip = scrapBankslip(doc, spotlightPrice);

      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private BankSlip scrapBankslip(Document doc, Double spotlightPrice) throws MalformedPricingException {
      Double bkPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".method-payment__topic-title .method-payment__parcel", null, true, ',', session);
      Double percentage = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".method-payment__topic-title .method-payment__parcel .method-payment__discount-text", null, true, ',', session);
      ;
      Double discount = percentage != null ? percentage / 100d : 0d;

      if (bkPrice == null) {
         bkPrice = spotlightPrice;
      }

      return BankSlipBuilder.create()
         .setFinalPrice(bkPrice)
         .setOnPageDiscount(discount)
         .build();
   }

   private CreditCards scrapCreditCardsFromProductPage(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments regularCard = scrapInstallments(doc, ".method-payment__card-box .method-payment__values--general-cards li > p");
      if (regularCard.getInstallments().isEmpty()) {
         regularCard.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(regularCard)
            .build());
      }

      Installments shopCard = scrapInstallments(doc, ".method-payment__card-luiza-box ul[class^=method-payment__values--] li > p");

      if (shopCard.getInstallments().isEmpty()) {
         shopCard = regularCard;
      }

      creditCards.add(CreditCardBuilder.create()
         .setBrand(Card.SHOP_CARD.toString())
         .setIsShopCard(true)
         .setInstallments(shopCard)
         .build());

      return creditCards;
   }

   private Installments scrapInstallments(Document doc, String selector) throws MalformedPricingException {
      Installments installments = new Installments();

      Elements normalCards = doc.select(selector);
      for (Element e : normalCards) {
         Double percentage = CrawlerUtils.scrapDoublePriceFromHtml(e, ".method-payment__discount-text", null, true, ',', session);
         ;
         Double discount = percentage != null ? percentage / 100d : 0d;
         Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, true);

         if (!pair.isAnyValueNull()) {
            installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(pair.getFirst())
               .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
               .setOnPageDiscount(discount)
               .build());
         } else if (!e.ownText().contains("x")) {
            Double price = MathUtils.parseDoubleWithComma(e.ownText());
            installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(price)
               .setFinalPrice(price)
               .setOnPageDiscount(discount)
               .build());
         }
      }

      return installments;
   }

   private String crawlInternalId(JSONObject skuJson) {
      String internalId = null;

      if (skuJson.has("sku") && !skuJson.isNull("sku")) {
         internalId = skuJson.get("sku").toString();
      }
      return internalId;
   }

   private String crawlNameFrontPage(Document doc, String id) {
      String name = null;
      Element elementName = doc.select("h1[itemprop=name], h1.header-product__title").first();

      if (elementName != null) {
         name = elementName.text();
      } else {
         name = CrawlerUtils.scrapStringSimpleInfo(doc, ".header-product__title--unavailable", true);
      }

      Elements variations = doc.select(".input__select.information-values__variation-select option");

      for (Element e : variations) {
         String idV = e.val();

         if (idV.equals(id)) {
            String variationName = e.ownText().trim();

            if (variationName.contains("-")) {
               variationName = variationName.split("-")[0];
            }

            name += " - " + variationName;

            break;
         }
      }

      return name;
   }

   private String crawlDescription(Document doc, String internalId) {
      StringBuilder description = new StringBuilder();

      Element elementDescription = doc.select(".factsheet-main-container").first();
      Element anchorDescription = doc.select("#anchor-description").first();

      if (elementDescription != null) {
         description.append(elementDescription.html());
      }

      if (anchorDescription != null) {
         description.append(anchorDescription.html());
      }

      return CommonMethods.stripNonValidXMLOrHTMLCharacters(description.toString());
   }

   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;

      Element image = doc.select(".showcase-product__big-img").first();

      if (image != null) {
         primaryImage = image.attr("src").trim();
      }

      if (primaryImage == null) {
         Element primaryImageElement = doc.select(".product-thumbs-carousel__column img").first();

         if (primaryImageElement != null) {
            primaryImage = primaryImageElement.attr("src").replace("88x66", "618x463");
         }
      }

      if (primaryImage == null) {
         Element primaryImageElement = doc.select(".unavailable__product-img").first();

         if (primaryImageElement != null) {
            primaryImage = primaryImageElement.attr("src").replace("88x66", "618x463");
         }
      }

      return primaryImage;
   }

   private List<String> crawlSecondaryImages(Document doc, String primaryImage) {
      List<String> secondaryImagesList = new ArrayList<>();

      Elements imageThumbs = doc.select(".showcase-product__container-thumbs .showcase-product__thumbs img");
      Elements imageThumbsSpecial = doc.select("img.product-thumbs-carousel__thumb");

      if (imageThumbs.size() > imageThumbsSpecial.size()) {
         for (int i = 1; i < imageThumbs.size(); i++) { // starts with index 1, because the first image
            // is the primary image
            Element e = imageThumbs.get(i);

            String image = e.attr("src").replace("88x66", "618x463");

            if (!image.equalsIgnoreCase(primaryImage)) {
               secondaryImagesList.add(image);
            }

         }
      } else {

         for (int i = 1; i < imageThumbsSpecial.size(); i++) { // starts with index 1, because the
            // first image is the primary image
            Element e = imageThumbsSpecial.get(i);

            String image = e.attr("src").replace("88x66", "618x463");

            //Removed the primary image check. In some cases the primary image appears in secondary images, causing disparities from the website
            secondaryImagesList.add(image);
         }
      }

      return secondaryImagesList;
   }

   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select(".breadcrumb__title > a.breadcrumb__item");

      for (int i = 0; i < elementCategories.size(); i++) {
         categories.add(elementCategories.get(i).text().trim());
      }

      return categories;
   }

   private Product crawlProduct(Document document) throws Exception {
      JSONObject skuJson = crawlJsonProduct(document);
      Product product = new Product();

      if (!skuJson.isEmpty()) {
         product = crawlProductFromDefaultLayout(skuJson, document);

      } else {
         skuJson = CrawlerUtils.selectJsonFromHtml(document, "#__NEXT_DATA__", null, "", false, false);

         if (!skuJson.isEmpty()) {
            product = crawlProductFromNewLayout(skuJson, document);
         }
      }

      return product;
   }

   private JSONObject crawlJsonProduct(Document document) {
      JSONObject skuJson = new JSONObject();

      String dataProduct = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".js-header-product[data-product]", "data-product");
      if (dataProduct != null) {
         JsonObject jsonObject = new JsonObject();

         // We use Gson in this case because this json has duplicate keys
         // Gson unify those values
         try {
            jsonObject = (new JsonParser()).parse(dataProduct).getAsJsonObject();
         } catch (JsonSyntaxException e) {
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
         }

         skuJson = CrawlerUtils.stringToJson(jsonObject.toString());

      }

      return skuJson;
   }

   private RatingsReviews scrapRatingsAlternativeWay(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);

      ratingReviews.setTotalRating(getTotalReviewCount(doc));
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setAverageOverallRating(getAverageOverallRating(doc));


      return ratingReviews;
   }

   private Integer getTotalReviewCount(Document doc) {

      return CrawlerUtils.scrapIntegerFromHtml(doc, ".interaction-client__rating-info > span", true, 0);
   }

   private Double getAverageOverallRating(Document doc) {

      return CrawlerUtils.scrapDoublePriceFromHtml(doc, ".interaction-client__rating-info > span", null, true, ',', session);
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".wrapper-reviews .wrapper-review__comment");

      for (Element review : reviews) {

         if (review != null) {

            int numberOfStars = getNumberStar(review);

            switch (numberOfStars) {
               case 5:
                  star5++;
                  break;
               case 4:
                  star4++;
                  break;
               case 3:
                  star3++;
                  break;
               case 2:
                  star2++;
                  break;
               case 1:
                  star1++;
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

   private int getNumberStar(Element e) {
      int star = 0;
      Elements elements = e.select(".rating-percent__full .rating-percent__full-star");
      for (Element el : elements) {
         String width = CrawlerUtils.scrapStringSimpleInfoByAttribute(el, null, "style");
         if (width != null && width.contains("20%")) {
            star++;
         }
      }
      return star;

   }

   public Product crawlProductFromNewLayout(JSONObject skuJsonInfo, Document doc) throws Exception {

      JSONObject json = JSONUtils.getValueRecursive(skuJsonInfo, "props.pageProps.data.product", JSONObject.class);

      String internalId = crawlInternalIdNewLayout(json);
      String reference = json.optString("reference");
      String name = json.optString("title") + (reference != null && !reference.equals("") ? " - " + reference : "");
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "div[data-testid=\"breadcrumb-item-list\"] a span", true);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("section[style='grid-area:maincontent']"));
      List<String> images = crawlImagesNewLayout(skuJsonInfo, json);
      String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
      boolean availableToBuy = json.optBoolean("available");
      Offers offers = availableToBuy ? scrapOffersNewLayout(doc, json) : new Offers();
      RatingsReviews ratingsReviews = scrapRatingsReviews(json);

      // Creating the product
      return ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(internalId)
         .setInternalPid(internalId)
         .setName(name)
         .setCategories(categories)
         .setPrimaryImage(primaryImage)
         .setSecondaryImages(images)
         .setDescription(description)
         .setRatingReviews(ratingsReviews)
         .setOffers(offers)
         .build();
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

   private Offers scrapOffersNewLayout(Document doc, JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      String sellerFullName = JSONUtils.getValueRecursive(json, "seller.id", String.class);

      boolean isMainRetailer = sellerFullName.equalsIgnoreCase(SELLER_NAME) || sellerFullName.equalsIgnoreCase(SELLER_NAME_1.replace(" ", ""));
      Pricing pricing = scrapPricingNewLayout(json);

      offers.add(OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerFullName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(isMainRetailer)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private String crawlInternalIdNewLayout(JSONObject json) {
      String internalId = json.optString("variationId");
      if (internalId == null || internalId.isEmpty()) {
         internalId = json.optString("id");
      }

      return internalId;

   }

   private Pricing scrapPricingNewLayout(JSONObject json) throws MalformedPricingException {
      JSONObject price = json.optJSONObject("price");
      if (price == null) {
         throw new MalformedPricingException("Price is null");
      }

      Double priceFrom = price.optDouble("price", 0.0);
      Double spotlightPrice = price.optDouble("bestPrice", 0.0);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();

   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
         .build());

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

}
