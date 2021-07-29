package br.com.lett.crawlernode.crawlers.corecontent.panama;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class PanamaElmachetazoCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "elmachetazo";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public PanamaElmachetazoCrawler(Session session) {
      super(session);
      // super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   protected Object fetch() {

      Map<String, String> headers = new HashMap<>();
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36");
      headers.put("authority", "elmachetazo.com");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("content-type", "application/x-www-form-urlencoded");
      headers.put("cookie", "PHPSESSID=fe0b97929478d40eefb5327ea5cb180b; _fbp=fb.1.1627501894875.1990086009; _hjid=a95ffbb1-27ad-41bd-9a3b-714c82840faf; form_key=p6lv7tmeASbWTXUO; mage-cache-storage=%7B%7D; mage-cache-storage-section-invalidation=%7B%7D; mage-cache-sessid=true; _gid=GA1.2.762555409.1627501896; mage-messages=; recently_viewed_product=%7B%7D; recently_viewed_product_previous=%7B%7D; recently_compared_product=%7B%7D; recently_compared_product_previous=%7B%7D; product_data_storage=%7B%7D; cf_clearance=3d34c651cb75924f9b055fc9fb61f440ec4b17b0-1627569451-0-150; _hjAbsoluteSessionInProgress=0; __cf_bm=d845d9074ffbe00ed0a54581d5eeb53a249818d3-1627583007-1800-AQm8H0VsuhqeNNb/Zl/06dwIkgPFAOUW8mexoAGF/wBhiiwMixMIK5t6U9hjpFeSrTXNo4cfzE8cxtZxkHgC0tpwLfWQmFWhaDkFZGohHLhYz7ao8LxGrdrXO4roDfNMEg==; _ga_GBF02SP5FB=GS1.1.1627583007.9.1.1627583008.0; _ga=GA1.2.278493691.1627501895"); //Thu Jul 28 2022 17:51:25 GMT-0300 (Brasilia Standard Time)

      Request request = Request.RequestBuilder.create().setUrl(session.getOriginalURL()).setHeaders(headers).setProxyservice(
         Arrays.asList(
            ProxyCollection.NO_PROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
         )
      ).build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-add-form input[name=\"product\"]", "value");

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title .base", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".gallery-placeholder._block-content-loading img", Arrays.asList("src"), "http", "elmachetazo.com");
         // Quando esse scraper foi feito o site não possuia imagem secundaria.
         boolean availableToBuy = doc.selectFirst(".stock.available") != null;
         Offers offers = availableToBuy ? scrapOffer(doc, internalId) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-info-main") != null;
   }

   private Offers scrapOffer(Document doc, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".price-box .discount_text");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-container.price-final_price span[data-price-type=\"finalPrice\"] .price", null, false, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-container.price-final_price span[data-price-type=\"oldPrice\"] .price", null, false, '.', session);

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
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

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
