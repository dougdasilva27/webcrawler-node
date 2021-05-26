package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Date: 09/07/2019
 *
 * @author Gabriel Dornelas
 */
public class KochCrawler extends Crawler {

   private String storeId;

   public String getStoreId() {
      return storeId;
   }

   public void setStoreId(String storeId) {
      this.storeId = storeId;
   }

   private static final String SELLER_FULLNAME = "Koch";
   private static final List<String> cards = Arrays.asList(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.ELO.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   public KochCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {

      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", "customer_website=website_lj" + storeId);

      Request req = RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .build();

      String content = this.dataFetcher.get(session, req).getBody();


      return Jsoup.parse(content);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".product.attribute .value", true);
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".price-box.price-final_price", "data-product-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title span", true);
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".gallery-placeholder img", "src");
         String description = description(doc);
         boolean available = !doc.select(".stock.available").isEmpty(); //I didn't find any product unavailable to test
         Offers offers = available ? scrapOffers(doc) : new Offers();
         List<String> eans = new ArrayList<>();
         eans.add(CrawlerUtils.scrapStringSimpleInfo(doc, "#product-attribute-specs-table > tbody > tr:nth-child(2) > td", true));

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-info-main") != null;
   }

   private String description(Document doc) {
      StringBuilder description = new StringBuilder();
      Elements elements = doc.select("tbody tr");
      for (Element element : elements) {
         description.append(CrawlerUtils.scrapStringSimpleInfo(element, ".col.label", true));
         description.append(": ");
         description.append(CrawlerUtils.scrapStringSimpleInfo(element, ".col.data", true));
         description.append(" | ");

      }

      return description.toString();
   }


   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);


      offers.add(new OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULLNAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }


   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String sale = CrawlerUtils.calculateSales(pricing);

      if (sale != null) {
         sales.add(sale);
      }

      return sales;
   }


   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double price = getSpotlighPrice(doc);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-price .price", null, true, ',', session);

      CreditCards creditCards = scrapCreditCards(price);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .build());


      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private Double getSpotlighPrice(Document doc) {
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".special-price .price", null, true, ',', session);

      if (price == null) {
         price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-price .price", null, true, ',', session);

      }

      return price;
   }

   private RatingsReviews scrapRatingReviews(JSONObject api) {

      RatingsReviews ratingsReviews = new RatingsReviews();
      JSONArray avaliacoes = api.optJSONArray("Avaliacoes");

      if (avaliacoes != null && !avaliacoes.isEmpty()) {

         int totalReviews = avaliacoes.length();
         int totalValueReviews = 0;
         ratingsReviews.setTotalRating(totalReviews);
         ratingsReviews.setTotalWrittenReviews(totalReviews);
         for (Object e : avaliacoes) {

            totalValueReviews += ((JSONObject) e).optInt("int_nota_review");
         }
         ratingsReviews.setAverageOverallRating((double) totalValueReviews / totalReviews);
         ratingsReviews.setAdvancedRatingReview(scrapAdvancedRatingReview(avaliacoes));
      } else {
         ratingsReviews.setTotalRating(0);
         ratingsReviews.setTotalWrittenReviews(0);
         ratingsReviews.setAverageOverallRating(0.0);
      }

      return ratingsReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONArray avaliacoes) {

      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();

      for (Object e : avaliacoes) {

         int reviewValue = ((JSONObject) e).optInt("int_nota_review");

         switch (reviewValue) {
            case 1:
               advancedRatingReview.setTotalStar1(reviewValue);
               break;
            case 2:
               advancedRatingReview.setTotalStar2(reviewValue);
               break;
            case 3:
               advancedRatingReview.setTotalStar3(reviewValue);
               break;
            case 4:
               advancedRatingReview.setTotalStar4(reviewValue);
               break;
            case 5:
               advancedRatingReview.setTotalStar5(reviewValue);
               break;
            default:
         }
      }

      return advancedRatingReview;
   }
}
