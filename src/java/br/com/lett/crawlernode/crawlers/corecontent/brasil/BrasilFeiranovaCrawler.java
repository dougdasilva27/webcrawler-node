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

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIjoic29saWRjb24iLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9lbWFpbGFkZHJlc3MiOiJzb2xpZGNvbkBzb2xpZGNvbi5jb20uYnIiLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1laWRlbnRpZmllciI6IjM3NTNiYWEzLTVhZGYtNDY0Ni1hNTY5LTIxMmQxMzlhNjdmYyIsImV4cCI6MTk1NTA0OTg3OSwiaXNzIjoiRG9yc2FsV2ViQVBJIiwiYXVkIjoic29saWRjb24uY29tLmJyIn0.LxDewxZ-V_kXYjcl8sM9Z3nD5vkymfAv4mAWJXGx5o4");

      String id = getIdFromUrl();

      String initPayload = "{\"Promocao\":false,\"Comprado\":false,\"Produto\": \"" + id + "\",\"Favorito\":false}";

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

   private Double scrapPrice(JSONObject product) {
      Double priceKg = JSONUtils.getDoubleValueFromJSON(product, "preco", true);

      if (product.getBoolean("inFracionado") == true) {
         Double priceFraction = JSONUtils.getDoubleValueFromJSON(product,"fracionamento", true);

         priceKg = priceKg * priceFraction;
      }

      return priceKg;
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
      Double spotlightPrice = scrapPrice(json);
      Double priceFrom = null;

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

   private String getIdFromUrl() {
      String [] urlParts = session.getOriginalURL().split("/");

      return urlParts[4];
   }
}
