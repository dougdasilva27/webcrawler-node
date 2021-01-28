package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArgentinaSupermercadolaanonimaonlinecipollettiCrawler extends Crawler {
   private static final String HOME_PAGE = "https://supermercado.laanonimaonline.com/";
   private static final String SELLER_FULL_NAME = "La anonima";


   public ArgentinaSupermercadolaanonimaonlinecipollettiCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public void handleCookiesBeforeFetch() {

      BasicClientCookie cookie = new BasicClientCookie("laanonimasucursalnombre", "CIPOLLETTI");
      cookie.setDomain("www.laanonimaonline.com");
      cookie.setPath("/");
      this.cookies.add(cookie);


      BasicClientCookie cookie2 = new BasicClientCookie("laanonimasucursal", "22");
      cookie2.setDomain("www.laanonimaonline.com");
      cookie2.setPath("/");
      this.cookies.add(cookie2);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#id_item", "value");
         String internalPid = crawlInternalPid(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".titulo_producto.principal", true);
         boolean available = crawlAvailability(doc);
         Offers offers = available ? crawlOffers(doc) : new Offers();
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = crawlPrimaryImage(doc);
         List<String> secondaryImages = crawlSecondaryImages(doc, primaryImage);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers crawlOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = crawPricing(doc);

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

   private Pricing crawPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = crawlPrice(doc);


      CreditCards creditCards = new CreditCards();
      Installments  installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentPrice(spotlightPrice)
         .setInstallmentNumber(1)
         .build());


      creditCards.add(CreditCard.CreditCardBuilder.create()
         .setBrand(Card.MASTERCARD.toString())
         .setInstallments(installments)
         .setIsShopCard(false)
         .build());


      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#id_item") != null;
   }


   private String crawlInternalPid(Document doc) {
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div[id*=prod_]", "id");
      return CommonMethods.getLast(internalPid.split("prod_"));
   }


   private Double crawlPrice(Document doc) {
      return CrawlerUtils.scrapDoublePriceFromHtml(doc, ".precio.destacado", null, false, ',', session);
   }

   private boolean crawlAvailability(Document doc) {
      String style = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div[id*=btnagregarcarritosinstock]", "style");
      return style != null && style.contains("display:none");
   }


   private String crawlPrimaryImage(Document doc) {
      return CrawlerUtils.scrapSimplePrimaryImage(doc, "#img_producto img", Arrays.asList("src"), "https:", "d1on8qs0xdu5jz.cloudfront.net");
   }

   private List<String> crawlSecondaryImages(Document doc, String primaryImage) {
      return CrawlerUtils.scrapSecondaryImages(doc, "#galeria_img div a", Arrays.asList("href"), "https:", "d1on8qs0xdu5jz.cloudfront.net", primaryImage);
   }

   private CategoryCollection crawlCategories(Document doc) {
      CategoryCollection categories = new CategoryCollection();

      Elements elementCategories = doc.select(".barra_navegacion a.link_navegacion");
      for (Element e : elementCategories) {
         categories.add(e.text().trim());
      }

      return categories;
   }





}
