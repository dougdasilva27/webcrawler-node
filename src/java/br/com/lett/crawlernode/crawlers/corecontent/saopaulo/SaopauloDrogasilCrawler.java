package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class SaopauloDrogasilCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Drogasil (São Paulo)";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.ELO.toString(), Card.HIPERCARD.toString(), Card.HIPER.toString(),
      Card.DINERS.toString(), Card.DISCOVER.toString(), Card.AURA.toString());

   public SaopauloDrogasilCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
      super.config.setFetcher(FetchMode.APACHE);
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
         JSONObject data = JSONUtils.getValueRecursive(json, "props.pageProps.pageData.productBySku", JSONObject.class);
         if (data != null) {
            String internalId = String.valueOf(JSONUtils.getValue(data, "id"));
            String internalPid = JSONUtils.getStringValue(data, "sku");
            String name = JSONUtils.getStringValue(data, "name");
            // Site hasn't category
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "div.sc-pANHa.iGPRRc img", Arrays.asList("src"), "https", "img.drogasil.com.br");
            String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("#ancorDescription div > p:nth-child(1)"));
            Integer stock = null;
            List<String> ean = new ArrayList<>();
            ean.add(CrawlerUtils.scrapStringSimpleInfo(doc, "tr:nth-child(2) > td", false));
            Boolean available = JSONUtils.getValueRecursive(data,
               "extension_attributes.stock_item.is_in_stock",
               Boolean.class);
            RatingsReviews ratingsReviews = crawlRating(internalPid);
            Offers offers = available ? scrapOffers(data) : new Offers();

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setDescription(description)
               .setStock(stock)
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
      return doc.selectFirst("#ancorDescription div > p:nth-child(1)") != null;
   }


   private Offers scrapOffers(JSONObject data) {
      Offers offers = new Offers();
      try {
         Pricing pricing = scrapPricing(data);
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

      } catch (Exception e) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }
      return offers;

   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getValueRecursive(data, "price_aux.value_to", Double.class);
      Double priceFrom = JSONUtils.getValueRecursive(data, "price_aux.value_from", Double.class);

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

   private String alternativeRatingFetch(String internalPid) {

      String url = "https://trustvox.com.br/widget/root?&code=" + internalPid + "&store_id=71447&product_extra_attributes[group]=P";

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

   private RatingsReviews crawlRating(String internalPid) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      String ratingResponse = alternativeRatingFetch(internalPid);

      JSONObject rating = CrawlerUtils.stringToJson(ratingResponse);

      Double avgReviews = JSONUtils.getValueRecursive(rating, "rate.average", Double.class);
      Integer totalReviews = JSONUtils.getValueRecursive(rating, "rate.count", Integer.class);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(rating);

      ratingsReviews.setTotalRating(totalReviews);
      ratingsReviews.setTotalWrittenReviews(totalReviews);
      ratingsReviews.setAverageOverallRating(avgReviews);
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
