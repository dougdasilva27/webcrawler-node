package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;


import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date: 27/01/21
 *
 * @author Fellype Layunne
 */

public class RiodejaneiroPrincesasupermercadosCrawler extends Crawler {

   private String getLocation() {
      return session.getOptions().getString("filial");
   }

   private final String grammatureRegex = "(\\d+[.,]?\\d*\\s?)(ml|l|g|gr|mg|kg)";

   private final String SELLER_NAME = "Princesa";

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AMEX.toString(), Card.HIPERCARD.toString(), Card.DINERS.toString());

   public RiodejaneiroPrincesasupermercadosCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
      super.config.setParser(Parser.JSONARRAY);
   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put("authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIjoic29saWRjb24iLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9lbWFpbGFkZHJlc3MiOiJzb2xpZGNvbkBzb2xpZGNvbi5jb20uYnIiLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1laWRlbnRpZmllciI6IjM3NTNiYWEzLTVhZGYtNDY0Ni1hNTY5LTIxMmQxMzlhNjdmYyIsImV4cCI6MTk1NTA0OTg3OSwiaXNzIjoiRG9yc2FsV2ViQVBJIiwiYXVkIjoic29saWRjb24uY29tLmJyIn0.LxDewxZ-V_kXYjcl8sM9Z3nD5vkymfAv4mAWJXGx5o4");
      headers.put("content-type", "application/json; charset=utf-8");
      headers.put("accept", "application/json");
      headers.put("Referer", "https://www.princesasupermercados.com.br/");

      String internalPid = getInternalPid();
      String payload = "{\"Produto\":\"" + internalPid + "\"}";

      Request request = Request.RequestBuilder.create().setUrl("https://ecom.solidcon.com.br/api/v2/shop/produto/empresa/103/filial/" + getLocation() + "/GetProdutos")
         .setPayload(payload)
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

         String internalPid = getInternalPid();
         String productName = scrapName(json);
         String description = json.optString("descricao");
         List<String> categories = new ArrayList<>();
         categories.add(JSONUtils.getStringValue(json, "Categoria"));
         String primaryImage = JSONUtils.getStringValue(json, "urlFoto");
         Integer stock = json.optInt("qtDisponivel");
         boolean isAvailable = stock > 0;
         Offers offers = isAvailable ? scrapOffers(json) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid)
            .setInternalPid(internalPid)
            .setName(productName)
            .setDescription(description)
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
         .setSellerFullName(SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Double scrapPrice(JSONObject product) {
      Double priceKg = product.optDouble("preco");

      if (product.optBoolean("inFracionado") == true) {
         Double priceFraction = product.optDouble("fracionamento");
         if (priceFraction != null && !priceFraction.isNaN()) {
            priceKg = priceKg * priceFraction;
         }
      }
      return priceKg;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = scrapPrice(json);
      Double priceFrom;
      DecimalFormat df = new DecimalFormat("######.##");

      Double isPromotion = json.optDouble("precoPromocao");
      Double priceFraction = json.optDouble("fracionamento");

      if (isPromotion != null && !isPromotion.isNaN()) {
         priceFrom = scrapPrice(json);
         if (priceFraction != null && !priceFraction.isNaN()) {
            spotlightPrice = isPromotion * priceFraction;
            df.setRoundingMode(RoundingMode.UP);
            spotlightPrice = MathUtils.parseDoubleWithComma((df.format(spotlightPrice)));
         }
      } else {
         priceFrom = null;
      }

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

   private String getInternalPid() {
      String[] idFromUrl = this.session.getOriginalURL().split("/");
      return idFromUrl[4];
   }

   private String scrapName(JSONObject json) {
      StringBuilder fullName = new StringBuilder();

      String productName = json.optString("nmProduto");
      String grammature = json.optString("txtFracionamento");

      String txtFractionation = extractGrammature(grammature);

      if (productName != null) {
         fullName.append(productName).append(" ");
      }
      if (stringHasGrammature(txtFractionation)) {
         fullName.append("- Porção de " + txtFractionation);
      }

      return fullName.toString();
   }

   private boolean stringHasGrammature(String hasString) {
      Pattern pattern = Pattern.compile(grammatureRegex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(hasString);

      return matcher.find();
   }

   private String extractGrammature(String txtFractionation) {
      Pattern pattern = Pattern.compile(grammatureRegex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
      final Matcher matcher = pattern.matcher(txtFractionation);

      return matcher.find() ? matcher.group(0) : "";
   }

}
