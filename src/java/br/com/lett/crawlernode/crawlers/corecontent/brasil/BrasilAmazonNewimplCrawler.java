package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.AmazonScraperUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class BrasilAmazonNewimplCrawler extends Crawler {
   private static final String IMAGES_HOST = "images-na.ssl-images-amazon.com";
   private static final String IMAGES_PROTOCOL = "https";
   private String sellerName = this.session.getOptions().optString("seller", "");

   public Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilAmazonNewimplCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
      Document doc;
      Response response;

      boolean success;
      int tries = 1;

      do {
         Request request = Request.RequestBuilder.create()
            .setUrl(session.getOriginalURL())
            .setCookies(cookies)
            .setSendUserAgent(true)
            .build();

         response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher()), session);

         doc = Jsoup.parse(response.getBody());
         String seller = CrawlerUtils.scrapStringSimpleInfo(doc, "#tabular-buybox .tabular-buybox-text[tabular-attribute-name=\"Vendido por\"] span", false);
         if (seller == null) {
            seller = CrawlerUtils.scrapStringSimpleInfo(doc, "#tabular-buybox-truncate-1 .a-truncate-full .tabular-buybox-text", false);
         }

         success = seller != null && !seller.isEmpty();

         if (success) {
            Logging.printLogInfo(logger, session, "HTML has seller!");
         } else {
            Logging.printLogError(logger, session, "HTML not have seller. Attempt: " + tries);
         }

      } while (!success && tries++ <= 4);


      return response;
   }

   private final AmazonScraperUtils amazonScraperUtils = new AmazonScraperUtils(logger, session);

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc) && getSellerName(doc).equalsIgnoreCase(sellerName)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = amazonScraperUtils.crawlInternalId(doc);
         String name = amazonScraperUtils.crawlName(doc);
         CategoryCollection categories = amazonScraperUtils.crawlCategories(doc);

         JSONArray images = this.amazonScraperUtils.scrapImagesJSONArray(doc);
         String primaryImage = this.amazonScraperUtils.scrapPrimaryImage(images, doc, IMAGES_PROTOCOL, IMAGES_HOST);
         List<String> secondaryImages = this.amazonScraperUtils.scrapSecondaryImages(images, IMAGES_PROTOCOL, IMAGES_HOST);

         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("#productDetails_feature_div", "#productDescription_feature_div.celwidget", "#featurebullets_feature_div.celwidget", ".a-normal.a-spacing-micro tbody", ".aplus-v2.desktop.celwidget"));
         List<String> eans = amazonScraperUtils.crawlEan(doc);
         boolean available = doc.selectFirst(".a-section.a-spacing-small.a-text-center > span.a-color-price") != null;
         Offers offers = !available ? scrapOffers(doc) : new Offers();

         RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
         ratingReviewsCollection.addRatingReviews(amazonScraperUtils.crawlRating(doc, internalId));
         RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(internalId);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setEans(eans)
            .setRatingReviews(ratingReviews)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   public boolean isProductPage(Document doc) {
      return doc.select("#dp").first() != null;
   }

   private String getSellerName(Document doc) {
      String seller = CrawlerUtils.scrapStringSimpleInfo(doc, "#tabular-buybox .tabular-buybox-text[tabular-attribute-name=\"Vendido por\"] span", false);
      if (seller == null) {
         seller = CrawlerUtils.scrapStringSimpleInfo(doc, "#tabular-buybox-truncate-1 .a-truncate-full .tabular-buybox-text", false);
      }

      return seller;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.sellerName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".a-price span", null, false, ',', session);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".a-price .a-offscreen", null, false, ',', session);
      }
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.a-section.a-spacing-small.aok-align-center > span > span.aok-relative > span > span > span[aria-hidden=\"true\"]", null, false, ',', session);

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
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
