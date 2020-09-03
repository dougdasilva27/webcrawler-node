package br.com.lett.crawlernode.crawlers.corecontent.itauna;

import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class ItaunaRenaemcasaCrawler extends Crawler{

   private static final String SELLER_FULL_NAME = "Rena em Casa";
   private static final String CATEGORIES_API = "https://api.itauna.renaemcasa.com.br/v1/loja/classificacoes_mercadologicas/departamentos/arvore/filial/1/centro_distribuicao/1";
   private String apiUrl = "https://api.itauna.renaemcasa.com.br/v1/loja/produtos/";
   private String token = "";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public ItaunaRenaemcasaCrawler(Session session){
      super(session);

      changeUrl(session.getOriginalURL());
   }

   private void changeUrl(String oldUrl){
      int productPosition = oldUrl.indexOf("detalhe");

      String internalIdFromUrl = oldUrl.substring(productPosition).split("/")[1];

      this.apiUrl = this.apiUrl + internalIdFromUrl + "/filial/1/centro_distribuicao/1/detalhes";
   }

   private void getToken(){

      final String API_KEY = "df072f85df9bf7dd71b6811c34bdbaa4f219d98775b56cff9dfa5f8ca1bf8469";
      final String API_DOMAIN = "itauna.renaemcasa.com.br";
      final String API_USERNAME = "loja";
      final String LOGIN_API = "https://api.itauna.renaemcasa.com.br/v1/auth/loja/login";

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type:", "application/json");

      JSONObject jsonPayload = new JSONObject();

      jsonPayload.put("domain", API_DOMAIN);
      jsonPayload.put("key", API_KEY);
      jsonPayload.put("username", API_USERNAME);

      Request request = Request.RequestBuilder.create().setHeaders(headers).setPayload(jsonPayload.toString()).setUrl(LOGIN_API).build();

      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());

      this.token = response.get("data").toString();

   }

   @Override
   protected Object fetch() {

      getToken();

      Map<String, String> headers = new HashMap<>();
      String token = "Bearer " + this.token;
      headers.put("authorization", token);
      headers.put("content-type:", "application/json");

      Request request = Request.RequestBuilder.create().setHeaders(headers).setUrl(this.apiUrl).build();
      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }

   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      json = JSONUtils.getJSONValue(json, "data");

      if (isProductPage(json)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject productJson = json.getJSONObject("produto");
         JSONObject OffersInfo = JSONUtils.getJSONValue(productJson, "oferta");

         String internalId = productJson.optString("produto_id");
         String internalPid = productJson.optString( "id");
         String name = productJson.optString( "descricao");
         String classification = productJson.optString("classificacao_mercadologica_id");

         CategoryCollection categories = crawlCategories(classification);
         String primaryImage = CrawlerUtils.completeUrl(JSONUtils.getStringValue(productJson, "imagem"), " https://", "s3.amazonaws.com/produtos.vipcommerce.com.br/250x250");
         boolean available = productJson.optBoolean("disponivel");
         String description = json.optString( "informacoes").trim();
         Integer stock = JSONUtils.getIntegerValueFromJSON(productJson, "quantidade_maxima", null);
         List<String> eans = new ArrayList<>();
         eans.add(productJson.optString("codigo_barras"));
         Offers offers = available ? scrapOffers(OffersInfo, productJson) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setStock(stock)
            .setEans(eans)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(JSONObject json) {

      return json.has("produto");
   }

   private CategoryCollection crawlCategories(String classification) {

      Map<String, String> headers = new HashMap<>();
      String token = "Bearer " + this.token;
      headers.put("authorization", token);
      headers.put("content-type:", "application/json");

      Request request = Request.RequestBuilder.create().setHeaders(headers).setUrl(CATEGORIES_API).build();
      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      CategoryCollection categories = new CategoryCollection();

      JSONArray departaments = response.getJSONArray("data");

      for(Object departament: departaments){

         JSONArray sections = ((JSONObject) departament).getJSONArray("children");

         for(Object section: sections){

            if(((JSONObject) section).optString("classificacao_mercadologica_id").equals(classification)){

               String[] items = JSONUtils.getStringValue((JSONObject) section,"link").substring(1).split("/");

               for(int i = 0; i < items.length; i++){
                  categories.add(items[i]);
               }

            }
         }
      }
      return categories;
   }

   private Offers scrapOffers(JSONObject OffersInfo, JSONObject productInfo) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(OffersInfo, productInfo);
      List<String> sales = new ArrayList<>();

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

   private Pricing scrapPricing(JSONObject OffersInfo, JSONObject productInfo) throws MalformedPricingException {
      Double spotlightPrice = null;
      Double priceFrom = null;

      if (!OffersInfo.isEmpty()) {
         spotlightPrice = JSONUtils.getDoubleValueFromJSON(OffersInfo, "preco_oferta", true);
         priceFrom = JSONUtils.getDoubleValueFromJSON(OffersInfo, "preco_antigo", true);
      } else {
         spotlightPrice = JSONUtils.getDoubleValueFromJSON(productInfo, "preco", true);

      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

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
