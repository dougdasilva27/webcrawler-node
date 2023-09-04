package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SaopauloTrimaisCrawler extends Crawler {
   public SaopauloTrimaisCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.trimais.com.br/";

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      if (isProductPage(doc)) {
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[itemprop=\"sku\"]", "content");
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-form [name=\"product\"]", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.name", true);
         List<String> secondaryImages = crawlImagesArray(doc);
         String primaryImage = !secondaryImages.isEmpty() ? secondaryImages.remove(0) : null;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#breadcrumb ul li a", true);
         List<String> ean = scrapEan(doc);

         boolean isAvailable = scrapAvailability(internalPid);
         Offers offers = isAvailable ? crawlOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setEans(ean)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean scrapAvailability(String internalPid) {
      String url = "https://www.trimais.com.br/api/1.0/public/order/";
      String payload = "{\"products\":[{\"options\":[],\"product\":\"" + internalPid + "\",\"quantity\":1}]}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setPayload(payload)
         .build();

      Response response = new ApacheDataFetcher().post(session, request);
      JSONObject jsonResponse = CrawlerUtils.stringToJson(response.getBody());
      JSONObject itemObj = JSONUtils.getValueRecursive(jsonResponse, "items.0", JSONObject.class);
      return itemObj != null;
   }

   private List<String> scrapEan(Document doc) {
      List<String> eans = new ArrayList<>();
      String ean = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[itemprop=\"gtin13\"]", "content");
      if (ean != null) {
         eans.add(ean);
      }
      return eans;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#content-product") != null;
   }

   private List<String> crawlImagesArray(Document doc) {
      List<String> imagesMini = CrawlerUtils.scrapSecondaryImages(doc, ".product-image ul li img", Arrays.asList("data-src"), "https", "io.convertiez.com.br", null);
      List<String> imagesLarge = new ArrayList<>();
      for (String image : imagesMini) {
         String updatedImage = image.replaceAll("mini", "medium");
         imagesLarge.add(updatedImage);
      }
      return imagesLarge;
   }

   private Offers crawlOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("trimais")
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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-form .sale-price strong", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-form .unit-price span", null, true, ',', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
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

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.DINERS.toString(), Card.ELO.toString(), Card.HIPERCARD.toString(), Card.DISCOVER.toString());
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
      sales.add(saleDiscount);
      return sales;
   }
}
