package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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

/**
 * Date: 2019-08-28
 * 
 * @author gabriel
 *
 */
public abstract class ArgentinaCarrefoursuper extends Crawler {

   public ArgentinaCarrefoursuper(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   private static final String HOST = "supermercado.carrefour.com.ar";
   private static final String SELLER_FULL_NAME = "Carrefoursuper";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   /**
    * This function might return a cep from specific store
    * 
    * @return
    */
   protected abstract String getCep();

   @Override
   protected Object fetch() {
      Request request = RequestBuilder.create()
            .setCookies(cookies)
            .setUrl(session.getOriginalURL())
            .mustSendContentEncoding(false)
            .build();

      Response response = dataFetcher.get(session, request);

      String html = Normalizer.normalize(response.getBody(), Normalizer.Form.NFD);
      return Jsoup.parse(html);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

      String payload = "codigo_postal=" + getCep();

      Request request = RequestBuilder.create()
            .setUrl("https://supermercado.carrefour.com.ar/stock/")
            .setCookies(cookies)
            .setPayload(payload)
            .setHeaders(headers)
            .setProxyservice(Arrays.asList(ProxyCollection.STORM_RESIDENTIAL_US, ProxyCollection.INFATICA_RESIDENTIAL_BR))
            .setStatusCodesToIgnore(Arrays.asList(302))
            .setFollowRedirects(false)
            .setBodyIsRequired(false)
            .mustSendContentEncoding(false)
            .build();

      List<Cookie> cookiesResponse = new FetcherDataFetcher().post(session, request).getCookies();
      for (Cookie c : cookiesResponse) {
         BasicClientCookie cookie = new BasicClientCookie(c.getName(), c.getValue());
         cookie.setDomain(HOST);
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name .h1", false);
         CategoryCollection categories = new CategoryCollection();

         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "div.product-image .gallery-image[data-zoom-image]", Arrays.asList(
               "data-zoom-image", "src"), "https:", HOST);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "div.product-image .gallery-image[data-zoom-image]", Arrays.asList(
               "data-zoom-image", "src"), "https:", HOST, primaryImage);

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=product]", "value");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".descripcion-texto", ".descripcion-content.clearfix", ".especificaciones-wrapper h2", ".especificaciones-wrapper ul > li"));
         boolean availableToBuy = doc.selectFirst(".info-y-galleria-wrapper .btn.btn-add") != null;
         Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setName(name)
               .setOffers(offers)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-view") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      List<String> sales = scrapSales(doc);
      Pricing pricing = scrapPricing(doc);

      offers.add(OfferBuilder.create()
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

      String firstSales = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-shop .offer", false);
      String secondSales = CrawlerUtils.scrapStringSimpleInfo(doc, ".price-info .price.precio-oferta-productos-destacados", true);

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      if (secondSales != null && !secondSales.isEmpty()) {
         sales.add(secondSales);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-info .precio-regular-productos-destacados, .price-info .regular-price", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return PricingBuilder.create()
            .setPriceFrom(null)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .build();

   }

   private CreditCards scrapCreditCards(Double spotligthPrice) throws MalformedPricingException {

      CreditCards creditCards = new CreditCards();
      Installments installments = scrapInstallment(spotligthPrice);

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;

   }

   private Installments scrapInstallment(Double spotlightPrice) throws MalformedPricingException {

      Installments installments = new Installments();

      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());

      return installments;
   }

}
