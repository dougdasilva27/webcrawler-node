package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.YotpoRatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ColombiaAlkostoCrawler extends Crawler {
   private static final String SELLER_FULL_NAME = "Alkosto";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   public ColombiaAlkostoCrawler(Session session) {
      super(session);

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name__sku-code span.code", true);

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product-name__name", true);
         List<String> images = scrapImages(doc);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;

         //Site hasn't categories
         String description = crawlDescription(doc);

         //The availability is defined by location. Without setting the location we cannot find the availability.
         boolean available = true; //I didn't find any product unavailable to test
         Offers offers = available ? scrapOffers(doc) : new Offers();
         RatingsReviews ratingReviews = scrapRating(doc, internalId);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setOffers(offers)
            .setRatingReviews(ratingReviews)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-main-info") != null;
   }

   private List<String> scrapImages(Document doc) {
      List<String> imagesList = new ArrayList<>();

      Elements el = doc.select("div.image-gallery__image img.js-zoom-desktop");

      if (el != null && !el.isEmpty()) {
         el.forEach(img -> imagesList.add("https://www.alkosto.com" + img.attr("data-zoom-image")));
      }

      return imagesList;
   }

   private String crawlDescription(Document doc) {
      String description = "";

      Element el = doc.selectFirst("div.row div.tab-details__outer-content");
      if (el != null) {
         description = el.toString();
      }

      return description;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setSales(sales)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.product-price-pickup", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.before-price-pickup", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String saleDiscount = CrawlerUtils.calculateSales(pricing);

      if (saleDiscount != null) {
         sales.add(saleDiscount);
      }

      return sales;
   }

   private RatingsReviews scrapRating(Document doc, String internalPid) {
      String url = "https://staticw2.yotpo.com/batch/" + fetchAppKey(doc) + "/" + internalPid;

      YotpoRatingReviewCrawler yotpo = new YotpoRatingReviewCrawler(session, cookies, logger);
      Document apiDoc = yotpo.extractRatingsFromYotpo(fetchAppKey(doc), dataFetcher, getPayload(internalPid), url);

      return yotpo.scrapRatingYotpo(apiDoc);
   }

   private String getPayload(String internalPid) {
      return "[{\"method\":\"main_widget\",\"params\":{\"pid\":\"" + internalPid + "\",\"order_metadata_fields\":{},\"index\":0,\"element_id\":\"1\"}}," +
         "{\"method\":\"bottomline\",\"params\":{\"pid\":\"" + internalPid + "\",\"link\":\""
         + this.session.getOriginalURL() + "\",\"skip_average_score\":false,\"main_widget_pid\":\"" + internalPid + "\",\"index\":1,\"element_id\":2}}]";
   }


   private String fetchAppKey(Document doc) {
      return CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#yotpoTotalReviews", "data-appkey");
   }
}
