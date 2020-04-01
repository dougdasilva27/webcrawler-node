package br.com.lett.crawlernode.crawlers.corecontent.portoalegre;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class PortoalegreBichopetstoreCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.bichopetstore.com.br/";
   private static final String MAIN_SELLER_NAME = "Bicho Pet Store";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), 
         Card.ELO.toString(), Card.DINERS.toString(), Card.AMEX.toString());
   
   public PortoalegreBichopetstoreCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         Elements variations = doc.select("#product .otp-option > li");

         String internalPid = scrapInternalPid(doc);
         String internalId = internalPid;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#content .row > div > h1", true);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#otp-price > li", null, false, ',', session);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".container > .breadcrumb > li:not(:last-child) > a", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".thumbnails > li.image-additional > a.thumbnail",
               Arrays.asList("href"), "https:", HOME_PAGE);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".thumbnails > li.image-additional > a.thumbnail",
               Arrays.asList("href"), "https:", HOME_PAGE, primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("#content .col-sm-8 p", "#content .col-sm-8 h4"));
         Offers offers = scrapOffers(doc, null);
         
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

         if (!variations.isEmpty()) {
            for (Element variation : variations) {
               Product variationProduct = product.clone();

               String variationId = CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, null, "value");
               Pricing variationPricing = generateVariationPricing(scrapVariationPrice(variation));
               Offers variationOffers = scrapOffers(doc, variationPricing);

               variationProduct.setInternalId(internalId + "-" + variationId);
               variationProduct.setName(name + " - " + CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, null, "title"));
               variationProduct.setOffers(variationOffers);

               products.add(variationProduct);
            }

         } else {
            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }
   
   private Offers scrapOffers(Document doc, Pricing variationPricing) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      
      if(variationPricing == null) {
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
      } else {
         offers.add(OfferBuilder.create()
               .setUseSlugNameAsInternalSellerId(true)
               .setSellerFullName(MAIN_SELLER_NAME)
               .setSellersPagePosition(1)
               .setIsBuybox(false)
               .setIsMainRetailer(true)
               .setPricing(variationPricing)
               .build());
      }
      
      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#otp-price > li", null, false, ',', session);
      
      if(spotlightPrice != null) {
        CreditCards creditCards = scrapCreditCards(spotlightPrice);

        return PricingBuilder.create()
              .setSpotlightPrice(spotlightPrice)
              .setCreditCards(creditCards)
              .build();
      }

      return null;
   }
   
   private Pricing generateVariationPricing(Double price) throws MalformedPricingException {
      
      if(price != null) {
        CreditCards creditCards = scrapCreditCards(price);

        return PricingBuilder.create()
              .setSpotlightPrice(price)
              .setCreditCards(creditCards)
              .build();
      }

      return null;
   }
   
   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
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
   
   private boolean isProductPage(Document document) {
      return document.selectFirst("body[class*=\"product-product\"]") != null;
   }

   private String scrapInternalPid(Document doc) {
      String internalPid = null;

      String onClick = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#content .btn-default[onclick]", "onclick");
      if (onClick != null && onClick.contains("(") && onClick.contains(")")) {
         int firstIndex = onClick.indexOf('(') + 1;
         int lastIndex = onClick.indexOf(')', firstIndex);

         internalPid = onClick.substring(firstIndex, lastIndex).replace("'", "");
      }

      return internalPid;
   }

   private Double scrapVariationPrice(Element element) {
      Double price = null;

      if (element != null) {
         String elementText = element.text();

         if (elementText.contains("R$")) {
            elementText = elementText.substring(elementText.indexOf("R$"));
            elementText = elementText.replaceAll("[^0-9,]+", "").replace(".", "").replace(",", ".");

            if (!elementText.isEmpty()) {
               price = Double.parseDouble(elementText);
            }
         }
      }

      return price;
   }
}
