package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MundodanoneCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   private static final String SELLER_NAME = "Mundo Danone";

   public MundodanoneCrawler(Session session) {
      super(session);
      config.setParser(Parser.JSON);
   }


   @Override
   protected Response fetchResponse() {

      Map<String, String> headers = new HashMap<>();
      headers.put("store", session.getOptions().optString("store"));

      String slug = getSlugFromUrl();

      String url = "https://service.mundodanone.com.br/" +
         "graphql?query=query+getProductDetailForProductPage%28%24urlKey%3AString%21%24pageSize%3AInt%3D6%24currentPage%3AInt%3D1%29%7Bproducts" +
         "%28filter%3A%7Burl_key%3A%7Beq%3A%24urlKey%7D%7D%29%7Bitems%7Bid+...ProductDetailsFragment+__typename%7D__typename%7D%7D" +
         "fragment+ProductDetailsFragment+on+ProductInterface%7B__typename+max_sale_qty+is_discount_recurrence+categories%7Bid+name+breadcrumbs%7Bcategory_id+__typename%7D__t" +
         "ypename%7Ddescription%7Bhtml+__typename%7Dshort_description%7Bhtml+__typename%7Dcustom_urgency+custom_preparation_mode+custom_use_care+custom_ingredients" +
         "+custom_nutritional_table+custom_brand+custom_manufacturer+custom_packing+custom_format+custom_weight+custom_dimensions+custom_flavor+id+media_gallery_entries%7Bid+label" +
         "+position+disabled+file+video_content%7Bvideo_url+__typename%7D__typename%7Dmeta_title+meta_description+meta_keyword+name+tags%7Bimage+name+__typename%7Dsku" +
         "+rating_summary+review_count+reviews%28pageSize%3A%24pageSize+currentPage%3A%24currentPage%29%7Bitems%7Bsummary+text+nickname+created_at+average_rating+ratings_breakdown%7Bname" +
         "+value+__typename%7D__typename%7Dpage_info%7Bcurrent_page+page_size+total_pages+__typename%7D__typename%7Dprice_tiers%7Bdiscount%7Bamount_off+percent_off+__typename" +
         "%7Dfinal_price%7Bcurrency+value+__typename%7Dquantity+__typename%7Dprice_range%7Bminimum_price%7Bregular_price%7Bvalue+currency+__typename%7Dfinal_price%7Bvalue+currency" +
         "+__typename%7Ddiscount%7Bpercent_off+__typename%7D__typename%7D__typename%7Dsmall_image%7Burl+__typename%7Dstock_status+related_products%7Bid+name+short_description%7Bhtml" +
         "+__typename%7Drating_summary+small_image%7Burl+__typename%7Durl_key+url_suffix+rating_summary+review_count+price_range%7Bminimum_price%7Bregular_price%7Bvalue+currency" +
         "+__typename%7Dfinal_price%7Bvalue+currency+__typename%7Ddiscount%7Bpercent_off+__typename%7D__typename%7D__typename%7Dcustom_urgency+custom_preparation_mode+custom_use_care" +
         "+custom_ingredients+custom_nutritional_table+custom_brand+custom_manufacturer+custom_packing+custom_format+custom_weight+custom_dimensions+custom_flavor+stock_status+...on" +
         "+ConfigurableProduct%7Bconfigurable_options%7Battribute_code+attribute_id+id+label+values%7Bdefault_label+label+store_label+use_default_value+value_index+swatch_data%7B...on" +
         "+ImageSwatchData%7Bthumbnail+__typename%7Dvalue+__typename%7D__typename%7D__typename%7Dvariants%7Battributes%7Bcode+value_index+uid+__typename%7Dproduct%7Bid+name+max_sale_qty" +
         "+media_gallery_entries%7Bid+disabled+file+label+position+__typename%7Dis_discount_recurrence+sku+stock_status" +
         "+custom_urgency+custom_preparation_mode+custom_use_care+custom_ingredients+custom_nutritional_table+custom_brand" +
         "+custom_manufacturer+custom_packing+custom_format+custom_weight+custom_dimensions+custom_flavor+rating_summary" +
         "+review_count+price_range%7Bminimum_price%7Bregular_price%7Bvalue+currency+__typename%7Dfinal_price%7Bvalue+currency" +
         "+__typename%7Ddiscount%7Bpercent_off+__typename%7D__typename%7D__typename%7D__typename%7D__typename%7D__typename%7D__typename%" +
         "7Durl_key+...on+ConfigurableProduct%7Bconfigurable_options%7Battribute_code+attribute_id+id+label+values%7Bdefault_label+label+store_label+use_default_value+value_index" +
         "+swatch_data%7B...on+ImageSwatchData%7Bthumbnail+__typename%7Dvalue+__typename%7D__typename%7D__typename%7Dvariants%7Battributes%7Bcode+value_index+uid+__typename%7Dproduct" +
         "%7Bid+name+max_sale_qty+is_discount_recurrence+media_gallery_entries%7Bid+disabled+file+label+position+__typename%7Dsku+stock_status+custom_urgency+custom_preparation_mode" +
         "+custom_use_care+custom_ingredients+custom_nutritional_table+custom_brand+custom_manufacturer+custom_packing+custom_format+custom_weight+custom_dimensions+custom_flavor" +
         "+rating_summary+review_count+price_tiers%7Bdiscount%7Bamount_off+percent_off+__typename%7Dfinal_price%7Bcurrency+value+__typename%7Dquantity+__typename%7Dprice_range" +
         "%7Bminimum_price%7Bregular_price%7Bvalue+currency+__typename%7Dfinal_price%7Bvalue+currency+__typename%7Ddiscount%7Bpercent_off+__typename%7D__typename%7D__typename" +
         "%7D__typename%7D__typename%7D__typename%7D%7D&operationName=getProductDetailForProductPage&variables=%7B%22pageSize%22%3A6%2C%22currentPage%22%3A1%2C%22urlKey%22%3A%22" +
         slug + "%22%7D";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      return this.dataFetcher
         .get(session, request);

   }

   private String getSlugFromUrl() {
      String slug = null;
      Pattern pattern = Pattern.compile("br\\/(.*)\\.html");
      Matcher matcher = pattern.matcher(session.getOriginalURL());
      if (matcher.find()) {
         slug = matcher.group(1);
      }
      return slug;

   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json != null && !json.isEmpty()) {
         JSONObject productJson = JSONUtils.getValueRecursive(json, "data.products.items.0", JSONObject.class);

         if (productJson != null && !productJson.isEmpty()) {

            Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

            String internalPid = productJson.optString("id");
            JSONArray variants = productJson.optJSONArray("variants");

            if (variants != null) {
               for (Object o : variants) {
                  if (o instanceof JSONObject) {
                     JSONObject variant = (JSONObject) o;
                     JSONObject productVariant = variant.optJSONObject("product");
                     Product product = extractProduct(productVariant, internalPid);
                     products.add(product);

                  }
               }
            } else {
               Product product = extractProduct(productJson, internalPid);
               products.add(product);

            }

         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Product extractProduct(JSONObject productJson, String internalPid) throws OfferException, MalformedPricingException, MalformedProductException {
      String internalId = productJson.optString("sku");
      String name = productJson.optString("name");
      String primaryImage = CrawlerUtils.completeUrl(JSONUtils.getValueRecursive(productJson, "media_gallery_entries.0.file", String.class), "https", "media.mundodanone.com.br/catalog/product");
      String stock = productJson.optString("stock_status");
      boolean available = stock != null ? !stock.contains("OUT_OF_STOCK") : null;
      String description = crawlDescription(productJson);
      Offers offers = available ? scrapOffers(productJson) : new Offers();
      RatingsReviews ratingsReviews = scrapRatingsReviews(productJson);
      return ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setPrimaryImage(primaryImage)
         .setDescription(description)
         .setRatingReviews(ratingsReviews)
         .setOffers(offers)
         .build();
   }

   private String crawlDescription(JSONObject productJson) {
      StringBuilder stringBuilder = new StringBuilder();
      String description = JSONUtils.getValueRecursive(productJson, "description.html", String.class);
      String shortDescription = JSONUtils.getValueRecursive(productJson, "short_description.html", String.class);
      String preparationMode = productJson.optString("custom_preparation_mode");
      String useCare = productJson.optString("custom_use_care");
      String ingredients = productJson.optString("custom_ingredients");
      String nutritionalTable = productJson.optString("custom_nutritional_table");
      if (description != null) {
         stringBuilder.append(description);
      }
      if (shortDescription != null) {
         stringBuilder.append("\n").append(shortDescription);
      }
      if (preparationMode != null) {
         stringBuilder.append("\n").append(preparationMode);
      }
      if (useCare != null) {
         stringBuilder.append("\n").append(useCare);
      }
      if (ingredients != null) {
         stringBuilder.append("\n").append(ingredients);
      }
      if (nutritionalTable != null) {
         stringBuilder.append("\n").append(nutritionalTable);
      }

      return stringBuilder.toString();

   }

   private RatingsReviews scrapRatingsReviews(JSONObject productJson) {

      RatingsReviews ratingsReviews = new RatingsReviews();

      Double average = getAverage(productJson);

      //sometimes site have a review but haven't a average.
      int review = productJson.optInt("review_count");
      if (average == 0){
         review = 0;
      }

      ratingsReviews.setDate(session.getDate());
      ratingsReviews.setTotalRating(review);
      ratingsReviews.setAverageOverallRating(average);
      ratingsReviews.setTotalWrittenReviews(review);

      return ratingsReviews;

   }


   private Double getAverage(JSONObject productJson){

      float averageInt = productJson.optInt("rating_summary");

      return (double) (averageInt * 5 / 100);

   }

   private Offers scrapOffers(JSONObject productJson) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productJson);
      List<String> sales = scrapSales(productJson);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private List<String> scrapSales(JSONObject productJson) {
      List<String> sales = new ArrayList<>();

      JSONObject saleJson = JSONUtils.getValueRecursive(productJson, "price_tiers.0", JSONObject.class);
      if (saleJson != null) {
         Integer discount = JSONUtils.getValueRecursive(saleJson, "discount.percent_off", Integer.class);
         Integer quantity = saleJson.optInt("quantity");
         Double priceDiscount = JSONUtils.getValueRecursive(productJson, "final_price.value", Double.class);
         StringBuilder stringBuilder = new StringBuilder();
         if (discount != null || quantity != 0 || priceDiscount != null ){
            stringBuilder.append(discount);
            stringBuilder.append("-").append("%: Compre ").append(quantity).append(" por ").append(priceDiscount).append("/ cada");
         }

         sales.add(stringBuilder.toString());

      }
      return sales;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {

      JSONObject priceInfo = productJson.optJSONObject("price_range");
      Double priceFrom = priceInfo != null ? JSONUtils.getValueRecursive(priceInfo, "value.final_price", Double.class) : null;
      Double spotlightPrice = priceInfo != null ? JSONUtils.getValueRecursive(priceInfo, "minimum_price.regular_price.value", Double.class) : null;
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
