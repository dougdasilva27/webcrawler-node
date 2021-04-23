package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

/************************************************************************************************************************************************************************************
 * Crawling notes (23/08/2016):
 *
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 *
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 *
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 *
 * 4) The sku page identification is done simply looking for an specific html element.
 *
 * 5) In this market was not found a product with status unnavailable.
 *
 * 6) There is internalPid for skus in this ecommerce.
 *
 * 7) The primary image is the first image in the secondary images selector.
 *
 * 8) Categories in this market appear only with cookies.
 *
 * Examples: ex1 (available):
 * http://www.amoedo.com.br/ar-condicionado-split-piso-teto-komeco-60btus-kop60fc ex2 (unavailable):
 * For this market, was not found product unnavailable
 *
 * Optimizations notes: No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilAmoedoCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   private static final String SELLER_NAME_LOWER = "amoedo brasil";
   private static final String HOME_PAGE = "http://www.amoedo.com.br/";


   public BrasilAmoedoCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String internalPid = crawlInternalPid(doc);
         String name = crawlName(doc);
         List<String> images = scrapImages(doc);
         String primaryImage = images.remove(0);
         String description = crawlDescription(doc);

         boolean available = doc.select("div.container-fluid.container-aviseme-box").isEmpty();
         Offers offers = available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(Document document) {
      return document.select(".catalog-product-view").first() != null;
   }


   /*******************
    * General methods *
    *******************/

   private String crawlInternalId(Document document) {
      String internalId = null;
      Element internalIdElement = document.selectFirst("input[name=product]");

      if (internalIdElement != null) {
         internalId = internalIdElement.val().trim();
      }

      return internalId;
   }

   private String crawlInternalPid(Document document) {
      String internalId = null;
      Element internalIdElement = document.selectFirst(".product.attribute.sku .value");

      if (internalIdElement != null) {
         internalId = internalIdElement.ownText().trim();
      }

      return internalId;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.selectFirst(".page-title > span");

      if (nameElement != null) {
         name = nameElement.ownText().trim();
      }

      return name;
   }

   private List<String> scrapImages(Document doc) {
      List<String> secondaryImages = new ArrayList<>();

      JSONArray images = CrawlerUtils.crawlArrayImagesFromScriptMagento(doc, "script[type=text/x-magento-init]");

      if(!images.isEmpty()){
         for (Object e : images) {
            secondaryImages.add(e.toString());
         }
      }

      return secondaryImages;
   }

   private String crawlDescription(Document document) {
      StringBuilder description = new StringBuilder();

      Element overviewElement = document.selectFirst(".product.attribute.overview > .value");
      Element descriptionElement = document.selectFirst(".product.attribute.description > .value");

      if (overviewElement != null) {
         description.append(overviewElement.html());
      }

      if (descriptionElement != null)
         description.append(descriptionElement.html());

      return description.toString();
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>(); // When this new offer model was implemented, no sales was found

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME_LOWER)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      BankSlip bankSlip;
      CreditCards creditCards;
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-main .special-price .price, .product-info-main [data-price-type=finalPrice] .price", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-price .price", null, true, ',', session);

      bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      creditCards = scrapCreditcards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();

   }

   private CreditCards scrapCreditcards(Double installmentPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(installmentPrice);

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   public Installments scrapInstallments(Double installmentPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(installmentPrice)
         .build());

      return installments;
   }

}
