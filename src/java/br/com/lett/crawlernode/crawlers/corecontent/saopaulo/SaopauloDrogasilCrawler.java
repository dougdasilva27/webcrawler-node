package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

public class SaopauloDrogasilCrawler extends Crawler {

   private static String SELLER_FULL_NAME = "Drogasil (São Paulo)";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.ELO.toString(), Card.HIPERCARD.toString(), Card.HIPER.toString(),
      Card.DINERS.toString(), Card.DISCOVER.toString(), Card.AURA.toString());
   // I could'nt find another selector for this description
   private static final String SMALL_DESCRIPTION_SELECTOR = ".sc-fzqNJr.hXQgjp";

   private List<String> getSellers() {
      JSONArray sellersArray = session.getOptions().optJSONArray("sellers");
      List<String> sellersList = new ArrayList<>();

      if (sellersArray != null && !sellersArray.isEmpty()) {
         for (Object o : sellersArray) {
            String seller = (String) o;
            sellersList.add(seller);
         }
      }
      return sellersList;
   }
   private List<String> SELLERS = getSellers();
   public SaopauloDrogasilCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches();
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, " ", false, false);
         JSONObject data = JSONUtils.getValueRecursive(json, "props.pageProps.pageData.productData.productBySku", JSONObject.class);

         if (data != null) {

            String internalId = JSONUtils.getStringValue(data, "sku");
            String internalPid = String.valueOf(JSONUtils.getValue(data, "id"));
            String name = scrapName(data, doc);

            Pair<String, Object> validationImages = new Pair<>();
            validationImages.set("disabled", false);

            List<String> images = CrawlerUtils.scrapImagesListFromJSONArray(JSONUtils.getJSONArrayValue(data, "media_gallery_entries"),
               "file", validationImages, "https", "img.drogasil.com.br/catalog/product", session);

            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".small-img", Arrays.asList("src"), "https", "img.drogasil.com.br");

            if (primaryImage != null) {
               primaryImage = primaryImage.split("\\?")[0]; // we need to remove parameters because this site resize img on html
            } else if (!images.isEmpty()) {
               primaryImage = images.get(0);
            }

            String description = scrapDescription(data);
            List<String> ean = new ArrayList<>();
            ean.add(CrawlerUtils.scrapStringSimpleInfo(doc, "tr:nth-child(2) > td", false));
            Boolean available = JSONUtils.getValueRecursive(data,
               "extension_attributes.stock_item.is_in_stock",
               Boolean.class);
            RatingsReviews ratingsReviews = crawlRating(internalId);
            Offers offers = available != null && available ? scrapOffers(data, doc) : new Offers();

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setEans(ean)
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

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-attributes") != null;
   }

   private String scrapName(JSONObject data, Document doc) {
      StringBuilder name = new StringBuilder();

      String mainName = data.optString("name");

      if (!mainName.isEmpty()) {
         name.append(mainName);

         String brand = CrawlerUtils.scrapStringSimpleInfo(doc, "div.rd-container li.brand", true);

         if (brand != null) {
            name.append(" ").append(brand);
         }

         String quantity = CrawlerUtils.scrapStringSimpleInfo(doc, "div.rd-container li.quantity", true);

         if (quantity != null) {
            name.append(" ").append(quantity);
         }
      }

      return name.toString();
   }

   private Offers scrapOffers(JSONObject data, Document doc) {
      Offers offers = new Offers();
      try {
         Pricing pricing = scrapPricing(data);
         List<String> sales = scrapSales(pricing, data);

         String mainSeller = isMainSeller(doc);

         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(mainSeller)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(mainSeller.equals(SELLER_FULL_NAME))
            .setPricing(pricing)
            .setSales(sales)
            .build());

      } catch (Exception e) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }
      return offers;

   }
   private String isMainSeller(Document doc) {
      String isMarketPlace = CrawlerUtils.scrapStringSimpleInfo(doc, "div[class*='SoldAndDelivered'] a", true);

      if (isMarketPlace != null) {
         return isMarketPlace;
      }

      return SELLER_FULL_NAME;
   }
   private List<String> scrapSales(Pricing pricing, JSONObject data) {
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      Object salesQuantity = data.optQuery("/price_aux/lmpm_qty");
      Object salesPrice = data.optQuery("/price_aux/lmpm_value_to");

      if (salesQuantity instanceof Integer && salesPrice != null) {
         int quantity = (int) salesQuantity;
         Double price = CommonMethods.objectToDouble(salesPrice);
         if (quantity > 1 && price != null) {
            sales.add("Leve " + quantity + " unidades por R$ " + price + " cada");
         }
      }

      return sales;
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      Object spotlightPriceObject = data.optQuery("/price_aux/value_to");
      Object priceFromObject = data.optQuery("/price_aux/value_from");

      Double spotlightPrice = CommonMethods.objectToDouble(spotlightPriceObject);
      Double priceFrom = CommonMethods.objectToDouble(priceFromObject);

      if (Objects.equals(priceFrom, spotlightPrice)) priceFrom = null;

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
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

   private String scrapDescription(JSONObject json) {
      JSONArray descriptionArray = json.optJSONArray("custom_attributes");

      if (descriptionArray != null) {
         for (Object attribute : descriptionArray) {
            if (JSONUtils.getValueRecursive(attribute, "attribute_code", String.class).equals("description")) {
               return JSONUtils.getValueRecursive(attribute, "value_string.0", String.class);
            }
         }
      }

      return null;
   }

   private String alternativeRatingFetch(String internalId) {

      String url = "https://trustvox.com.br/widget/root?&code=" + internalId + "&store_id=71447&product_extra_attributes[group]=P";

      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
      headers.put("Accept", "application/vnd.trustvox-v2+json");
      headers.put("Referer", "https://www.drogasil.com.br/");

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      return new FetcherDataFetcher().get(session, request).getBody();
   }

   private RatingsReviews crawlRating(String internalId) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      String ratingResponse = alternativeRatingFetch(internalId);

      JSONObject rating = CrawlerUtils.stringToJson(ratingResponse);

      Number avgReviews = JSONUtils.getValueRecursive(rating, "rate.average", Number.class);
      Number totalReviews = JSONUtils.getValueRecursive(rating, "rate.count", Number.class);
      Integer totalReviewsInt = Objects.isNull(totalReviews) ? null : totalReviews.intValue();
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(rating);

      ratingsReviews.setTotalRating(totalReviewsInt);
      ratingsReviews.setTotalWrittenReviews(totalReviewsInt);
      ratingsReviews.setAverageOverallRating(Objects.isNull(avgReviews) ? null : avgReviews.doubleValue());
      ratingsReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingsReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject rating) {
      JSONObject stars = JSONUtils.getValueRecursive(rating, "rate.histogram", JSONObject.class);
      if (stars != null) {
         Integer star1 = stars.optInt("1");
         Integer star2 = stars.optInt("2");
         Integer star3 = stars.optInt("3");
         Integer star4 = stars.optInt("4");
         Integer star5 = stars.optInt("5");

         return new AdvancedRatingReview.Builder()
            .totalStar1(star1)
            .totalStar2(star2)
            .totalStar3(star3)
            .totalStar4(star4)
            .totalStar5(star5)
            .build();
      } else {
         return new AdvancedRatingReview();
      }
   }
}
