package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
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
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.stream.Collectors;

public class BrasilPeixotoCrawler extends Crawler {
   public BrasilPeixotoCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.JSOUP);
   }

   String SESSION_COOKIE = "ASP.NET_SessionId=";

   // DEPRECADO, TEM QUE USAR FETCHRESPONSE E USAR CONFIG.SETPARSER
   @Override
   protected Document fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "www.peixoto.com.br");
      headers.put("accept", "/");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("cache-control", "no-cache");
      headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("origin", "www.peixoto.com.br");
      headers.put("pragma", "no-cache");
      headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36");
      headers.put("x-requested-with", "XMLHttpRequest");
      // ESSAS LOCALIDADES ESTÁ FIXA, SE DECIDIR MONITORAR OUTRA FUTURAMENTE? ADAPTAR
      headers.put("cookie", SESSION_COOKIE + ";b2bfilfatexp=003RR02-2ED026|58;b2bfilfatexplist=58,59;b2blog=true%230%23BAR+DO+PORTUGUES%23%23204743%2332%230%23-1%23%230%230%2c38%237100624%23%230%230%23;language=pt-BR;loja_id=32");

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setCookies(this.cookies)
         .setHeaders(headers)
         .setSendUserAgent(true)
         .build();

      Response response = dataFetcher.get(session, request);

      this.cookies.addAll(response.getCookies());

      if ("https://www.peixoto.com.br/User/SelecionarFilialUsuario".equals(response.getRedirectUrl())) {
         Request filialRequest = Request.RequestBuilder.create()
            .setHeaders(headers)
            .setCookies(this.cookies)
            .setFollowRedirects(true)
            .setUrl("https://www.peixoto.com.br/User/FilialUsuarioSelecionada?id=58&formaPagamento=25&condicaoId=53&antecipado=true")
            .build();
         Response filialResponse = this.dataFetcher.get(session, filialRequest);

         this.cookies.addAll(filialResponse.getCookies());
      } else {
         return Jsoup.parse(response.getBody());
      }

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      JSONArray arr = new JSONArray();
      List<Product> products = new ArrayList<>();
      String script = CrawlerUtils.scrapScriptFromHtml(doc, "head > script:nth-child(5n-1)");
      if(script != null){
         script = script.replaceAll("window.data_layer = true;", "");
         script = script.replaceAll("dataLayer = ", "");
         script = script.replaceAll("\r", "");
         script = script.replaceAll("\n", "");
         script = script.replaceAll("\t", "");
         script = script.substring(1, script.length() - 2);
         arr= JSONUtils.stringToJsonArray(script);
      }
      if (arr.length() > 0) {
         JSONObject data = (JSONObject) arr.get(0);
         Integer id = JSONUtils.getValueRecursive(data, "transactionProducts.0.id", Integer.class);
         String internalId = id != null ? id.toString() : null;
         String name = JSONUtils.getValueRecursive(data, "transactionProducts.0.name", String.class);
         String primaryImage = JSONUtils.getValueRecursive(data, "transactionProducts.0.fullImage", String.class);
         String description = JSONUtils.getValueRecursive(data, "transactionProducts.0.description", String.class);
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc,".thumbnails .list a", Arrays.asList("data-src"),"https", "https://www.peixoto.com.br/", primaryImage);
         Boolean stock = JSONUtils.getValueRecursive(data, "transactionProducts.0.available", Boolean.class);
         Offers offers = stock ? scrapOffers(data) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .build();
         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }
      return products;
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Map<String, String> headersLogin = new HashMap<>();
      headersLogin.put("authority", "www.peixoto.com.br");
      headersLogin.put("accept", "/");
      headersLogin.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headersLogin.put("cache-control", "no-cache");
      headersLogin.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
      headersLogin.put("origin", "www.peixoto.com.br");
      headersLogin.put("pragma", "no-cache");
      headersLogin.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36");
      headersLogin.put("x-requested-with", "XMLHttpRequest");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.peixoto.com.br/User/Login")
         .setHeaders(headersLogin)
         .setPayload("password=BAR1824&domain_id=167&email=40374650000111")
         .build();

      Response responseApi = new FetcherDataFetcher().post(session, request);
      this.cookies.addAll(responseApi.getCookies());

      // TODO: PODE DAR NULL POINTER, ALTERAR O FILTRO PARA VERIFICAR SE O COOKIE ESTÁ NULO
      SESSION_COOKIE += responseApi.getCookies().stream().filter(c -> c.getName().equals("ASP.NET_SessionId")).findFirst().get().getValue();
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
         .setSellerFullName("peixoto")
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {

      Double spotlightPrice = JSONUtils.getValueRecursive(data, "transactionProducts.0.fullPrice", Double.class);
      ;
      Double priceFrom = spotlightPrice;

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

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());

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
