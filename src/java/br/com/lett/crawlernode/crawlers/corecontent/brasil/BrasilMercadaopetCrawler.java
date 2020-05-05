package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import models.prices.Prices;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilMercadaopetCrawler extends Crawler {
   
  private static final String MAIN_SELLER_NAME = "Mercadão Pet";
  private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString());

  public BrasilMercadaopetCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    if (isProductPage(doc)) {

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".single-product-info .add_to_wishlist[data-product-id]",
          "data-product-id");
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_meta .sku", true);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product_title", true);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs-inner a:not(:first-child)");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".images a", Arrays.asList("href"), "https",
          "mercadaopet.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".images a", Arrays.asList("href"), "https",
          "mercadaopet.com.br", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".short-description", "#tab-description",
          "#tab-additional_information"));
      Offers offers = scrapOffers(doc);
      
//      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".product-content .price", null, false, ',', session);
//      Prices prices = scrapPrices(price);

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
     
     Elements priceElements = doc.select(".product-content .price .amount");
     Element priceElement = priceElements.last();
     
     if(priceElement != null) {
       Double priceFrom = priceElements.size() > 1 ? 
             CrawlerUtils.scrapDoublePriceFromHtml(priceElements.first(), null, null, false, ',', session) : 
             null;
       Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(priceElement, null, null, false, ',', session);
  
       if(spotlightPrice != null) {
         CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
  
         return PricingBuilder.create()
               .setSpotlightPrice(spotlightPrice)
               .setPriceFrom(priceFrom)
               .setCreditCards(creditCards)
               .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
               .build();
       }
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

  private Prices scrapPrices(Float price) {
    Prices prices = new Prices();
    if (price != null) {

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      prices.setBankTicketPrice(price);
      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

    }
    return prices;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product_meta .sku").isEmpty();
  }
}
