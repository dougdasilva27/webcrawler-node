package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.PricingBuilderDsl;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offers;
import models.prices.Prices;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilServnutriCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.servnutri.com.br/";
   private static final String SELLER_NAME = "ServNutri Brasil";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilServnutriCrawler(Session session) {
      super(session);
   }


   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      if(isProductpage(document)){
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document,"div[itemscope][data-productid]","data-productid");
         String internalPId= internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(document ,"[itemprop=\"name\"]",true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(document,".picture img",Arrays.asList("src"),"http",HOME_PAGE);
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(document, ".picture-thumbs a img", Arrays.asList("data-fullsize"), "http", HOME_PAGE, primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(document, Arrays.asList(".full-description"));
         CategoryCollection category = CrawlerUtils.crawlCategories(document,".breadcrumb li",true);
         boolean available = !document.select(".product-add-button").isEmpty();
         Offers offers = available? scrapOffers(document):new Offers();

         products.add(ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .setCategories(category)
            .build());
      }
      else{
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }


      return products;
   }

   private Offers scrapOffers(Document document) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();


      Pricing pricing = scrapPricing(document);
      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());
      return offers;
   }

   private Pricing scrapPricing(Document document) throws MalformedPricingException {

      Double pricingFrom = CrawlerUtils.scrapDoublePriceFromHtml(document,".old-product-price",null,true,',',session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(document,"span[class*=\"price-value\"]",null,true,',',session);

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentPrice(spotlightPrice)
         .setInstallmentNumber(1)
         .build());
      CreditCards creditCards = new CreditCards();

      for (String s : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setIsShopCard(false)
            .setBrand(s)
            .setInstallments(installments)
            .build());
      }


      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(pricingFrom)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build())
         .build();
   }

   private boolean isProductpage(Document document) {
      return !document.select(".product-details-page").isEmpty();
   }
}
