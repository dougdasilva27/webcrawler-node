package br.com.lett.crawlernode.crawlers.corecontent.equador;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class EquadorFrecuentoCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Frecuento";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DISCOVER.toString(), Card.DINERS.toString(), Card.AMEX.toString());

   public EquadorFrecuentoCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {

      List<Product> products = new ArrayList<>();

      if (isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String code = CrawlerUtils.scrapStringSimpleInfo(document, ".code", true);
         String name =CrawlerUtils.scrapStringSimpleInfo(document, ".name", true);
         String internalId = code != null ? code.replaceAll("[^0-9]", "") : null;
         String internalPid = internalId;
         CategoryCollection categories = CrawlerUtils.crawlCategories(document, ".breadcrumb", true);

         Elements images = document.select("#imageGallary div > img[id*=Zoom]");

         String primaryImage = (images != null && !images.isEmpty()) ? images.remove(0).attr("data-src") : null;
         List<String> secondaryImages = crawlSecondaryImages(images);
         String description = CrawlerUtils.scrapSimpleDescription(document, Arrays.asList(".tab-details", ".product-classifications"));
         boolean availableToBuy = document.selectFirst(".btn-add-to-cart") != null;
         Offers offers = availableToBuy ? scrapOffer(document) : new Offers();

         if (document.selectFirst("#variant") != null) {
            Elements variants = document.select("#variant option[value]");
            internalPid = CommonMethods.getLast(session.getOriginalURL().split("/"));
            for (Element element : variants) {
               internalId =  element.attr("value");
               String nameVariant = name + " " + element.text();

               // Creating the product
               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(nameVariant)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setOffers(offers)
                  .build();

               products.add(product);

            }


         }else {
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

         }


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Element doc) {
      return doc.selectFirst(".product-details") != null;
   }

   private List<String> crawlSecondaryImages(Elements images) {
      List<String> imageList = new ArrayList<>();
      for (Element element : images) {
         imageList.add(element.attr("src"));
      }
      return imageList;
   }


   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".first_price_discount_container");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Double scrapSpotlightPrice(Document doc) {
      Double price = null;
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price_regular_precio,.atg_store_newPrice,.atg_store_oldPrice.price_regular", null, false, ',', session);
      Double priceDiscount = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".first_price_discount_container .price_discount", null, false, '.', session);
      if (priceDiscount != null) {
         price = priceDiscount;
      } else {
         price = spotlightPrice;
      }
      return price;
   }

   private Double scrapPriceFrom(Document doc, Double spotlightPrice) {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price_regular_precio", null, false, '.', session);
      if (spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }
      return priceFrom;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-details .price", null, true, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-details .discount", null, true, '.', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
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
}
