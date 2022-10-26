package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PricesmartCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPERCARD.toString(), Card.ELO.toString());
   private static final String MAINSELLER = "Price Smart";


   public PricesmartCrawler(Session session) {
      super(session);
   }


   @Override
   protected Object fetch() {

      String club_id = session.getOptions().optString("club_id");
      String country = session.getOptions().optString("country");

      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "userPreferences=country=" + country + "&selectedClub=" + club_id + "&lang=es");

      Request request = Request.RequestBuilder.create().setUrl(session.getOriginalURL()).setHeaders(headers).build();
      String response = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(response);

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".row .product-price-small") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "#itemNumber", false);
         List<String> categories = CrawlerUtils.crawlCategories(doc, ".product-page-breadcrumb a", true);
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, "#collapseOne .card-body", true);
         Integer stock = null;
         boolean available = doc.selectFirst(".btn-add-to-cart-disabled") == null;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#product-name-item", false);
         JSONArray jsonVariations = getVariations(doc);
         if (jsonVariations != null && jsonVariations.length() > 0) {
            for (Object variantion : jsonVariations) {
               JSONObject jsonVariantion = (JSONObject) variantion;
               String internalId = jsonVariantion.optString("productId");
               String nameVariantion = crawlVariationName(jsonVariantion, name);
               List<String> images = getImages(jsonVariantion);
               String primaryImage = images.size() > 0 ? images.remove(0) : null;
               List<String> secondaryImages = images.size() > 0 ? images : null;
               Offers offers = available ? scrapOffers(doc) : new Offers();

               // Creating the product
               Product product = ProductBuilder.create()
                  .setUrl(updateUrl(this.session.getOriginalURL()))
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(nameVariantion)
                  .setCategories(categories)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setStock(stock)
                  .setOffers(offers)
                  .build();

               products.add(product);

            }
         } else {
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".pdp-main-image", Arrays.asList("src"), "http://", "pim-img-psmt1.aeropost.com");
            List<String> secondaryImage = CrawlerUtils.scrapSecondaryImages(doc, ".product-thumb-item-img", Arrays.asList("src"), "", "", primaryImage);
            Offers offers = available ? scrapOffers(doc) : new Offers();
            Product product = ProductBuilder.create()
               .setUrl(updateUrl(this.session.getOriginalURL()))
               .setInternalId(internalPid)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategories(categories)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImage)
               .setDescription(description)
               .setStock(stock)
               .setOffers(offers)
               .build();

            products.add(product);
         }


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String crawlVariationName(JSONObject jsonVariantion, String name) {
      JSONObject jsonColor = getJsonVariation(jsonVariantion, "color");
      JSONObject jsonSize = getJsonVariation(jsonVariantion, "size");
      JSONObject jsonApresentation = getJsonVariation(jsonVariantion, "COF_PRES");
      String color = jsonColor != null ? getVariation(jsonColor) : "";
      String size = jsonSize != null ? getVariation(jsonSize) : "";
      String apresentation = jsonSize != null ? getVariation(jsonApresentation) : "";

      return name + " " + color + " " + size + " " + apresentation;

   }

   private String getVariation(JSONObject json) {
      JSONArray displayName = json.optJSONArray("displayName");
      if (displayName != null && displayName.length() > 0) {
         for (Object o : displayName) {
            JSONObject jsonDisplayName = (JSONObject) o;
            String language = jsonDisplayName.optString("language");
            if (language.equals("es")) {
             return jsonDisplayName.optString("value");
            }
         }
      }

      return "";
   }


   private JSONObject getJsonVariation(JSONObject jsonVariantion, String type) {
      JSONArray array =  jsonVariantion.optJSONArray("customAttributes");
      if (array != null && array.length() > 0) {
         for (Object object : array) {
            JSONObject jsonObject = (JSONObject) object;
            if (jsonObject.optString("attributeId").equals(type)) {
               return jsonObject;
            }
         }
      }

      return new JSONObject();
   }

   private List<String> getImages(JSONObject jsonObject) {
      JSONArray imagesJsonArray = JSONUtils.getValueRecursive(jsonObject, "images.imageGroup.0.image", JSONArray.class);
      List<String> images = new ArrayList<String>();
      if (imagesJsonArray != null) {
         for (Object imgObject : imagesJsonArray) {
            JSONObject imgJson = (JSONObject) imgObject;
            images.add(JSONUtils.getValueRecursive(imgJson, "path", String.class));
         }
      }
      return images;
   }

   private JSONArray getVariations(Document doc) {
      Element script = doc.selectFirst("script:containsData(\n        var trackingData)");
      String varString = script.toString();
      String extractedString = CrawlerUtils.extractSpecificStringFromScript(varString, "JSON.parse('", true, "');", true);
      String sanitizedString = extractedString.replaceAll("\\\\\"", "\"").replaceAll("\\\\\\\\", "\\\\");
      JSONObject objJson = JSONUtils.stringToJson(sanitizedString);
      JSONArray variants = JSONUtils.getValueRecursive(objJson, "productVariants", JSONArray.class);
      return variants;
   }

   private String updateUrl(String url) {
      String regex = "/site/(..)/es";
      String updatedUrl = url;
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(url);

      if (matcher.find()) {
         updatedUrl = url.replace(matcher.group(1), session.getOptions().optString("country"));
      }
      return updatedUrl;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setMainPagePosition(1)
         .setSellerFullName(MAINSELLER)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setSales(sales)
         .setPricing(pricing)
         .build());


      return offers;

   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      return sales;
   }


   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double  spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc,"#product-price.currency",null,false,',',session);
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.currency", null, false, ',', session);
      price = price != null ? price : spotlightPrice;
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(price)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();

   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
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
}
