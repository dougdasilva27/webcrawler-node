package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offers;
import models.prices.Prices;
import models.pricing.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.event.DocumentEvent;
import java.util.*;

public class MexicoJustoCrawler extends Crawler {
   private static final String HOME_PAGE = "https://justo.mx/";
   private static final String POSTAL_CODE = "14300";
   private static final String SELLER_FULL_NAME = "Justo";

   public MexicoJustoCrawler(Session session) {
      super(session);
   }

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(), Card.DINERS.toString());

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("postal_code", POSTAL_CODE);
      cookie.setDomain("justo.mx");
      cookie.setPath("/");
      this.cookies.add(cookie);
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

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".shopping-list__content", "data-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc,".product-info__name", false);;
         boolean isAvailable = crawlAvailability(doc);
         CategoryCollection categories = crawlCategories(doc);
         List<String> images = crawlImages(doc);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         String description = crawlDescription(doc);
         Offers offers = isAvailable ? crawlOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
      }else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product-details").isEmpty();
   }

   private Offers crawlOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      if(pricing != null){
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double[] prices = scrapPrices(doc);
      Double priceFrom = prices[0];
      Double spotlightPrice = prices[1];

      if(spotlightPrice != null){
         CreditCards creditCards = scrapCreditCards(spotlightPrice);
         BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();

         return Pricing.PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();
      }else{
         return null;
      }
   }

   private Double[] scrapPrices(Document doc){
      Double spotlightPrice;
      Double priceFrom;

      if(doc.select(".product-info__discount").isEmpty()){
         priceFrom = null;
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info__price", null, false, '.', session);
      }else{
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info__discount", null, false, '.', session);
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info__price", null, false, '.', session);
      }

      return new Double[]{priceFrom, spotlightPrice};
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

   private boolean crawlAvailability(Document doc){
      return !doc.select(".product-info__price").isEmpty();
   }

   private CategoryCollection crawlCategories(Document doc){

      CategoryCollection categoryCollection = CrawlerUtils.crawlCategories(doc, ".breadcrumbs.list-unstyled span:not(:last-child)", false);

      categoryCollection.removeIf(c -> c.equals(""));

      return categoryCollection;
   }

   private List<String> crawlImages(Document doc){
      Elements imageElements = doc.select(".product-image img");
      List<String> imageList = new ArrayList<>();

      for (Element element: imageElements) {
         imageList.add(element.attr("data-src"));
      }

      return imageList;
   }

   private String crawlDescription(Document doc){
      return CrawlerUtils.scrapStringSimpleInfo(doc, ".product__info-description-text", false);
   }

}
