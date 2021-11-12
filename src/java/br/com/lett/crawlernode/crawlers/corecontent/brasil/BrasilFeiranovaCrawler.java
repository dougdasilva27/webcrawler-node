package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;


public class BrasilFeiranovaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "feira nova";
   private static final String HOME_PAGE = "https://www.feiranovaemcasa.com.br/";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(),
      Card.DINERS.toString());
   private String token;

   public BrasilFeiranovaCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSONARRAY);
   }

   private Response validateToken() {
      String initPayload = "{\n" +
         "    \"Token\": null,\n" +
         "    \"CdCliente\": null,\n" +
         "    \"CdEmpresa\": 113,\n" +
         "    \"Nome\": null,\n" +
         "    \"Senha\": \"7C4A8D09CA3762AF61E59520943DC26494F8941B\",\n" +
         "    \"Email\": \"ttatianeisabelfigueiredo@atiara.com.br\",\n" +
         "    \"Cpf\": null,\n" +
         "    \"inCNPJ\": false,\n" +
         "    \"LiberaDescontos\": false,\n" +
         "    \"LiberaPrDesconto\": null,\n" +
         "    \"LiberaValidadeDias\": null\n" +
         "}";

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("authorization", "Bearer");

      Request request = Request.RequestBuilder.create().setUrl("https://ecom.solidcon.com.br/api/crm/login/")
         .setPayload(initPayload)
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .build();

      Response response = this.dataFetcher.post(session, request);

      return response;
   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      JSONObject tokenJson = JSONUtils.stringToJson(validateToken().getBody());
      headers.put("content-type", "application/json");
      headers.put("authorization", "Bearer " + tokenJson.getString("token"));

      String initPayload = "{\"Promocao\":false,\"Comprado\":false,\"Produto\":\"5000267023601\",\"Favorito\":false}";

      Request request = Request.RequestBuilder.create().setUrl("https://ecom.solidcon.com.br/api/v2/shop/produto/empresa/113/filial/329/GetProdutos")
         .setPayload(initPayload)
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .build();

      Response response = this.dataFetcher.post(session, request);

      return response;
   }

   @Override
   public List<Product> extractInformation(JSONArray responseJson) throws Exception {
      super.extractInformation(responseJson);
      List<Product> products = new ArrayList<>();
      JSONObject json = (JSONObject) responseJson.get(0);

      if (json.has("nmProduto")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         Number internalPidNumber = json.getNumber("cdProduto");
         String internalPid = internalPidNumber.toString();
         String name = JSONUtils.getStringValue(json, "nmProduto");
         List<String> categories = new ArrayList<>();
         categories.add(JSONUtils.getStringValue(json, "Categoria"));
         String primaryImage = JSONUtils.getStringValue(json, "urlFoto");
         boolean available = (!json.getBoolean("comprado")) ? true : false;
         Offers offers = available ? scrapOffers(json) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

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

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(json, "preco", true);//TODO: verificar preço
      Double priceFrom = spotlightPrice;

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
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
}
