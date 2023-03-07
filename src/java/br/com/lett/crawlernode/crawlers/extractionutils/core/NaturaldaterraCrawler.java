package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
      String sourceCode = session.getOptions().optString("code");
      Map<String, String> headers = new HashMap<>();
      headers.put("store", "natural_da_terra");
      String lastParams = CommonMethods.getLast(session.getOriginalURL().split("/")).replace(".html", "");
      String query = "query+getProductDetailForProductPage%28%24urlKey%3AString%21%24sourceCode%3AString%29%7Bproducts%28filter%3A%7Burl_key%3A%7Beq%3A%24urlKey%7D%7DsourceCode%3A%24sourceCode%29%7Bitems%7Bid+...ProductDetailsFragment+upsell_products%7Bid+ean+name+categories%7Bname+__typename%7Dprice%7BregularPrice%7Bamount%7Bcurrency+value+__typename%7D__typename%7D__typename%7Dprice_range%7Bmaximum_price%7Bdiscount%7Bamount_off+percent_off+__typename%7Dfinal_price%7Bcurrency+value+__typename%7Dregular_price%7Bcurrency+value+__typename%7D__typename%7D__typename%7Dprice_tiers%7Bdiscount%7Bamount_off+percent_off+__typename%7Dfinal_price%7Bcurrency+value+__typename%7Dquantity+__typename%7Dpromotion_label+sku+small_image%7Burl+label+__typename%7Dstock_status+stock_quantity+unity_price_description+total_unity_price+total_unity_description+medium_product_weight+product_title_description+url_key+url_suffix+__typename%7Dstock_status+related_products%7Bid+ean+name+categories%7Bname+__typename%7Dprice%7BregularPrice%7Bamount%7Bcurrency+value+__typename%7D__typename%7D__typename%7Dprice_range%7Bmaximum_price%7Bdiscount%7Bamount_off+percent_off+__typename%7Dfinal_price%7Bcurrency+value+__typename%7Dregular_price%7Bcurrency+value+__typename%7D__typename%7D__typename%7Dprice_tiers%7Bdiscount%7Bamount_off+percent_off+__typename%7Dfinal_price%7Bcurrency+value+__typename%7Dquantity+__typename%7Dpromotion_label+sku+small_image%7Burl+label+__typename%7Dstock_status+stock_quantity+unity_price_description+total_unity_price+total_unity_description+medium_product_weight+product_title_description+url_key+url_suffix+__typename%7D__typename%7D__typename%7D%7Dfragment+ProductDetailsFragment+on+ProductInterface%7B__typename+sku+product_title_description+medium_product_weight+stock_status+stock_quantity+promotion_label+unity_price_description+total_unity_price+total_unity_description+url_key+url_suffix+meta_description+name+categories%7Bid+name+breadcrumbs%7Bcategory_id+__typename%7D__typename%7Ddescription%7Bhtml+__typename%7Dmedia_gallery_entries%7Bid+label+position+disabled+file+__typename%7Dprice%7BregularPrice%7Bamount%7Bcurrency+value+__typename%7D__typename%7D__typename%7Dprice_range%7Bmaximum_price%7Bdiscount%7Bamount_off+percent_off+__typename%7Dfinal_price%7Bcurrency+value+__typename%7Dregular_price%7Bcurrency+value+__typename%7D__typename%7D__typename%7Dprice_tiers%7Bdiscount%7Bamount_off+percent_off+__typename%7Dfinal_price%7Bcurrency+value+__typename%7Dquantity+__typename%7Dsmall_image%7Burl+label+__typename%7D...on+CustomizableProductInterface%7Boptions%7Btitle+option_id+uid+required+sort_order+__typename%7D__typename%7D...on+ConfigurableProduct%7Bconfigurable_options%7Battribute_code+attribute_id+id+label+values%7Bdefault_label+label+store_label+use_default_value+value_index+swatch_data%7B...on+ImageSwatchData%7Bthumbnail+__typename%7Dvalue+__typename%7D__typename%7D__typename%7Dvariants%7Battributes%7Bcode+value_index+__typename%7Dproduct%7Bid+media_gallery_entries%7Bid+disabled+file+label+position+__typename%7Dsku+stock_status+stock_quantity+price%7BregularPrice%7Bamount%7Bcurrency+value+__typename%7D__typename%7D__typename%7D__typename%7D__typename%7D__typename%7D%7D";
      String variables = "%7B%22urlKey%22%3A%22" + lastParams + "%22%2C%22sourceCode%22%3A%22" + sourceCode + "%22%7D";
      String url = "https://naturaldaterra.com.br/graphql?query=" + query + "&variables=" + variables;
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, new ApacheDataFetcher(), true);
      return CrawlerUtils.stringToJson(response.getBody());
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

   private Double getPrice(JSONObject json, String path) {
      Double price = JSONUtils.getValueRecursive(json, path, Double.class);
      if (price != null) {
         return price;
      }
      Integer priceInt = JSONUtils.getValueRecursive(json, path, Integer.class);
      return priceInt != null ? priceInt * 1.0 : null;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double priceFrom = getPrice(json, "price.regularPrice.amount.value");
      Double spotlightPrice = getPrice(json, "price_tiers.0.final_price.value");
      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
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
