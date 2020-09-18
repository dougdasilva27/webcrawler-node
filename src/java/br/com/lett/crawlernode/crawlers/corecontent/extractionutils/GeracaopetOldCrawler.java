package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing.PricingBuilder;

public class GeracaopetOldCrawler {

   private static final String YOURVIEWS_API_KEY = "86ceddc8-6468-456a-a862-aad27453c9ae";

   protected String cep;
   protected Session session;
   protected Logger logger;
   protected DataFetcher dataFetcher;
   protected List<Cookie> cookies = new ArrayList<>();

   public GeracaopetOldCrawler(Session session, String cep, Logger logger, DataFetcher dataFetcher) {
      this.logger = logger;
      this.dataFetcher = dataFetcher;
      this.cep = cep;
      this.session = session;

      BasicClientCookie cookie = new BasicClientCookie("zipcode", cep);
      cookie.setDomain(".www.geracaopet.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());
         JSONObject jsonHtml = crawlJsonHtml(doc);

         JSONObject skuJson = crawlSkuJson(jsonHtml);

         String internalPid = crawlInternalPid(doc);
         String description = crawlDescription(doc);

         JSONObject options = crawlOptions(skuJson);

         if (options.length() > 0) {
            Map<String, Set<String>> variationsMap = crawlVariationsMap(skuJson);

            for (String internalId : options.keySet()) {

               boolean available = crawlAvailabilityWithVariation(variationsMap, internalId);
               String name = crawlNameWithVariation(doc, variationsMap, internalId);
               String primaryImage = crawlPrimaryImageWithVariation(skuJson, internalId);

               String secondaryImages = crawlSecondaryImagesWithVariation(skuJson, internalId,
                     available);
               Integer stock = null;
               RatingsReviews ratingsReviews = scrapRatingsReviews(skuJson, internalPid);
               Offers offers = scrapOffers(doc, skuJson, internalId);
               // Creating the product
               Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
                     .setInternalId(internalId).setInternalPid(internalPid).setOffers(offers)
                     .setName(name).setCategory1(null).setCategory2(null).setCategory3(null)
                     .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
                     .setDescription(description).setStock(stock)
                     .setEans(null).setRatingReviews(ratingsReviews).build();

               products.add(product);
            }
         } else {
            String internalId = crawlInternalId(doc);
            String name = crawlName(doc);
            String primaryImage = crawlPrimaryImage(jsonHtml);
            String secondaryImages = crawlSecondaryImages(jsonHtml);
            RatingsReviews ratingsReviews = scrapRatingsReviews(jsonHtml, internalPid);
            Offers offers = scrapOffers(doc, jsonHtml, internalId);

            // Creating the product
            Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setCategory1(null)
                  .setCategory2(null).setCategory3(null)
                  .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
                  .setDescription(description).setStock(null).setOffers(offers)
                  .setEans(null).setRatingReviews(ratingsReviews).build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(Document doc, JSONObject json, String internalId)
         throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Double price;
      if (doc.selectFirst(".btn.action") == null) {
         return offers;
      }

      JSONObject jsonConfig = json.optJSONObject("jsonConfig");
      JSONObject eachPrice = null;
      if (jsonConfig != null) {
         JSONObject optionPrices = jsonConfig.optJSONObject("optionPrices");
         if (optionPrices != null) {
            eachPrice = optionPrices.optJSONObject(internalId);
         }
      }

      if (eachPrice != null) {
         price = CrawlerUtils
               .getDoubleValueFromJSON(eachPrice.optJSONObject("finalPrice"), "amount", false, false);
      } else {
         price = CrawlerUtils
               .scrapDoublePriceFromHtml(doc, "#product-price-" + internalId, "data-price-amount", false,
                     '.', session);
      }

      Double priceFrom = CrawlerUtils
            .scrapDoublePriceFromHtml(doc, ".old-price span[data-price-amount]", "data-price-amount",
                  false, '.', session);
      if (Objects.equals(price, priceFrom)) {
         priceFrom = null;
      }

      String sellerName = "Petland";
      Element saleElem = doc.selectFirst(".product-discount");
      List<String> sales = new ArrayList<>();
      if (saleElem != null && saleElem.wholeText() != null) {
         String sale = saleElem.wholeText().trim();
         if (Pattern.matches("[0-9]", sale)) {
            sales.add(sale);
         }
      }

      offers.add(OfferBuilder.create()
            .setSales(sales)
            .setPricing(PricingBuilder.create()
                  .setSpotlightPrice(price)
                  .setPriceFrom(priceFrom)
                  .setBankSlip(BankSlipBuilder
                        .create()
                        .setFinalPrice(price)
                        .build())
                  .setCreditCards(new CreditCards(extCreditCards(price)))
                  .build())
            .setIsMainRetailer(true)
            .setSellerFullName(sellerName)
            .setIsBuybox(false)
            .setUseSlugNameAsInternalSellerId(true)
            .build());

      return offers;
   }

   private List<CreditCard> extCreditCards(Double price) {
      return Stream.of(Card.VISA, Card.MASTERCARD, Card.ELO)
            .map(card -> {
               try {
                  return CreditCard.CreditCardBuilder.create()
                        .setIsShopCard(false)
                        .setBrand(card.toString())
                        .setInstallments(new Installments(
                              Collections.singleton(InstallmentBuilder
                                    .create()
                                    .setInstallmentNumber(1)
                                    .setInstallmentPrice(price)
                                    .setFinalPrice(price)
                                    .build())
                        ))
                        .build();
               } catch (MalformedPricingException e) {
                  throw new RuntimeException(e);
               }
            })
            .collect(Collectors.toList());
   }

   private RatingsReviews scrapRatingsReviews(JSONObject json, String internalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      YourreviewsRatingCrawler yr =
            new YourreviewsRatingCrawler(session, cookies, logger, YOURVIEWS_API_KEY, this.dataFetcher);

      Document docRating = yr
            .crawlPageRatingsFromYourViews(internalPid, YOURVIEWS_API_KEY, this.dataFetcher);

      Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
      Double avgRating = getTotalAvgRating(docRating);
      AdvancedRatingReview advancedRatingReview = yr.getTotalStarsFromEachValue(internalPid);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Double getTotalAvgRating(Document docRating) {
      Double avgRating = null;
      Element rating = docRating.selectFirst("meta[itemprop=ratingValue]");
      if (rating != null) {
         avgRating = Double.parseDouble(rating.attr("content"));

      }

      return avgRating;
   }

   private Integer getTotalNumOfRatings(Document doc) {
      Integer totalRating = null;
      Element totalRatingElement = doc.select("strong[itemprop=ratingCount]").first();

      if (totalRatingElement != null) {
         String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!totalText.isEmpty()) {
            totalRating = Integer.parseInt(totalText);
         }
      }

      return totalRating;
   }

   private String crawlSecondaryImages(JSONObject jsonHtml) {
      JSONArray secondaryImages = new JSONArray();

      if (jsonHtml.has("[data-gallery-role=gallery-placeholder]")) {
         JSONObject galleryPlaceholder = jsonHtml
               .getJSONObject("[data-gallery-role=gallery-placeholder]");

         if (galleryPlaceholder.has("mage/gallery/gallery")) {
            JSONObject gallery = galleryPlaceholder.getJSONObject("mage/gallery/gallery");

            if (gallery.has("data")) {
               JSONArray images = gallery.getJSONArray("data");

               for (Object object : images) {
                  JSONObject image = (JSONObject) object;

                  if (image.has("isMain") && !image.getBoolean("isMain") && image.has("img")) {
                     secondaryImages.put(image.getString("img"));

                  }
               }
            }
         }
      }

      return secondaryImages.toString();
   }

   private String crawlPrimaryImage(JSONObject jsonHtml) {
      String primaryImage = null;

      if (jsonHtml.has("[data-gallery-role=gallery-placeholder]")) {
         JSONObject galleryPlaceholder = jsonHtml
               .getJSONObject("[data-gallery-role=gallery-placeholder]");

         if (galleryPlaceholder.has("mage/gallery/gallery")) {
            JSONObject gallery = galleryPlaceholder.getJSONObject("mage/gallery/gallery");

            if (gallery.has("data")) {
               JSONArray images = gallery.getJSONArray("data");

               for (Object object : images) {
                  JSONObject image = (JSONObject) object;

                  if (image.has("isMain") && image.getBoolean("isMain") && image.has("img")) {
                     primaryImage = image.getString("img");

                  }
               }
            }
         }
      }

      return primaryImage;
   }

   private String crawlName(Document doc) {

      Element title = doc.selectFirst("h1 span[itemprop=\"name\"]");
      String name = null;

      if (title != null) {
         name = title.text();
      }

      return name;
   }

   private String crawlInternalId(Document doc) {
      Element input = doc.selectFirst("input[name=\"product\"]");
      String internalId = null;

      if (input != null) {
         internalId = input.val();
      }

      return internalId;
   }

   private boolean crawlAvailabilityWithVariation(Map<String, Set<String>> variationsMap,
         String internalId) {
      boolean availability = false;

      if (variationsMap.containsKey(internalId)) {
         String name = variationsMap.get(internalId).toString();
         if (!name.contains("disabled")) {
            availability = true;
         }
      }
      return availability;
   }

   private JSONObject crawlSkuJson(JSONObject jsonHtml) {
      JSONObject skuJson = new JSONObject();

      if (jsonHtml.has("[data-role=swatch-options]")) {
         JSONObject dataSwatch = jsonHtml.getJSONObject("[data-role=swatch-options]");

         if (dataSwatch.has("Magento_Swatches/js/swatch-renderer")) {
            skuJson = dataSwatch.getJSONObject("Magento_Swatches/js/swatch-renderer");
         }
      }

      return skuJson;
   }

   private Map<String, Set<String>> crawlVariationsMap(JSONObject skuJson) {
      Map<String, Set<String>> variationsMap = new HashMap<>();
      JSONArray options = new JSONArray();

      if (skuJson.has("jsonConfig")) {
         JSONObject jsonConfig = skuJson.getJSONObject("jsonConfig");

         if (jsonConfig.has("attributes")) {
            JSONObject attributes = jsonConfig.getJSONObject("attributes");

            for (String keyStr : attributes.keySet()) {
               JSONObject attribute = (JSONObject) attributes.get(keyStr);

               if (attribute.has("options")) {
                  options = attribute.getJSONArray("options");
               }
            }
         }
      }

      for (Object object : options) {
         JSONObject option = (JSONObject) object;
         String label = null;
         if (option.has("label")) {
            label = option.getString("label");
         }

         if (option.has("products")) {
            JSONArray products = option.getJSONArray("products");

            for (Object object2 : products) {
               String id = (String) object2;

               if (variationsMap.containsKey(id)) {
                  Set<String> names = variationsMap.get(id);
                  Set<String> newList = new HashSet<>(names);
                  newList.add(label);
                  variationsMap.put(id, newList);
               } else {
                  Set<String> newSet = new HashSet<>();
                  newSet.add(label);
                  variationsMap.put(id, newSet);
               }
            }
         }
      }

      return variationsMap;
   }

   private JSONObject crawlOptions(JSONObject skuJson) {
      JSONObject optionPrices = new JSONObject();

      if (skuJson.has("jsonConfig")) {
         JSONObject jsonConfig = skuJson.getJSONObject("jsonConfig");

         if (jsonConfig.has("optionPrices")) {
            optionPrices = jsonConfig.getJSONObject("optionPrices");
         }
      }

      return optionPrices;
   }

   private String crawlSecondaryImagesWithVariation(JSONObject skuJson, String internalId,
         boolean available) {
      JSONArray secondaryImages = new JSONArray();

      if (available && skuJson.has("jsonConfig")) {
         JSONObject jsonConfig = skuJson.getJSONObject("jsonConfig");
         if (jsonConfig.has("images")) {
            JSONObject images = jsonConfig.getJSONObject("images");

            if (images.has(internalId)) {
               JSONArray image = images.getJSONArray(internalId);

               for (Object object : image) {
                  JSONObject img = (JSONObject) object;

                  if (img.has("isMain") && !img.getBoolean("isMain") && img.has("img")) {
                     secondaryImages.put(img.getString("img"));
                  }
               }
            }
         }
      }

      return secondaryImages.toString();
   }

   private String crawlPrimaryImageWithVariation(JSONObject skuJson, String internalId) {
      String primaryImage = null;
      if (skuJson.has("jsonConfig")) {
         JSONObject jsonConfig = skuJson.getJSONObject("jsonConfig");
         if (jsonConfig.has("images")) {
            JSONObject images = jsonConfig.getJSONObject("images");

            if (images.has(internalId)) {
               JSONArray image = images.getJSONArray(internalId);

               for (Object object : image) {
                  JSONObject img = (JSONObject) object;

                  if (img.optBoolean("isMain")) {
                     primaryImage = img.getString("img");
                  }
               }
            }
         }
      }

      return primaryImage;
   }

   private String crawlNameWithVariation(Document doc, Map<String, Set<String>> variationsMap,
         String internalId) {
      Element title = doc.selectFirst("h1 span[itemprop=\"name\"]");
      String name = null;

      if (title != null) {
         name = title.text();

         if (variationsMap.containsKey(internalId)) {
            String variation = variationsMap.get(internalId).toString();

            if (variation.contains("[") && variation.contains("]")) {
               variation = variation.replace("[", "").replace("]", "");
            }

            if (variation.contains("disabled")) {
               variation = variation.replaceAll("disabled", "");
            }
            name = name.concat(" ").concat(variation);
         }
      }

      return name;
   }

   private String crawlDescription(Document doc) {

      Element div = doc.selectFirst(".data.item.content");
      String description = null;

      if (div != null) {
         description = div.html();
      }

      return description;
   }

   private String crawlInternalPid(Document doc) {
      String internalPid = null;
      Element div = doc.selectFirst("div[data-product-id]");

      if (div != null) {
         internalPid = div.attr("data-product-id");
      }

      return internalPid;
   }

   private JSONObject crawlJsonHtml(Document doc) {
      JSONObject skuJson = new JSONObject();
      Element script = doc.selectFirst(".fieldset script[type=\"text/x-magento-init\"]");

      if (script != null) {
         skuJson = CrawlerUtils.stringToJson(script.html());

      } else {
         script = doc.selectFirst(".media script[type=\"text/x-magento-init\"]");

         if (script != null) {
            skuJson = CrawlerUtils.stringToJson(script.html());
         }
      }

      return skuJson;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("h1 span[itemprop=name]") != null;
   }

}
