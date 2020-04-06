package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jsoup.nodes.Document;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilDogsdayCrawler extends Crawler {
   
  private static final String MAIN_SELLER_NAME = "Dog's Day";
  private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), 
        Card.ELO.toString(), Card.DINERS.toString(), Card.AMEX.toString());

  public BrasilDogsdayCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#product_id", "value");
      String internalPid = internalId;
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1:not([style])", true);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child):not(:last-child) a");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".thumbnails li a", Arrays.asList("href"), "https", "www.dogsday.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".thumbnails li a", Arrays.asList("href"), "https", "www.dogsday.com.br",
          primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#tab-description"));
      Offers offers = scrapOffers(doc);

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
          .setOffers(offers)
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }
  
  private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
     Offers offers = new Offers();
     Pricing pricing = scrapPricing(doc);

     if(pricing != null) {
       offers.add(OfferBuilder.create()
             .setUseSlugNameAsInternalSellerId(true)
             .setSellerFullName(MAIN_SELLER_NAME)
             .setSellersPagePosition(1)
             .setIsBuybox(false)
             .setIsMainRetailer(true)
             .setPricing(pricing)
             .build());
     }

     return offers;
  }
  
  private Pricing scrapPricing(Document doc) throws MalformedPricingException {
     Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span[style=\"text-decoration: line-through;\"], .list-unstyled  h2", null, false, ',', session);

     if(spotlightPrice != null) {
        Double priceFrom = null;
        CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

       return PricingBuilder.create()
             .setSpotlightPrice(spotlightPrice)
             .setPriceFrom(priceFrom)
             .setCreditCards(creditCards)
             .build();
     }

     return null;
  }
  
  private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
     CreditCards creditCards = new CreditCards();

     Installments installments = new Installments();
     installments.add(InstallmentBuilder.create()
           .setInstallmentNumber(1)
           .setInstallmentPrice(spotlightPrice)
           .build());

     for (String brand : cards) {
        creditCards.add(CreditCardBuilder.create()
              .setBrand(brand)
              .setIsShopCard(false)
              .setInstallments(installments)
              .build());
     }

     return creditCards;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst("#product_id") != null;
  }
}
