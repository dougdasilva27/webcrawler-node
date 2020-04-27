package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilEsalpetCrawler extends Crawler {

   private static final String IMAGE_HOST = "assets.xtechcommerce.com";
   private static final String SELLER_FULL_NAME = "Esalpet";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilEsalpetCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".prod-action [name=id]", "value");
         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "var productvariants_settings_" + internalId + " = ", ";", false, false);

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[name=product_sku]", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1[itemprop=name]", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > li:not(:last-child)", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".prod-image #zoom", Arrays.asList("data-zoom-image", "src"), "https", IMAGE_HOST);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".prod-image-thumbs > a", Arrays.asList("data-zoom-image", "data-image"), "https", IMAGE_HOST, primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".prod-excerpt", ".prod-description"));
         Integer stock = json.has("overall_quantity") && json.get("overall_quantity") instanceof Integer ? json.getInt("overall_quantity") : 0;
         boolean available = stock > 0 || ((json.has("allow_os_purchase") && json.get("allow_os_purchase") instanceof Boolean) && json.getBoolean("allow_os_purchase"));
         RatingsReviews ratingsReviews = scrapRating(internalId, doc);
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
               .setStock(stock)
               .setRatingReviews(ratingsReviews)
               .setOffers(offers)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#product") != null;
   }

   private RatingsReviews scrapRating(String internalId, Document doc) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "109465", logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();

      offers.add(OfferBuilder.create()
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

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product_price", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      BankSlip bankSlip = BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();

      return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }

}
