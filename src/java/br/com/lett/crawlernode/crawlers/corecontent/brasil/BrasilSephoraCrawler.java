package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import models.Offers;
import models.RatingsReviews;
import models.prices.Prices;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.Map.Entry;

/**
 * date: 05/09/2018
 *
 * @author gabriel
 *
 */

public class BrasilSephoraCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.sephora.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "sephora";

   public BrasilSephoraCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
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

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".no-display input[name=product]", "value");

         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li:not(.home, .product) a", false);

         String description = crawlDescription(doc);

         JSONObject priceProducts = fetchPrice(internalPid);

         String nameBase = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name h1", false);

         for (String internalId : priceProducts.keySet()) {

            JSONObject jsonPrice = JSONUtils.getJSONValue(priceProducts, internalId);

            Element variantEl = crawlVariationElement(doc, internalId);

            String name = scrapName(variantEl, nameBase);

            String primaryImage = crawlPrimaryImage(doc, internalId);

            List<String> secondaryImages = crawlSecondaryImages(doc);

            Offers offers = scrapOffers(jsonPrice);

            RatingsReviews ratingReviews = crawRating(doc, internalPid);

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategories(categories)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setOffers(offers)
               .setRatingReviews(ratingReviews)
               .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(Document document) {
      return document.selectFirst(".reference") != null;
   }

   /*******************
    * General methods *
    *******************/

   private String scrapName(Element variantEl, String nameBase) {
      return nameBase != null ? nameBase + " " +CrawlerUtils.scrapStringSimpleInfo(variantEl, "label p.reference.info", false) : null;
   }

   private Offers scrapOffers(JSONObject jsonPrice) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      if (!JSONUtils.getValue(jsonPrice, "is_salable").equals(true)) {
         return offers;
      }

      Pricing pricing = scrapPricing(jsonPrice);

      List<String> sales = scrapSales(jsonPrice);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(MAIN_SELLER_NAME_LOWER)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());
      return offers;
   }

   private Pricing scrapPricing(JSONObject jsonPrice) throws MalformedPricingException {

      Double spotlightPrice = CrawlerUtils.getDoubleValueFromJSON(jsonPrice, "current_price", true, false);

      Double priceFrom = CrawlerUtils.getDoubleValueFromJSON(jsonPrice, "price", true, false);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      if (priceFrom <= spotlightPrice) {
         priceFrom = null;
      }

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private List<String> scrapSales(JSONObject jsonPrice) {
      List<String> sales = new ArrayList<>();

      Object showDiscount = JSONUtils.getValue(jsonPrice, "show_discount");

      if (showDiscount instanceof Integer) {
         String sale = showDiscount.toString()+"% OFF";
         sales.add(sale);
      }

      return sales;
   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Set<Card> cards = Sets.newHashSet(
         Card.VISA,
         Card.MASTERCARD,
         Card.AURA,
         Card.DINERS,
         Card.HIPER,
         Card.AMEX
      );

      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (Card card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card.toString())
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private Element crawlVariationElement(Document doc, String internalId) {

      return doc.selectFirst(".col-bundle li[data-productid=\" " + internalId + "\"]");
   }

   private Float crawlPrice(Prices prices) {
      Float price = null;

      if (!prices.isEmpty() && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
         Double priceDouble = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
         price = priceDouble.floatValue();
      }

      return price;
   }

   private String crawlPrimaryImage(Document doc, String internalId) {
      String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#variant-" + internalId + "[data-zoom-image]", "data-zoom-image");

      if (primaryImage == null) {
         primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#image-main[data-zoom-image]", "data-zoom-image");
      }

      return primaryImage;
   }

   private List<String> crawlSecondaryImages(Document doc) {
      List<String> secondaryImagesArray = new ArrayList<>();

      Elements images = doc.select("#container_gallery .link-media-gallery");
      for (int i = 1; i < images.size(); i++) {
         String image = images.attr("data-zoom-image").trim();
         if (!image.isEmpty()) {
            secondaryImagesArray.add(image);
         }
      }
      return secondaryImagesArray;
   }

   private Map<String, Prices> crawlMarketplace(JSONObject jsonSku, Document doc) {
      Map<String, Prices> marketplace = new HashMap<>();

      String sellerName = MAIN_SELLER_NAME_LOWER;

      if (jsonSku.has("seller")) {
         JSONObject sellerJson = jsonSku.getJSONObject("seller");

         if (sellerJson.has("name")) {
            sellerName = sellerJson.getString("name").toLowerCase();
         }
      }

      marketplace.put(sellerName, crawlPrices(jsonSku, doc));

      return marketplace;

   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Elements elementsInformation = doc.select("#section-description, #neemu-how-to-use, #neemu-look, .info-content#brand");
      for (Element e : elementsInformation) {
         description.append(e.html());
      }

      return description.toString();
   }

   /**
    * To crawl installments in this site, we need crawl the installments array rule, like this:
    * [["40","1",""],["60","2",""],["80","3",""],["100","4",""],["120","5",""],["140","6",""],["160","7",""],["180","8",""],["200","9",""],["","10",""]]
    * <p>
    * I found this array because in this site, the installments values are calculated, so
    * <p>
    * <p>
    * - if the price is lower then 40 will have 1 installment
    * <p>
    * - if the price is lower then 60 and greater then 40 will have 2 installments
    * <p>
    * ...
    *
    * @param doc
    * @param jsonSku
    * @return
    */
   private Prices crawlPrices(JSONObject jsonSku, Document doc) {
      Prices prices = new Prices();


      if (jsonSku.has("price")) {
         Float price = CrawlerUtils.getFloatValueFromJSON(jsonSku, "price", true, false);
         prices.setBankTicketPrice(price);

         Map<Integer, Float> mapInstallments = new HashMap<>();
         mapInstallments.put(1, price);

         Map<Float, Integer> installmentsRules = crawlInstallmentsRules(doc);

         Integer installmentsNumber = 1;
         for (Entry<Float, Integer> entry : installmentsRules.entrySet()) {
            if (price < entry.getKey()) {
               installmentsNumber = entry.getValue();
               break;
            }
         }

         mapInstallments.put(installmentsNumber, MathUtils.normalizeTwoDecimalPlaces(price / installmentsNumber));

         prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
         prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);
         prices.insertCardInstallment(Card.DINERS.toString(), mapInstallments);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), mapInstallments);
      }

      return prices;
   }

   private Map<Float, Integer> crawlInstallmentsRules(Document doc) {
      Map<Float, Integer> installmentsRulesMap = new TreeMap<>();

      Elements scripts = doc.select("script");
      for (Element e : scripts) {
         String script = e.html().replace(" ", "").toLowerCase();

         if (script.contains("newcatalogproduct(")) {
            int x = script.indexOf("[[");
            int y = script.indexOf("]]", x) + 2;

            try {
               JSONArray array = new JSONArray(script.substring(x, y));

               for (Object o : array) {
                  JSONArray installments = (JSONArray) o;

                  if (installments.length() > 1) {
                     Float value = MathUtils.parseFloatWithComma(installments.get(0).toString());
                     Integer installmentNumber = MathUtils.parseInt(installments.get(1).toString());

                     if (value != null && installmentNumber != null) {
                        installmentsRulesMap.put(value, installmentNumber);
                     } else if (installmentNumber != null) {
                        installmentsRulesMap.put(999999999f, installmentNumber);
                     }
                  }
               }
            } catch (JSONException e1) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e1));
            }

            break;
         }
      }

      return installmentsRulesMap;
   }


   private RatingsReviews crawRating(Document doc, String internalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(getTotalAvgRating(doc));
      ratingReviews.setAdvancedRatingReview(scrapTotalOfRewviesPerEachStar(doc, internalPid));


      return ratingReviews;
   }

   private Double getTotalAvgRating(Document docRating) {
      Double avgRating = 0d;
      Element rating = docRating.selectFirst("#customer-reviews .average span");

      if (rating != null) {
         String text = rating.ownText().replaceAll("[^0-9.]", "").trim();

         if (!text.isEmpty()) {
            avgRating = Double.parseDouble(text);
         }
      }

      return avgRating;
   }


   private Integer getTotalNumOfRatings(Document doc) {
      Integer totalRating = 0;
      Element totalRatingElement = doc.selectFirst("#customer-reviews .average");

      if (totalRatingElement != null) {
         String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!totalText.isEmpty()) {
            totalRating = Integer.parseInt(totalText);
         }
      }

      return totalRating;
   }

   private JSONObject fetchPrice(String id) {
      String url = "https://www.sephora.com.br/restapi/price/getpricesbyparent/product/" + id;

      Request request = Request.RequestBuilder.create().setUrl(url).build();
      String response = this.dataFetcher.get(session, request).getBody();

      return JSONUtils.stringToJson(response);
   }


   private JSONArray fetchRatingApi(String internalPid){
      String url = "https://www.sephora.com.br/ajaxreview/list?product_id="+internalPid+"&cur_page=1";

      Request request =  Request.RequestBuilder.create().setUrl(url).build();
      String response = this.dataFetcher.get(session,request).getBody();

      return JSONUtils.stringToJsonArray(response);

   }

   private AdvancedRatingReview scrapTotalOfRewviesPerEachStar(Document doc,String internalPid) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      JSONArray ratingInfoArr = fetchRatingApi(internalPid);

      if(ratingInfoArr != null) {
         for (Object e : ratingInfoArr) {
            JSONObject ratingInfo = (JSONObject) e;
            int numberOfStars = ratingInfo.optInt("votes_percent");

            if (numberOfStars == 20) {
               star1++;
            } else if (numberOfStars == 40) {
               star2++;
            } else if (numberOfStars == 60) {
               star3++;
            } else if (numberOfStars == 80) {
               star4++;
            } else if (numberOfStars == 100) {
               star5++;
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


}
