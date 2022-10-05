package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;

public class MrestoqueunidasulCrawler extends Crawler {
   private static final String SELLER_FULL_NAME = "Mr Estoque Unidasul";
   private static final List<String> cards = Arrays.asList(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AMEX.toString(), Card.HIPERCARD.toString(), Card.DINERS.toString(), Card.VISA.toString());

   public MrestoqueunidasulCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);
      super.config.setParser(Parser.HTML);
   }

   @Override
   protected Response fetchResponse() {
      String login = session.getOptions().optString("login");
      String password = session.getOptions().optString("password");

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/x-www-form-urlencoded");
      headers.put("authority", "www.mrestoque.com.br");

      String payload = "username=" + login + "&password=" + password;


      Request requestLogin = Request.RequestBuilder.create()
         .setUrl("https://www.mrestoque.com.br/cliente/entrar")
         .setHeaders(headers)
         .setPayload(payload)
         .setFollowRedirects(false)
         .setCookies(cookies)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.SMART_PROXY_BR,
            ProxyCollection.LUMINATI_SERVER_BR
         ))
         .build();

      Response responseLogin = this.dataFetcher.post(session, requestLogin);
      this.cookies = responseLogin.getCookies();


      Request requestLogin2 = Request.RequestBuilder.create()
         .setUrl("https://www.mrestoque.com.br" + responseLogin.getHeaders().get("location"))
         .setFollowRedirects(false)
         .setCookies(cookies)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.SMART_PROXY_BR,
            ProxyCollection.LUMINATI_SERVER_BR
         ))
         .build();

      Response responseLogin2 = this.dataFetcher.get(session, requestLogin2);

      Request requestLogin3 = Request.RequestBuilder.create()
         .setUrl("https://www.mrestoque.com.br" + responseLogin2.getHeaders().get("location"))
         .setFollowRedirects(false)
         .setCookies(cookies)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.SMART_PROXY_BR,
            ProxyCollection.LUMINATI_SERVER_BR
         ))
         .build();

      Response responseLogin3 = this.dataFetcher.get(session, requestLogin3);

      Request requestProduct = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setCookies(cookies)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.SMART_PROXY_BR,
            ProxyCollection.LUMINATI_SERVER_BR
         ))
         .build();

      Response response = CrawlerUtils.retryRequest(requestProduct, session, this.dataFetcher, true);

      return response;
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 20);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(
            logger, session, "Product page identified: " + session.getOriginalURL());

         String internalId = scrapInternalId(doc);
         String internalPid = scrapPid(doc);
         String name = scrapName(doc);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li:not(:nth-child(2)):not(:first-child) a");
         List<String> images = scrapImage(doc);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".grid-descriptions"));
         boolean available = !doc.select("a.js-buy-items").isEmpty();
         Offers offers = available ? scrapOffers(doc) : new Offers();
         List<String> eans = scrapEan(doc);

         Product product =
            ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategories(categories)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setEans(eans)
               .setOffers(offers)
               .setDescription(description)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   private String scrapName(Document doc) {
      String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product__images__grid img", "alt");

      if (name == null || name.isEmpty()) {
         name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-data h1", true);
      }

      return name;
   }

   private String scrapInternalId(Document doc) {
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product__grid .product__button", "data-product");

      if (internalId == null || internalId.isEmpty()) {
         String productSlug = CommonMethods.getLast(session.getOriginalURL().split("/"));
         if (productSlug != null && !productSlug.isEmpty()) {
            String alternativeInternalId = CommonMethods.getLast(productSlug.split("-"));
            if (alternativeInternalId != null && !alternativeInternalId.isEmpty()) {
               internalId = alternativeInternalId;
            }
         }
      }

      return internalId;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("#product-detail").isEmpty();
   }

   private List<String> scrapEan(Document doc) {
      String ean;
      List<String> eans = new ArrayList<>();
      Elements especifications = doc.select(".grid-descriptions .specs p");
      for (Element e : especifications) {
         String spec = CrawlerUtils.scrapStringSimpleInfo(e, ".font--bold", true);
         if (spec != null && spec.contains("Barras")) {
            ean = e.ownText();
            if (ean != null) {
               eans.add(ean.replace(":", "").trim());
            }
            break;
         }
      }
      return eans;
   }

   private String scrapDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Elements specificationsTittle = doc.select(".technical-specifications dt");
      Elements specificationsValue = doc.select(".technical-specifications dd");

      if (specificationsTittle != null && specificationsValue != null) {
         for (Element specification : specificationsTittle) {
            description.append(specification);
            for (Element specificationValue : specificationsValue) {
               description.append(specificationValue);
               specificationsValue.remove(0);
               break;
            }
         }
      }

      return description.toString();
   }

   private List<String> scrapImage(Document doc) {
      List<String> images = new ArrayList<>();
      Elements imagesElements = doc.select(".product-images li");
      for (Element element : imagesElements) {
         String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, "img", "src");
         String url = CrawlerUtils.completeUrl(imageUrl, "https", "www.mrestoque.com.br");
         images.add(url);
      }
      return images;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
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

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = scrapPrice(doc);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
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

   private Double scrapPrice(Document doc) {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.price span.flex", null, true, ',', session);

      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.price strong", null, true, ',', session);
      }

      return spotlightPrice;
   }

   private String scrapPid(Document doc) {
      String totalProducts = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-data .color--base-30", true);

      return totalProducts.replaceAll("[^0-9]", "");
   }
}

