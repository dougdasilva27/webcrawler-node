package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Date: 15/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilPichauCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.pichau.com.br/";
   private static final String SELLER_FULL_NAME = "Pichau";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.HIPERCARD.toString());

   public BrasilPichauCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".sku .value", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product.title h1", true);
         boolean available = !doc.select(".stock.available").isEmpty();
         //the product page has no categories
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs .item:not(.home):not(.product)");
         JSONArray images = CrawlerUtils.crawlArrayImagesFromScriptMagento(doc);
         String primaryImage = crawlPrimaryImage(images, doc);
         String secondaryImages = crawlSecondaryImages(images);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".form-pichau-product .product.info")).replace("hidemobile", "");
         RatingsReviews ratingReviews = crawlRating(doc);
         Offers offers = available ? scrapOffers(doc) : new Offers();

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
            .setRatingReviews(ratingReviews)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product-info-main").isEmpty();
   }

   private static String crawlInternalId(Document doc) {
      String internalId = null;

      Element infoElement = doc.selectFirst("input[name=product]");
      if (infoElement != null) {
         internalId = infoElement.val();
      }

      return internalId;
   }

   private RatingsReviews crawlRating(Document document) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = crawlInternalId(document);
      JSONObject productInfo = crawlProductInfo(document);

      Integer totalNumOfEvaluations = getTotalNumOfRatings(productInfo);
      Double avgRating = getTotalAvgRating(productInfo);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      return ratingReviews;
   }

   private Double getTotalAvgRating(JSONObject ratingJson) {
      Double avgRating = 0d;

      if (ratingJson.has("ratingValue") && ratingJson.get("ratingValue") instanceof Double) {
         avgRating = ratingJson.getDouble("ratingValue");
      }

      return avgRating;
   }

   private Integer getTotalNumOfRatings(JSONObject ratingJson) {
      Integer totalRating = 0;

      if (ratingJson.has("ratingCount")) {
         String text = ratingJson.get("ratingCount").toString().replaceAll("[^0-9]", "");

         if (!text.isEmpty()) {
            totalRating = Integer.parseInt(text);
         }
      }

      return totalRating;
   }

   private JSONObject crawlProductInfo(Document doc) {
      JSONObject obj = new JSONObject();

      Elements scripts = doc.select("script");
      for (Element e : scripts) {
         String script = e.html().replace(" ", "");

         if (script.contains("aggregateRating")) {
            JSONArray array = CrawlerUtils.stringToJsonArray(script);
            if (array.length() > 0) {
               obj = array.getJSONObject(0);
            }
            break;
         }
      }

      if (obj.has("aggregateRating")) {
         return obj.getJSONObject("aggregateRating");
      }

      return obj;
   }

   private String crawlPrimaryImage(JSONArray images, Document doc) {
      Element metaImage = doc.selectFirst("meta[property=\"og:image\"]");
      String token = null;
      String primaryImage = null;

      if (metaImage != null) {
         String attr = metaImage.attr("content");
         token = attr.substring(attr.lastIndexOf("/"));
      }
      for (Object object : images) {
         String image = (String) object;

         if (image.endsWith(token)) {
            primaryImage = image;
         }
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(JSONArray images) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      if (images.length() > 1) {
         images.remove(0);
         secondaryImagesArray = images;
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }


   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-wrapper .price", null, true, ',', session);
      Double priceBoleto = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-boleto span", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(doc);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(priceBoleto)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Element doc) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Element e = doc.selectFirst(".price-installments");
      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, false, "x", "cartão", true, ',');
      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(pair.getFirst())
            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
            .build());
      }

         for (String brand : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }

}
