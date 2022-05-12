package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
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
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilMenonatacadistaCrawler extends Crawler {

   private final String HOME_PAGE = "https://www.menonatacadista.com.br/";
   private static final String SELLER_FULL_NAME = "Menon Atacadista";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString(), Card.AMEX.toString(), Card.DINERS.toString(), Card.DISCOVER.toString(), Card.JCB.toString(), Card.AURA.toString(), Card.HIPERCARD.toString());

   public BrasilMenonatacadistaCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
   }

   private String cookiePHPSESSID = null;

   @Override
   public void handleCookiesBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("authority", "www.menonatacadista.com.br");
      String payloadString = "email=paulo.carvalho%40mdlz.com&password=c9d59";

      Request request = Request.RequestBuilder.create().setUrl("https://www.menonatacadista.com.br/index.php?route=account/login").setPayload(payloadString).setFollowRedirects(false).setHeaders(headers).setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.BONANZA, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY)).build();

      Response response = new FetcherDataFetcher().post(session, request);

      List<Cookie> cookiesResponse = response.getCookies();

      for (Cookie cookieResponse : cookiesResponse) {
         if (cookieResponse.getName().equalsIgnoreCase("PHPSESSID")) {
            this.cookiePHPSESSID = cookieResponse.getValue();
         }
      }
   }

   @Override
   protected Response fetchResponse() {

      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "PHPSESSID=" + this.cookiePHPSESSID + ";");

      Request request = Request.RequestBuilder.create().setUrl(session.getOriginalURL()).setHeaders(headers).setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.BONANZA, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY)).setHeaders(headers).build();

      return new FetcherDataFetcher().get(session, request);
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String code = CrawlerUtils.scrapStringSimpleInfo(doc, "div.product-detail-info small", true);
         String internalId = getcodeId(code);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product-title", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "ul.breadcrumb a", false);
         JSONArray imagesArray = CrawlerUtils.crawlArrayImagesFromScriptMagento(doc);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "a.thumbnail img", Arrays.asList("src"), "https", "www.menonatacadista.com.br");
         //List<String> secondaryImage = CrawlerUtils.scrapSecondaryImagesMagentoList(imagesArray, primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("div.text"));
         boolean availableToBuy = doc.select("button[id=button-out]").isEmpty();
         Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalId).setName(name).setCategories(categories).setPrimaryImage(primaryImage).setOffers(offers).setDescription(description)

            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("div.product-detail-info") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create().setUseSlugNameAsInternalSellerId(true).setSellerFullName(SELLER_FULL_NAME).setMainPagePosition(1).setSales(sales).setIsBuybox(false).setIsMainRetailer(true).setPricing(pricing).build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.product-price .price", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span[class*=Information_discounted]", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create().setSpotlightPrice(spotlightPrice).setPriceFrom(priceFrom).setCreditCards(creditCards).build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create().setInstallmentNumber(1).setInstallmentPrice(spotlightPrice).build());


      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create().setBrand(card).setInstallments(installments).setIsShopCard(false).build());
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

   private String getcodeId(String code) {
      String aux = "";

      if (code != null) {
         final String regex = "(\\d+)";
         final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         final Matcher matcher = pattern.matcher(code);

         if (matcher.find()) {
            aux = matcher.group(0);
            return aux;
         }
      }
      return null;
   }
}
