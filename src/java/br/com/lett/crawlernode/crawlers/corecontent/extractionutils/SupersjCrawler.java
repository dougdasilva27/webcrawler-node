package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

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
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

abstract public class SupersjCrawler extends Crawler {

   private static final String SELLER_NAME = "Super Sj";
   private static final String API = "https://www.supersj.com.br/api/produto?id=";
   private static final List<Cookie> COOKIES = new ArrayList<>();
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public SupersjCrawler(Session session){
      super(session);
   }

   abstract protected String getLocationId();

   @Override
   protected Object fetch(){

      int initialIndex = session.getOriginalURL().indexOf("//");
      String[] urlSplit = session.getOriginalURL().substring(initialIndex+2).split("/");
      String skuId = null;
      if(urlSplit.length > 0){
         skuId = urlSplit[2];
      }
      BasicClientCookie cookie = new BasicClientCookie("ls.uid_armazem", getLocationId());
      cookie.setDomain("www.supersj.com.br");
      cookie.setPath("/");
      COOKIES.add(cookie);

      Request request = Request.RequestBuilder.create().setUrl(API+skuId).setCookies(COOKIES).build();

      return JSONUtils.stringToJson(dataFetcher.get(session, request).getBody());
   }
   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {

      JSONArray infoProduct = json.optJSONArray("Produtos");
      JSONObject productJson = infoProduct.optJSONObject(0);
      List<Product> products = new ArrayList<>();
      if(isProductPage(productJson)){

         String name = productJson.optString("str_nom_produto");
         String internalId = String.valueOf(productJson.optInt("id_produto"));
         CategoryCollection categories = scrapCategories(productJson);
         String primaryImage = productJson.optString("str_img_path_cdn", null) + "-g.jpg";
         List<String> secondaryImages = scrapSecondaryProducts(json);
         String description = scrapDescription(productJson);
         boolean available = !productJson.optBoolean("bit_esgotado");
         Offers offers = available ? scrapOffers(productJson) : new Offers();
         Double stock2 = productJson.optDouble("int_qtd_estoque_produto", 0.0);
         int stock = (int) productJson.optDouble("int_qtd_estoque_produto", 0.0);
         List<String> eans = Arrays.asList(productJson.optString("str_cod_barras_produto"), null);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .setStock(stock)
            .setEans(eans)
            .build();

         products.add(product);


      } else{
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(JSONObject productJson) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productJson);
      List<String> sales = Collections.singletonList(productJson.optString("str_desc_central_promo", null));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setSales(sales)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {

      Double spotlightPrice = MathUtils.normalizeTwoDecimalPlaces(productJson.optDouble("mny_vlr_promo_tabela_preco", 0.0));
      Double priceFrom = MathUtils.normalizeTwoDecimalPlaces(productJson.optDouble("mny_vlr_produto_tabela_preco", 0.0));
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      CreditCards creditCards = scrapCreditCards(productJson, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(JSONObject productJson, double spotlightPrice) throws MalformedPricingException {

      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(productJson);
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

   public Installments scrapInstallments(JSONObject productJson) throws MalformedPricingException {
      Installments installments = new Installments();

      int installment = productJson.optInt("int_qtd_parcela", 0);
      double valueElement = productJson.optDouble("mny_vlr_parcela", 0.0);

      if (valueElement != 0.0 && installment != 0) {
         Double value = MathUtils.normalizeTwoDecimalPlaces(valueElement);

         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installment)
            .setInstallmentPrice(value)
            .build());
      }
      return installments;
   }

   private boolean isProductPage(JSONObject productJson){
      return productJson.has("id_produto");
   }

   private CategoryCollection scrapCategories(JSONObject json){

      CategoryCollection categories = new CategoryCollection();

      categories.add(json.optString("str_categoria", null));
      categories.add(json.optString("str_subcategoria", null));
      categories.add(json.optString("str_tricategoria", null));

      return categories;
   }

   private List<String> scrapSecondaryProducts(JSONObject json){

      List<String> images = new ArrayList<>();
      JSONArray imagesArray = json.optJSONArray("Imagens");
      if(imagesArray != null){
         imagesArray.remove(0);
         for(Object i:imagesArray){
            images.add(((JSONObject) i).optString("str_img_path_cdn", null));
         }
      }

      return images;
   }

   private String scrapDescription(JSONObject productJson){

      String description = null;
      description += productJson.optString("str_html_descricao_produto")
         + productJson.optString("str_html_breve_descricao_produto")
         + productJson.optString("str_html_dados_tecnicos_produto")
         + productJson.optString("str_html_especificacao_produto")
         + productJson.optString("str_html_especificacao_produto");

      return description;
   }


}
