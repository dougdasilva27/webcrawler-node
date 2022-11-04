package br.com.lett.crawlernode.crawlers.corecontent.campinas;

import br.com.lett.crawlernode.core.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
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
import br.com.lett.crawlernode.util.CommonMethods;
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
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.*;

public class CampinasDiscampCrawler extends Crawler {
   public CampinasDiscampCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   //ESTE SITE NÃO POSSUI IMAGEM SECUNDÁRIA NO DIA DA CRIAÇÃO DO CRAWLER
   //ESTE SITE NÃO POSSUI DESCRIÇÃO NO DIA DA CRIAÇÃO DO CRAWLER
   private final String SELLER_NAME = "discamp";
   private String homePage = "https://loja.vrsoft.com.br/discamp/produto/570/";

   // WEBDRIVER  protected CrawlerWebdriver webdriver;
   @Override
   protected Response fetchResponse() {
      String url = "https://api.vrconnect.com.br/loja-virtual/browser/v1.05/detalheProduto";
      String payload = "{\"cabecalho\":{\"loja\":\"570\"},\"parametros\":{\"codigo\":" + getUrl() + "}}";
      Map<String, String> headers = new HashMap<>();
      String token = "Bearer uoteK1SjWL0LNH5lsgWUJUobNqIjp6kivELR0rbBroGfa73WvLg1itsS2wuU2umjvyfAbbooMIf6Kt542ZPcw0upBM7dJ20UxV2o";
      headers.put("origin", "https://loja.vrsoft.com.br");
      headers.put("Authorization", token);
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("authority", "api.vrconnect.com.br");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher
         (request, List.of(new FetcherDataFetcher(), new JsoupDataFetcher(), new ApacheDataFetcher()), session, "post");
      return response;
   }

   public List<Product> extractInformation(JSONObject jsonObject) throws Exception {
      List<Product> products = new ArrayList<>();
      //WEBDRIVER   Document docWebDriver = getDocWithWebDriver();
      if (jsonObject != null) {
         Integer internalId = JSONUtils.getValueRecursive(jsonObject, "retorno.produto.id_produto", Integer.class);
         String name = JSONUtils.getValueRecursive(jsonObject, "retorno.produto.nome", String.class);
         String primaryImage = getImage(jsonObject);
         String categories = JSONUtils.getValueRecursive(jsonObject, "retorno.produto.categoria", String.class);
         Double stock = getQuantity(jsonObject);
         Offers offers = checkAvailability(stock) ? scrapOffers(jsonObject) : new Offers();
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(String.valueOf(internalId))
            .setName(name)
            .setCategory1(categories)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .build();
         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private String getUrl() {
      return session.getOriginalURL().replaceAll("https://loja.vrsoft.com.br/discamp/produto/570/", "");
   }

   private String getImage(JSONObject jsonObject) {
      Integer image = JSONUtils.getValueRecursive(jsonObject, "retorno.produto.imgid", Integer.class);
      if (image != null && image != 0) {
         return "https://estaticos.nweb.com.br/imgs/nprodutos/t-" + image + ".jpg";
      }
      return null;
   }

   private Boolean checkAvailability(Double stock) {
      if (stock != null && stock > 0) {
         return true;
      }
      return false;
   }

   private Offers scrapOffers(JSONObject data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();
      Pricing pricing = scrapPricing(data);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());
      return offers;
   }

   private Double getPrice(JSONObject json) {
      Double priceDouble = JSONUtils.getValueRecursive(json, "retorno.produto.oferta.preco_oferta", Double.class);
      if (priceDouble != null) {
         priceDouble = JSONUtils.getValueRecursive(json, "retorno.produto.preco", Double.class);
         return priceDouble;
      } else {
         Integer priceInt = JSONUtils.getValueRecursive(json, "retorno.produto.preco", Integer.class);
         if (priceInt != null) {
            return (double) priceInt;
         }
      }
      return null;
   }

   private Double getQuantity(JSONObject json) {
      Double quantityDouble = JSONUtils.getValueRecursive(json, "retorno.produto.qtdmin", Double.class);
      if (quantityDouble != null && quantityDouble != 0) {
         return quantityDouble;
      } else {
         Integer quantityInt = JSONUtils.getValueRecursive(json, "retorno.produto.qtdmin", Integer.class);
         if (quantityInt != null) {
            return (double) quantityInt;
         }
      }
      return null;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double priceFrom = JSONUtils.getValueRecursive(json, "retorno.produto.preco", Double.class);
      Double spotlightPrice = getPrice(json);
      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }
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

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPERCARD.toString(),
         Card.DINERS.toString(), Card.HIPER.toString(), Card.ELO.toString(), Card.SOROCRED.toString(), Card.AMEX.toString());

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
/* WEBDRIVER

   private Document getDocWithWebDriver() {
      List<String> proxies = List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY);
      Integer attempt = 0;
      ChromeOptions options = new ChromeOptions();
      options.addArguments("--window-size=1920,1080");
      options.addArguments("--headless");
      options.addArguments("--no-sandbox");
      options.addArguments("--disable-dev-shm-usage");
      Document document = null;

      do {
         try {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), proxies.get(attempt), session,null, homePage, options);
            webdriver.waitLoad(60000);
            if (webdriver != null) {
               webdriver.waitLoad(60000);

               document = Jsoup.parse(webdriver.getCurrentPageSource());

               webdriver.terminate();

            }
            attempt++;
         } catch (Exception e) {
            Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));
         }
      }
      while (attempt < 3);

      return document;
   }
} */
