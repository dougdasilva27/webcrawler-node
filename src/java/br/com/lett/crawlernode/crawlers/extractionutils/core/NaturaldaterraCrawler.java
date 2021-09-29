package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
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
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

public class NaturaldaterraCrawler extends Crawler {

   private static final String SELLER_NAME_LOWER = "natural da terra";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public NaturaldaterraCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("store", "natural_da_terra");

      String lastParams = CommonMethods.getLast(session.getOriginalURL().split("/")).replace(".html", "");

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("urlKey", lastParams);
      jsonObject.put("sourceCode", session.getOptions().optString("code")); //options

      String paramsEncoded = null;
      try {
         paramsEncoded = URLEncoder.encode(jsonObject.toString(), "UTF-8");
      } catch (UnsupportedEncodingException e) {
         e.printStackTrace();
      }

      String url = "https://naturaldaterra.com.br/graphql?query=query+getProductDetailForProductPage($urlKey:String!$sourceCode:String){products(filter:{url_key:{eq:$urlKey}}sourceCode:$sourceCode){items{id+...ProductDetailsFragment+upsell_products{id+ean+name+categories{name+__typename}price{regularPrice{amount{currency+value+__typename}__typename}__typename}price_range{maximum_price{discount{amount_off+percent_off+__typename}final_price{currency+value+__typename}regular_price{currency+value+__typename}__typename}__typename}price_tiers{discount{amount_off+percent_off+__typename}final_price{currency+value+__typename}quantity+__typename}promotion_label+sku+small_image{url+label+__typename}stock_status+stock_quantity+unity_price_description+total_unity_price+total_unity_description+url_key+url_suffix+__typename}stock_status+related_products{id+ean+name+categories{name+__typename}price{regularPrice{amount{currency+value+__typename}__typename}__typename}price_range{maximum_price{discount{amount_off+percent_off+__typename}final_price{currency+value+__typename}regular_price{currency+value+__typename}__typename}__typename}price_tiers{discount{amount_off+percent_off+__typename}final_price{currency+value+__typename}quantity+__typename}promotion_label+sku+small_image{url+label+__typename}stock_status+stock_quantity+unity_price_description+total_unity_price+total_unity_description+url_key+url_suffix+__typename}__typename}__typename}}fragment+ProductDetailsFragment+on+ProductInterface{__typename+sku+product_title_description+medium_product_weight+stock_status+stock_quantity+promotion_label+unity_price_description+total_unity_price+total_unity_description+url_key+url_suffix+meta_description+name+categories{id+name+breadcrumbs{category_id+__typename}__typename}description{html+__typename}media_gallery_entries{id+label+position+disabled+file+__typename}price{regularPrice{amount{currency+value+__typename}__typename}__typename}price_range{maximum_price{discount{amount_off+percent_off+__typename}final_price{currency+value+__typename}regular_price{currency+value+__typename}__typename}__typename}price_tiers{discount{amount_off+percent_off+__typename}final_price{currency+value+__typename}quantity+__typename}small_image{url+label+__typename}...on+CustomizableProductInterface{options{title+option_id+uid+required+sort_order+__typename}__typename}...on+ConfigurableProduct{configurable_options{attribute_code+attribute_id+id+label+values{default_label+label+store_label+use_default_value+value_index+swatch_data{...on+ImageSwatchData{thumbnail+__typename}value+__typename}__typename}__typename}variants{attributes{code+value_index+__typename}product{id+media_gallery_entries{id+disabled+file+label+position+__typename}sku+stock_status+stock_quantity+price{regularPrice{amount{currency+value+__typename}__typename}__typename}__typename}__typename}__typename}}&operationName=getProductDetailForProductPage&variables=" +
         paramsEncoded;
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      String content = new JsoupDataFetcher().get(session, request).getBody();

      return CrawlerUtils.stringToJson(content);
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(json)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject productJson = JSONUtils.getValueRecursive(json, "data.products.items.0", JSONObject.class);

         String internalPid = productJson.optString("sku");
         String internalId = productJson.optString("id");
         String name = productJson.optString("name");
         String description = productJson.optString("meta_description");
         String primaryImage = JSONUtils.getValueRecursive(productJson, "small_image.url", String.class);
         boolean available = productJson.optString("stock_status").equals("IN_STOCK");
         Offers offers = available ? scrapOffers(productJson) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(JSONObject jsonObject) {
      return jsonObject.has("data");
   }

   private Offers scrapOffers(JSONObject json) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);
      List<String> sales = getSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME_LOWER)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private List<String> getSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String sale = CrawlerUtils.calculateSales(pricing);
      if (sale != null) {
         sales.add(sale);
      }
      return sales;

   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double priceFrom = JSONUtils.getValueRecursive(json, "price.regularPrice.amount.value", Double.class);
      Double spotlightPrice = JSONUtils.getValueRecursive(json, "price_tiers.0.final_price.value", Double.class);
     if (priceFrom.equals(spotlightPrice)){
        priceFrom = null;
     }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditcards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditcards(Double spotlightPrice) throws MalformedPricingException {
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
