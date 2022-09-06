package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import br.com.lett.crawlernode.util.*;
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

import java.util.*;

public class BrasilMartinsCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   public BrasilMartinsCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
      super.config.setParser(Parser.HTML);
   }

   protected String accessToken;

   protected String cnpj = getCnpj();
   protected String codCli = getCodCli();
   protected String ufBilling = getUfBilling();
   protected String filDelivery = getFilDelivery();

   protected String getPassword() {
      return session.getOptions().optString("pass");
   }

   protected String getLogin() {
      return session.getOptions().optString("login");
   }

   protected String getCnpj() {
      return session.getOptions().optString("cnpj");
   }

   protected String getCodCli() {
      return session.getOptions().optString("cod_cliente");
   }

   protected String getUfBilling() {
      return session.getOptions().optString("uf_billing");
   }

   protected String getFilDelivery() {
      return session.getOptions().optString("fil_delivery");
   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("authority", "www.martinsatacado.com.br");

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(session.getOriginalURL())
         .setSendUserAgent(true)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .build();
      return CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session, "get");
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      String str = CrawlerUtils.scrapScriptFromHtml(doc, "#__NEXT_DATA__");
      JSONArray productArr = JSONUtils.stringToJsonArray(str);
      JSONObject data = JSONUtils.getValueRecursive(productArr, "0.props.pageProps.fallback.PRODUCT_DETAIL", JSONObject.class);
      if (data != null) {
         String internalPid = null;
         String id = data.optString("productSku");

         if (id != null) {
            internalPid = CommonMethods.getLast(id.split("_"));
         }

         String name = data.optString("name");
         String primaryImage = JSONUtils.getValueRecursive(data, "images.0.value", String.class);

         String description = data.optString("description");
         List<String> secondaryImages = scrapSecondaryImages(data, primaryImage);
         JSONArray priceObj = getOffersFromApi(data);
         Offers offers = priceObj != null && !priceObj.isEmpty() ? scrapOffers(priceObj) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   protected JSONArray getOffersFromApi(JSONObject data) {
      JSONObject priceObj = fetchPrice(data.optString("productSku"));
      if (priceObj != null) {

         JSONArray prices = JSONUtils.getValueRecursive(priceObj, "resultado.0.precos", JSONArray.class);

         if (prices != null) {
            for (Object p : prices) {
               JSONObject price = (JSONObject) p;
               if (price.optInt("estoque", 0) > 0) {
                  return prices;
               }
            }
         } else {
            JSONArray pricesSellers = priceObj.optJSONArray("lstPrecoSeller");
            if (pricesSellers != null) {
               return pricesSellers;
            }

         }
      }

      return new JSONArray();
   }

   protected void login() {

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("authority", "www.martinsatacado.com.br");
      headers.put("x-requested-with", "XMLHttpRequest");
      headers.put("referer", "https://www.martinsatacado.com.br/");
      headers.put("Authorization", "Basic YmI2ZDhiZTgtMDY3MS0zMmVhLTlhNmUtM2RhNGM2MzUyNWEzOmJmZDYxMTdlLWMwZDMtM2ZjNS1iMzc3LWFjNzgxM2Y5MDY2ZA==");

      String payload = "{\"grant_type\":\"password\",\"cnpj\":\"" + cnpj + "\",\"username\":\"" + getLogin() + "\",\"codCli\":\"" + codCli + "\",\"password\":\"" + getPassword() + "\",\"codedevnd\":\"\",\"profile\":\"ROLE_CLIENT\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl("https://ssd.martins.com.br/oauth-marketplace-portal/access-tokens")
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .setHeaders(headers)
         .setSendUserAgent(true)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new ApacheDataFetcher(), new FetcherDataFetcher()), session, "post");

      String str = response.getBody();
      JSONObject body = JSONUtils.stringToJson(str);
      accessToken = body.optString("access_token");
   }

   private JSONObject fetchPrice(String id) {
      try {
         login();
         List<String> parts = List.of(id.split("_"));

         Map<String, String> headers = new HashMap<>();
         headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
         headers.put("Origin", "www.martinsatacado.com.br");
         headers.put("access_token", accessToken);
         headers.put("host", "ssd.martins.com.br");
         headers.put("client_id", "bb6d8be8-0671-32ea-9a6e-3da4c63525a3");
         headers.put("Authorization", "Basic YmI2ZDhiZTgtMDY3MS0zMmVhLTlhNmUtM2RhNGM2MzUyNWEzOmJmZDYxMTdlLWMwZDMtM2ZjNS1iMzc3LWFjNzgxM2Y5MDY2ZA==");

         String payloadMartins = parts.get(0).equals("martins") ? "{\"CodigoMercadoria\": \"" + id + "\", \"Quantidade\": 0, \"codGroupMerFrac\": 0, \"codPmc\": null }" : "";
         String payload3P = !parts.get(0).equals("martins") ? "{\"seller\":\"" + parts.get(0) + "\",\"CodigoMercadoria\":\"" + id + "\",\"Quantidade\":0}" : "";

         String payload = "{\"asm\":0,\"produtos\":[" + payloadMartins + "]," +
            "\"ProdutosExclusaoEan\":[],\"produtosSeller\":[" + payload3P + "],\"codeWarehouseDelivery\":0,\"codeWarehouseBilling\":0,\"condicaoPagamento\":111,\"uid\":6659973,\"segment\":0," +
            "\"tipoLimiteCred\":\"C\",\"precoEspecial\":\"S\",\"ie\":\"127285489112\",\"territorioRca\":0,\"classEstadual\":10,\"tipoSituacaoJuridica\":\"M\",\"codSegNegCliTer\":0," +
            "\"tipoConsulta\":1,\"commercialActivity\":5,\"groupMartins\":171,\"codCidadeEntrega\":3232,\"codCidade\":3232,\"codRegiaoPreco\":250,\"temVendor\":\"S\",\"codigoCanal\":9," +
            "\"ufTarget\":\"SP\",\"bu\":1,\"manual\":\"N\",\"email\":\"" + getLogin() + "\",\"numberSpinPrice\":\"64\",\"codeDeliveryRegion\":\"322\",\"ufFilialFaturamento\":\"GO\"," +
            "\"cupons_novos\":[],\"codopdtrcetn\":10,\"origemChamada\":\"PLP\"}";

         Request request = Request.RequestBuilder.create()
            .setUrl("https://ssd.martins.com.br/b2b-partner/v1/produtosBuyBox")
            .setPayload(payload)
            .setProxyservice(Arrays.asList(
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.BUY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
            .setHeaders(headers)
            .build();

         Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new ApacheDataFetcher(), new FetcherDataFetcher()), session, "post");
         return JSONUtils.stringToJson(response.getBody());

      } catch (Exception e) {
         return null;
      }
   }

   private List<String> scrapSecondaryImages(JSONObject data, String primaryImage) {
      List<String> list = new ArrayList<>();
      JSONArray arr = data.optJSONArray("images");
      for (Object i : arr) {
         JSONObject image = (JSONObject) i;
         String imageStr = image.optString("value");
         if (!imageStr.equals(primaryImage)) {
            list.add(imageStr);
         }
      }

      return list;
   }

   private Offers scrapOffers(JSONArray prices) throws OfferException, MalformedPricingException {
      String seller1P = filDelivery + " - " + ufBilling;
      Offers offers = new Offers();

      for (Object p : prices) {
         JSONObject price = (JSONObject) p;
         if (price.optInt("estoque", 0) > 0) {
            Pricing pricing = scrapPricing(price);
            String sales = scrapSales(price);
            String seller = scrapSeller(price);

            offers.add(Offer.OfferBuilder.create()
               .setIsBuybox(false)
               .setPricing(pricing)
               .setSales(Collections.singletonList(sales))
               .setSellerFullName(seller)
               .setIsMainRetailer(seller.equalsIgnoreCase(seller1P))
               .setUseSlugNameAsInternalSellerId(true)
               .build());
         }
      }

      return offers;
   }

   private String scrapSeller(JSONObject price) {
      String seller = null;
      String sellerName = price.optString("fil_delivery");
      if (sellerName == null || sellerName.isEmpty()) {
         sellerName = price.optString("seller");
      }

      if (sellerName != null) {
         seller = sellerName;
         String sellerLocate = price.optString("uf_Billing");
         if (sellerLocate == null || sellerLocate.isEmpty()) {
            sellerLocate = price.optString("uf_delivery");
            seller = " - " + sellerLocate;

         }
      }

      return seller;

   }

   private String scrapSales(JSONObject obj) {
      List<String> sales = new ArrayList<>();
      Integer q = obj.optInt("QdeUndVndCxaFrn");
      String salesFromDoc = null;
      if (q > 0) {
         String value = obj.optString("precoCaixa");
         if (value != null) {
            Double valueDouble = Double.parseDouble(value);
            valueDouble = valueDouble * q;
            salesFromDoc = "Caixa com " + q + "un, com valor de: " + MathUtils.normalizeTwoDecimalPlaces(valueDouble);
         }

      }

      if (salesFromDoc != null) {
         sales.add(salesFromDoc);
      }

      return sales.toString();
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      String spotlightPriceStr = data.optString("preco");
      if (spotlightPriceStr != null && (spotlightPriceStr.equals("0.0") || spotlightPriceStr.isEmpty())) {
         spotlightPriceStr = data.optString("precoNormal");
      }
      Double spotlightPrice = null;
      if (spotlightPriceStr != null) {
         spotlightPrice = Double.parseDouble(spotlightPriceStr);
      }


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

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

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
