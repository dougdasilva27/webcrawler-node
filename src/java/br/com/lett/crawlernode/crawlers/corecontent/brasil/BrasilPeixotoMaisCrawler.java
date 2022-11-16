package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class BrasilPeixotoMaisCrawler extends Crawler {

   public BrasilPeixotoMaisCrawler(Session session) {
      super(session);
   }

   private String SELLER_NAME = "PeixotoMais";
   public Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<Product>();
      Element product = doc.selectFirst(".column.main");
      if (product != null) {
         List<String> categories = getCategories(doc);
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".price-box.price-final_price", "data-product-id");
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".social-addthis", "data-media");
         List<String> secondaryImages = getImageListFromScript(doc);
         String description = CrawlerUtils.scrapStringSimpleInfo(product, ".data.item.content", false);
         String name = CrawlerUtils.scrapStringSimpleInfo(product, ".page-title", false);
         boolean available = doc.selectFirst(".stock.available") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();
         Product newProduct = ProductBuilder.create()
            .setInternalId(internalId)
            .setUrl(this.session.getOriginalURL())
            .setCategories(categories)
            .setOffers(offers)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .build();
         products.add(newProduct);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private List<String> getCategories(Document doc) {
      List<String> categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs .items > li", true);
      if (categories != null && categories.size() > 0) {
         categories.remove(categories.size() - 1);
         return categories.size() > 0 ? categories : new ArrayList<>();
      }
      return new ArrayList<>();
   }

   private List<String> getImageListFromScript(Document doc) {
      Element imageScript = doc.selectFirst("script:containsData(mage/gallery/gallery)");
      if (imageScript != null) {
         JSONObject imageToJson = CrawlerUtils.stringToJson(imageScript.html());
         JSONArray imageArray = JSONUtils.getValueRecursive(imageToJson, "[data-gallery-role=gallery-placeholder].mage/gallery/gallery.data", JSONArray.class, new JSONArray());
         List<String> imagesList = new ArrayList<>();
         for (int i = 1; i < imageArray.length(); i++) {
            String imageList = JSONUtils.getValueRecursive(imageArray, i + ".img", String.class);
            imagesList.add(imageList);
         }
         return imagesList;
      }
      return null;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-main-content .old-price .price-wrapper", "data-price-amount", false, '.', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc,
         ".price-box.price-final_price .price-container .price-wrapper", "data-price-amount", false, '.', session);

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
