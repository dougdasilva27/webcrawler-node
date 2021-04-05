package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import models.Offer;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * date: 02/04/2019
 *
 * @author gabriel
 *
 */
public class BrasilGazinCrawler extends Crawler {

   private static final String MAINSELLER = "Gazin Feira de Santana";
   private static final String PROTOCOL = "https";
   private static final String HOST = "www.gazin.com.br";
   private static final String HOME_PAGE = PROTOCOL + "://" + HOST;
   private static final String PRODUCT_API = "https://marketplace-api.gazin.com.br/v1/canais/produtos/";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPERCARD.toString(), Card.ELO.toString(),
      Card.SOROCRED.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilGazinCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   private String createApiUrl(){

      String[] urlSplitId = session.getOriginalURL().split("/");
      String[] urlSplitParams = session.getOriginalURL().split("\\?");
      String id = "";
      String params = "";

      if(urlSplitId.length > 3){
         id = urlSplitId[4];
      }

      if(urlSplitParams.length > 1){
         params = CommonMethods.getLast(urlSplitParams);
      }

      return PRODUCT_API + id + "?" + params;
   }

   @Override
   protected Object fetch(){

      HashMap<String, String> headers = new HashMap<>();
      headers.put("Canal", "gazin-ecommerce");

      Request request = Request.RequestBuilder.create().setUrl(createApiUrl()).setHeaders(headers).build();

      return JSONUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (isProductPage(json)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONArray variations = json.optJSONArray("variacoes");
         JSONObject categoriesJson = json.optJSONObject("categoria");
         String internalPid = json.optString("id");
         String description = scrapDescription(internalPid) + json.optString("descricao");

         if(variations != null){

            for(Object e: variations){

               JSONObject variation = (JSONObject) e;

               String internalId = variation.optString("id");
               String name = json.optString("titulo");
               name = scrapName(name, variation);
               CategoryCollection categories = scrapCategories(categoriesJson);
               String primaryImage = !scrapImages(variation).isEmpty() ? scrapImages(variation).get(0) : null;
               List<String> secondaryImages = scrapSecondaryImages(variation);
               List<String> eans = Collections.singletonList(variation.optString("ean"));
               boolean available = isAvailable(variation);
               Offers offers = available ? scrapOffer(variation) : new Offers();

               Product product = ProductBuilder
                  .create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setDescription(description)
                  .setCategories(categories)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setOffers(offers)
                  .setEans(eans)
                  .build();

               products.add(product);
            }
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(JSONObject json) {
      return json.has("slug");
   }

   private boolean isAvailable(JSONObject variation){
      JSONArray advertising = variation.optJSONArray("anuncios");

      for(Object e: advertising){

         JSONObject offer = (JSONObject) e;

         if(offer.optInt("estoque") > 0){
            return true;
         }
      }

      return false;
   }

   private String scrapName(String name, JSONObject variation){

      if (name != null) {
         JSONArray combinations = variation.getJSONArray("combinacoes");

         if(combinations != null){

            StringBuilder nameBuilder = new StringBuilder(name);
            for(Object e: combinations){

               JSONObject combination = (JSONObject) e;

               if(!combination.optString("valor").equals("Sem Cor") && !combination.optString("valor").equals("Sem Voltagem")){
                  nameBuilder.append(" - ").append(combination.optString("valor"));
               }
            }
            name = nameBuilder.toString();
         }
      }

      return name;
   }

   private String scrapDescription(String internalPid){

      String url = PRODUCT_API + internalPid + "/informacoes";

      HashMap<String, String> headers = new HashMap<>();
      headers.put("Canal", "gazin-ecommerce");

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build();
      JSONArray response = JSONUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      StringBuilder description = new StringBuilder();
      for(Object e: response){

         JSONObject info = (JSONObject) e;

         description.append(info.optString("campo"))
            .append(" ")
            .append(info.optString("valor"))
            .append("<div></div>");
      }

      return description.toString();
   }

   private List<String> scrapImages(JSONObject variation){

      List<String> imagensUrl = new ArrayList<>();
      JSONArray imagens = variation.getJSONArray("imagens");

      if(imagens != null){

         for(Object e: imagens){

            JSONObject imagem = (JSONObject) e;

            imagensUrl.add(imagem.optString("url"));
         }
      }

      return imagensUrl;
   }

   private List<String> scrapSecondaryImages(JSONObject variation){

      List<String> images = scrapImages(variation);

      if(!images.isEmpty()){
         images.remove(0);
      }

      return images;
   }

   private CategoryCollection scrapCategories(JSONObject categoriesJson){

      CategoryCollection categoryCollection = new CategoryCollection();
      boolean hasAnotherCategories = true;

      if(categoriesJson != null){
         String lastCategories = categoriesJson.optString("nome");
         categoryCollection.add(lastCategories);

         if(categoriesJson.has("categoria_pai")){
            do{
               JSONArray categoriesArray = categoriesJson.optJSONArray("categoria_pai");
               if(!categoriesArray.isEmpty()){
                  categoryCollection.add(((JSONObject)categoriesArray.get(0)).optString("nome"));
                  categoriesJson = ((JSONObject)categoriesArray.get(0));
               } else{
                  hasAnotherCategories = false;
               }
            } while (hasAnotherCategories);
         }

         Collections.reverse(categoryCollection);
      }

      return categoryCollection;

   }

   private Offers scrapOffer(JSONObject variation) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      JSONArray advertising = variation.optJSONArray("anuncios");

      for(Object e: advertising){

         JSONObject offer = (JSONObject) e;

         if(offer.optInt("estoque") > 1){
            Pricing pricing = scrapPricing(offer);
            List<String> sales = new ArrayList<>();

            offers.add(Offer.OfferBuilder.create()
               .setUseSlugNameAsInternalSellerId(false)
               .setInternalSellerId(scrapSellerId(offer))
               .setSellerFullName(scrapSellerName(offer))
               .setMainPagePosition(1)
               .setIsBuybox(true)
               .setIsMainRetailer(scrapSellerName(offer).equalsIgnoreCase(MAINSELLER))
               .setSales(sales)
               .setPricing(pricing)
               .build());
         }
      }


      return offers;

   }

   private String scrapSellerId(JSONObject offer){

      JSONObject seller = offer.optJSONObject("seller");

      return Integer.toString(seller.optInt("id"));
   }

   private String scrapSellerName(JSONObject offer){

      JSONObject seller = offer.optJSONObject("seller");
      String sellerName = "";

      if(seller != null){
         return seller.optString("nome_fantasia");
      }

      return sellerName;
   }

   private Pricing scrapPricing(JSONObject offer) throws MalformedPricingException {

      JSONObject priceInfo = offer.optJSONObject("preco");
      JSONObject payment = offer.optJSONObject("formas_pagamento");
      JSONObject bankslipPayment = payment.optJSONObject("boleto");
      Double bankSlipPrice = null;
      Double priceFrom = null;
      Double spotlightPrice = null;

      if(bankslipPayment != null){
         bankSlipPrice = bankslipPayment.optDouble("por");
         spotlightPrice = bankSlipPrice;
         priceFrom = bankslipPayment.optDouble("de");
      }

      CreditCards creditCards = scrapCreditCards(priceInfo, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(scrapBankSlip(bankSlipPrice))
         .setCreditCards(creditCards)
         .build();

   }

   private BankSlip scrapBankSlip(Double bankSlipPrice) throws MalformedPricingException {

      return BankSlip.BankSlipBuilder.create()
         .setFinalPrice(bankSlipPrice)
         .build();
   }

   private CreditCards scrapCreditCards(JSONObject priceInfo, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      JSONObject installmentsInfo = null;

      if(priceInfo != null){

         installmentsInfo = priceInfo.optJSONObject("parcelamento");
      }

      Installments installments = scrapInstallments(installmentsInfo);
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

   private Installments scrapInstallments(JSONObject installmentsInfo) throws MalformedPricingException {
      Installments installments = new Installments();

      Double value;
      int installmentsNumbers;

      if(installmentsInfo != null){
         value = installmentsInfo.optDouble("valor");
         installmentsNumbers = installmentsInfo.optInt("quantidade");

         for(int i = 1; i <= installmentsNumbers; i++){
            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(i)
               .setInstallmentPrice(value)
               .build());
         }
      }
      return installments;
   }
}
