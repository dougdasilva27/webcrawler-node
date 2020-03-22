package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

/**
 * 
 * @author samirleao
 * @author gabriel (refactor) 06/06/17
 *
 */

public class BrasilMagazineluizaCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.magazineluiza.com.br/";
   private static final String SELLER_NAME = "magalu";
   private static final String SELLER_NAME_1 = "magazine luiza";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilMagazineluizaCrawler(Session session) {
      super(session);
      this.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         products.add(crawlProduct(doc));
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   public Product crawlProduct(Document doc) throws Exception {
      JSONObject skuJsonInfo = crawlFullSKUInfo(doc);

      String internalId = crawlInternalId(skuJsonInfo);
      String internalPid = internalId;
      String frontPageName = crawlNameFrontPage(doc, internalId);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc, primaryImage);
      boolean availableToBuy = !doc.select(".button__buy-product-detail").isEmpty();
      Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();
      RatingsReviews ratingReviews = crawlRatingNew(doc, internalId);
      String description = crawlDescription(doc, internalId);

      // Creating the product
      return ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(frontPageName)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .setRatingReviews(ratingReviews)
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
      Double percentage = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".method-payment__topic-title .method-payment__parcel .method-payment__discount-text", null, true, ',', session);;
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
         Double percentage = CrawlerUtils.scrapDoublePriceFromHtml(e, ".method-payment__discount-text", null, true, ',', session);;
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

   /*******************************
    * Product page identification *
    *******************************/
   private boolean isProductPage(Document doc) {
      return doc.select("h1[itemprop=name]").first() != null;
   }

   /**
    * Crawl Internal ID
    * 
    * @param doc
    * @return
    */
   private String crawlInternalId(JSONObject skuJson) {
      String internalId = null;

      if (skuJson.has("sku") && !skuJson.isNull("sku")) {
         internalId = skuJson.get("sku").toString();
      }

      return internalId;
   }

   /**
    * Crawl name in front page
    * 
    * @param doc
    * @return
    */
   private String crawlNameFrontPage(Document doc, String id) {
      String name = null;
      Element elementName = doc.select("h1[itemprop=name]").first();

      if (elementName != null) {
         name = elementName.text();
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

   /**
    * Crawl Description
    * 
    * @param doc
    * @return
    */
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

      // String descriptionURL = "http://www.magazineluiza.com.br/produto/ficha-tecnica/" + internalId +
      // "/";
      // description.append(DataFetcher.fetchString("GET", session, descriptionURL, null, cookies));

      return CommonMethods.stripNonValidXMLOrHTMLCharacters(description.toString());
   }

   /**
    * 
    * @param doc
    * @return
    */
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

   /**
    * 
    * @param doc
    * @return
    */
   private String crawlSecondaryImages(Document doc, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements imageThumbs = doc.select(".showcase-product__container-thumbs .showcase-product__thumbs img");
      Elements imageThumbsSpecial = doc.select("img.product-thumbs-carousel__thumb");

      if (imageThumbs.size() > imageThumbsSpecial.size()) {
         for (int i = 1; i < imageThumbs.size(); i++) { // starts with index 1, because the first image
            // is the primary image
            Element e = imageThumbs.get(i);

            String image = e.attr("src").replace("88x66", "618x463");

            if (!image.equalsIgnoreCase(primaryImage)) {
               secondaryImagesArray.put(image);
            }

         }
      } else {

         for (int i = 1; i < imageThumbsSpecial.size(); i++) { // starts with index 1, because the
            // first image is the primary image
            Element e = imageThumbsSpecial.get(i);

            String image = e.attr("src").replace("88x66", "618x463");

            if (!image.equalsIgnoreCase(primaryImage)) {
               secondaryImagesArray.put(image);
            }

         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   /**
    * Crawl categories
    * 
    * @param document
    * @return
    */
   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select(".breadcrumb__title > a.breadcrumb__item");

      for (int i = 0; i < elementCategories.size(); i++) {
         categories.add(elementCategories.get(i).text().trim());
      }

      return categories;
   }


   /**
    * @param document
    * @return a json object containing all sku informations in this page.
    */
   private JSONObject crawlFullSKUInfo(Document document) {
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

   public RatingsReviews crawlRatingNew(Document doc, String internalId) {

      RatingsReviews ratingReviews = crawlRatingReviews(doc);
      ratingReviews.setInternalId(internalId);

      return ratingReviews;
   }

   private RatingsReviews crawlRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());

      ratingReviews.setTotalRating(getTotalReviewCount(doc));
      ratingReviews.setAverageOverallRating(getAverageOverallRating(doc));

      return ratingReviews;
   }

   private Integer getTotalReviewCount(Document doc) {
      Integer totalReviewCount = null;
      Element total = doc.select(".interaction-client__rating-info > span").last();

      if (total != null) {
         totalReviewCount = Integer.parseInt(total.ownText().replaceAll("[^0-9]", ""));
      }

      return totalReviewCount;
   }

   private Double getAverageOverallRating(Document doc) {
      Double avgOverallRating = null;
      Element avg = doc.select(".interaction-client__rating-info > span").first();

      if (avg != null) {
         avgOverallRating = Double.parseDouble(avg.ownText().replaceAll("[^0-9,]", "").replace(",", "."));
      }

      return avgOverallRating;
   }
}
