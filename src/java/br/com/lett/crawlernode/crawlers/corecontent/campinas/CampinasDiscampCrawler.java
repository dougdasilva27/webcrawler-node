package br.com.lett.crawlernode.crawlers.corecontent.campinas;

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

import java.util.*;

public class CampinasDiscampCrawler extends Crawler {
   public CampinasDiscampCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   private final String SELLER_NAME = "discamp";
   private String homePage = "https://loja.vrsoft.com.br/discamp/produto/570/";

   @Override
   protected Response fetchResponse() {
      String url = "https://api.vrconnect.com.br/loja-virtual/browser/v1.05/detalheProduto";
      String payload = "{\"cabecalho\":{\"loja\":\"570\"},\"parametros\":{\"codigo\":" + getId() + "}}";
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
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new JsoupDataFetcher(), new ApacheDataFetcher()), session, "post");
      JSONObject json = CrawlerUtils.stringToJson(response.getBody());

      if (json.optJSONObject("retorno") == null) {
         url = "https://api.vrconnect.com.br/loja-virtual/browser/v1.08/carregamentoInicial";
         payload = "{\"cabecalho\":{\"detalhes_dispositivo\":{\"os_name\":\"Linux\",\"os_version\":106,\"browser_name\":\"Firefox\",\"browser_version\":106,\"navigator_useragent\":\"Mozilla/5.0 (X11; Linux x86_64; rv:106.0) Gecko/20100101 Firefox/106.0\",\"navigator_appversion\":\"5.0 (X11)\",\"navigator_platform\":\"Linux x86_64\"},\"os\":4,\"versao\":\"1.5.2\",\"loja\":570,\"is_cadastro\":0,\"is_maioridade\":1,\"identificacao\":1},\"parametros\":{\"ultima_atualizacao\":\"\",\"organizar\":0,\"sessao\":-1}}";
         request = Request.RequestBuilder.create()
            .setUrl(url)
            .setHeaders(headers)
            .setPayload(payload)
            .build();
         response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new JsoupDataFetcher(), new ApacheDataFetcher()), session, "post");
         JSONObject carregamentoInicialJson = CrawlerUtils.stringToJson(response.getBody());
         JSONObject containers = JSONUtils.getValueRecursive(carregamentoInicialJson, "retorno.containers.0", ".", JSONObject.class, new JSONObject());
         JSONArray items = containers.optJSONArray("itens");
         String vrFamilia = null;

         for (int i = 0; i < items.length(); i++) {
            String codigo = items.optJSONObject(i).optString("codigo");
            if (getId().equals(codigo)) {
               vrFamilia = items.optJSONObject(i).optJSONObject("oferta").optString("vr_familia");
               break;
            }
         }

         url = "https://api.vrconnect.com.br/loja-virtual/browser/v1.05/detalheProduto";
         payload = "{\"cabecalho\":{\"detalhes_dispositivo\":{\"os_name\":\"Linux\",\"os_version\":106,\"browser_name\":\"Firefox\",\"browser_version\":106,\"navigator_useragent\":\"Mozilla/5.0 (X11; Linux x86_64; rv:106.0) Gecko/20100101 Firefox/106.0\",\"navigator_appversion\":\"5.0 (X11)\",\"navigator_platform\":\"Linux x86_64\"},\"os\":4,\"versao\":\"1.5.2\",\"loja\":570,\"is_cadastro\":0,\"is_maioridade\":1,\"identificacao\":1},\"parametros\":{\"vr_familia\":" + vrFamilia + "}}";
         request = Request.RequestBuilder.create()
            .setUrl(url)
            .setHeaders(headers)
            .setPayload(payload)
            .build();
         response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new JsoupDataFetcher(), new ApacheDataFetcher()), session, "post");
      }

      return response;
   }

   public List<Product> extractInformation(JSONObject jsonObject) throws Exception {
      List<Product> products = new ArrayList<>();
      if (jsonObject != null) {
         JSONObject productJson = getProductJson(jsonObject);

         String internalId = productJson.optString("codigo");
         String name = productJson.optString("nome");
         String primaryImage = getImage(productJson);
         // not have secondary images and description
         String categories = productJson.optString("categoria");
         Double stock = getQuantity(productJson);
         Offers offers = checkAvailability(stock) ? scrapOffers(productJson) : new Offers();
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
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

   JSONObject getProductJson(JSONObject jsonObject) {
      if (jsonObject.optJSONObject("retorno").has("produto")) { // available product
         return JSONUtils.getValueRecursive(jsonObject, "retorno.produto", ".", JSONObject.class, new JSONObject());
      } else { // unavailable product
         JSONObject containers = JSONUtils.getValueRecursive(jsonObject, "retorno.containers.0", ".", JSONObject.class, new JSONObject());
         JSONArray items = containers.optJSONArray("itens");
         for (int i = 0; i < items.length(); i++) {
            String codigo = items.optJSONObject(i).optString("codigo");
            if (getId().equals(codigo)) {
               return items.optJSONObject(i);
            }
         }
      }

      return null;
   }

   private String getId() {
      return session.getOriginalURL().replaceAll("https://loja.vrsoft.com.br/discamp/produto/570/", "");
   }

   private String getImage(JSONObject productJson) {
      String image = productJson.optString("imgid");
      if (image != null && !image.equals("0")) {
         return "https://estaticos.nweb.com.br/imgs/nprodutos/" + image + ".jpg";
      } else if (image.equals("0")) {
         String isi = JSONUtils.getValueRecursive(productJson, "oferta.isi", ".", String.class, "");
         return !isi.equals("") ? "https://estaticos.nweb.com.br/imgs/produtos/" + isi + ".jpg" : null;
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
      if (json.optJSONObject("oferta") != null) {
         return JSONUtils.getValueRecursive(json, "oferta.preco_oferta", ".", Double.class, 0.0);
      } else {
         return json.optDouble("preco");
      }
   }

   private Double getQuantity(JSONObject json) {
      Double quantityDouble = json.optDouble("qtdmin");
      if (quantityDouble != 0) {
         return quantityDouble;
      } else {
         Integer quantityInt = json.optInt("qtdmin");
         return (double) quantityInt;
      }
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double priceFrom = json.optDouble("preco");
      Double spotlightPrice = getPrice(json);
      if (priceFrom.equals(spotlightPrice)) {
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
