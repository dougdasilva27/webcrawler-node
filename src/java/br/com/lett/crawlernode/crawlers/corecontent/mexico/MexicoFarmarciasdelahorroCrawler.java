package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
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
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class MexicoFarmarciasdelahorroCrawler extends Crawler {

   private static String SELLER_NAME = "Farmacias del Ahorro";
   public Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public MexicoFarmarciasdelahorroCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "www.fahorro.com");
      headers.put(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
      headers.put(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36");

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setProxyservice(List.of(
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.SMART_PROXY_MX
         ))
         .setHeaders(headers)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new ApacheDataFetcher(), new FetcherDataFetcher(), new JsoupDataFetcher()), session, "get");

      return response;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String productName = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title-wrapper.product", false);
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-info-price > .price-box", "data-product-id");
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".product.attribute.description .value", false);
         List<String> categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs");
         String primaryImage = getPrimaryImage(doc);
         List<String> secondaryImages = getImageListFromScript(doc);
         boolean available = doc.selectFirst("#product-addtocart-button") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(productName)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setCategories(categories)
            .setOffers(offers)
            .build();
         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#maincontent.page-main") != null;
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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price", null, false, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-price", null, false, '.', session);

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

   private String getPrimaryImage(Document doc) {
      String primaryImage = null;
      Element imageScript = doc.selectFirst("script:containsData(mage/gallery/gallery)");
      if (imageScript != null) {
         JSONObject imageToJson = CrawlerUtils.stringToJson(imageScript.html());
         JSONArray imageArray = JSONUtils.getValueRecursive(imageToJson, "[data-gallery-role=gallery-placeholder].mage/gallery/gallery.data", JSONArray.class, new JSONArray());
         if (imageArray != null && !imageArray.isEmpty()) {
            JSONObject object = JSONUtils.getValueRecursive(imageArray, "0", JSONObject.class);
            if (object != null && !object.isEmpty()) {
               primaryImage = object.optString("img");
            }
         }
         return primaryImage;
      }
      return null;
   }
}
