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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
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

   private String getProductId() {
      String[] url = session.getOriginalURL().split("/");
      return url[url.length - 1].split("\\?")[0];
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (!json.isEmpty()) {

         JSONObject productJson = JSONUtils.getValueRecursive(json, "data.products.items.0", JSONObject.class);

         if (!productJson.isEmpty()) {

            Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
            Object dataJson = json.query("/items");

            String internalPid = productJson.optString("id");
            String description = JSONUtils.getValueRecursive(dataJson, "0.x_caracteristicasHtml", String.class);
            JSONArray variants = productJson.optJSONArray("variants");

            if (variants != null) {
               for (Object o : variants) {
                  if (o instanceof JSONObject) {
                     JSONObject variant = (JSONObject) o;
                     JSONObject productVariant = variant.optJSONObject("product");
                     String internalId = productVariant.optString("sku");
                     String name = productVariant.optString("name");
                     JSONArray imageJson = productVariant.optJSONArray("media_gallery_entries");
                     List<String> images = imageJson != null ? CrawlerUtils.scrapImagesListFromJSONArray(imageJson, "file", null, "https", "media.mundodanone.com.br", session) : null;
                     String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
                     String stock = productVariant.optString("stock_status");
                     boolean available = stock != null && !stock.contains("OUT_OF_STOCK");
                     Offers offers = available ? scrapOffers(productVariant) : new Offers();
                     RatingsReviews ratingsReviews = scrapRatingsReviews(internalId);
                     Product product = ProductBuilder.create()
                        .setUrl(session.getOriginalURL())
                        .setInternalId(internalId)
                        .setInternalPid(internalPid)
                        .setName(name)
                        .setPrimaryImage(primaryImage)
                        .setSecondaryImages(images)
                        .setDescription(description)
                        .setRatingReviews(ratingsReviews)
                        .setOffers(offers)
                        .build();
                     products.add(product);

                  }
               }
            } else {
               String internalId = productJson.optString("sku");
               String name = JSONUtils.getValueRecursive(dataJson, "0.displayName", String.class);
               JSONArray imageJson = JSONUtils.getValueRecursive(dataJson, "0.largeImageURLs", JSONArray.class);
               List<String> images = imageJson != null ? CrawlerUtils.scrapImagesListFromJSONArray(imageJson, null, null, "https", "www.taqi.com.br", session) : null;
               String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
               String stock = JSONUtils.getValueRecursive(stockInfo, "items.0.stockStatus", String.class);
               boolean available = stock != null ? !stock.contains("OUT_OF_STOCK") : null;
               Offers offers = available ? scrapOffers(dataJson) : new Offers();
               RatingsReviews ratingsReviews = scrapRatingsReviews(internalId);
               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(images)
                  .setDescription(description)
                  .setRatingReviews(ratingsReviews)
                  .setOffers(offers)
                  .build();
               products.add(product);

            }


            // Creating the product


         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private RatingsReviews scrapRatingsReviews(String internalId) {

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json; charset=UTF-8");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.taqi.com.br/ccstorex/custom/v1/hervalApiCalls/getData")
         .setPayload("{\"url\":\"/Produtos/" + internalId + "/avaliacoes\",\"data\":{},\"method\":\"GET\"}")
         .setHeaders(headers)
         .build();
      Response response = new JsoupDataFetcher().post(session, request);
      JSONArray data = JSONUtils.stringToJsonArray(response.getBody());

      RatingsReviews ratingsReviews = new RatingsReviews();

      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(data);

      ratingsReviews.setDate(session.getDate());
      ratingsReviews.setTotalRating(data.length());
      ratingsReviews.setAverageOverallRating(CrawlerUtils.extractRatingAverageFromAdvancedRatingReview(advancedRatingReview));
      ratingsReviews.setTotalWrittenReviews(data.length());
      ratingsReviews.setAdvancedRatingReview(advancedRatingReview);


      return ratingsReviews;


   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONArray data) {
      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();
      int[] notas = {0, 0, 0, 0, 0};
      for (Object o : data) {
         JSONObject obj = (JSONObject) o;


         switch (obj.optInt("nota")) {
            case 1:
               notas[0]++;
            case 2:
               notas[1]++;
            case 3:
               notas[2]++;
            case 4:
               notas[3]++;
            case 5:
               notas[4]++;
         }
      }

      advancedRatingReview.setTotalStar1(notas[0]);
      advancedRatingReview.setTotalStar2(notas[1]);
      advancedRatingReview.setTotalStar3(notas[2]);
      advancedRatingReview.setTotalStar4(notas[3]);
      advancedRatingReview.setTotalStar5(notas[4]);

      return advancedRatingReview;
   }

   private Offers scrapOffers(Object jsonObject) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonObject);
      List<String> sales = scrapSales(pricing);

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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String salesOnJson = CrawlerUtils.calculateSales(pricing);
      sales.add(salesOnJson);
      return sales;
   }

   private Map<String, String> getHeaders() {
      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
      headers.put("Referer", session.getOriginalURL());
      headers.put("content-type", "application/json; charset=UTF-8");

      return headers;
   }


   private JSONObject getPrices() {
      Map<String, String> headers = getHeaders();
      String API = "https://www.taqi.com.br/ccstorex/custom/v1/parcelamento/getParcelamentos";
      String payload = "{\"produtos\":[{\"id\":\"" + getProductId() + "\",\"quantity\":1}],\"siteId\":\"siteUS\",\"catalogId\":\"cloudCatalog\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setHeaders(headers)
         .setPayload(payload)
         .build();
      String content = new JsoupDataFetcher()
         .post(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);

   }

   private Pricing scrapPricing(Object jsonObject) throws MalformedPricingException {

      JSONObject priceInfo = getPrices();
      Double priceFrom = JSONUtils.getValueRecursive(jsonObject, "0.listPrices.real", Double.class);
      String spotlightPriceStr = JSONUtils.getValueRecursive(priceInfo, "produtos.0.parcelas.boleto.0.value", String.class);
      Double spotlightPrice = spotlightPriceStr != null ? MathUtils.parseDoubleWithDot(spotlightPriceStr) : null;
      CreditCards creditCards = scrapCreditCards(priceInfo);
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

   private CreditCards scrapCreditCards(JSONObject priceInfo) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      JSONArray installmentsArray = JSONUtils.getValueRecursive(priceInfo, "produtos.0.parcelas.outros", JSONArray.class);
      Installments installments = new Installments();
      if (installmentsArray != null) {
         for (Object obj : installmentsArray) {
            if (obj instanceof JSONObject) {
               JSONObject installmentsJson = (JSONObject) obj;
               Integer key = installmentsJson.optInt("key");
               String valueStr = installmentsJson.optString("total");
               Double value = MathUtils.parseDoubleWithDot(valueStr);
               Double total = installmentsJson.optDouble("total");
               installments.add(Installment.InstallmentBuilder.create()
                  .setInstallmentNumber(key)
                  .setFinalPrice(total)
                  .setInstallmentPrice(value)
                  .build());

            }
         }
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
