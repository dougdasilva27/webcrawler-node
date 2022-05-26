package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilPalacioDasFerramentas extends Crawler {
   private static String SELLER_FULL_NAME = "Palácio das ferramentas";
   private static String HOST = "www.palaciodasferramentas.com.br/";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.ELO.toString(), Card.HIPERCARD.toString(), Card.HIPER.toString(),
      Card.DINERS.toString(), Card.DISCOVER.toString(), Card.AURA.toString());

   public BrasilPalacioDasFerramentas(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "button#Buy", "data-item");
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "[itemprop=\"sku\"]", false);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1[itemprop=\"name\"]", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "img#mainImage", Arrays.asList("data-big"), "https", HOST);
         List<String> images = CrawlerUtils.scrapSecondaryImages(doc, "ul#productImages img", Arrays.asList("data-big"), "https", HOST, primaryImage);
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, "div.descricao", false);
         Boolean available = true;

         Offers offers = available != null && available ? new Offers() : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return true;//doc.selectFirst(".product-attributes") != null;
   }

   private List<String> crawlSecondaryImages(Document doc, String primaryImage) {
      List<String> secondaryImages = new ArrayList<>();
      Elements images = doc.select("ul#productImages");

      if (!images.isEmpty() && images.size() > 1) {
         images.remove(0);
         for (Element e : images) {
            String imageUrl = CrawlerUtils.scrapSimpleSecondaryImages(doc, "ul#productImages img", Arrays.asList("data-big"), "https", HOST, primaryImage);
            secondaryImages.add(e.attr("src"));
         }
      }

      return secondaryImages;
   }

   private Offers scrapOffers(JSONObject data, Document doc) {
      Offers offers = new Offers();
      try {
         Pricing pricing = scrapPricing(data);
         List<String> sales = scrapSales(pricing, data);

         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());

      } catch (Exception e) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }
      return offers;

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

   private String alternativeRatingFetch(String internalId) {

      String url = "https://trustvox.com.br/widget/root?&code=" + internalId + "&store_id=71447&product_extra_attributes[group]=P";

      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
      headers.put("Accept", "application/vnd.trustvox-v2+json");
      headers.put("Referer", "https://www.drogasil.com.br/");

      Request request = Request.RequestBuilder.create()
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

