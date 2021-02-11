package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import models.RatingsReviews;
import models.pricing.*;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jooq.tools.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ucar.nc2.util.net.URLencode;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.*;

public class ColombiaAlkostoCrawler extends Crawler {
   private static final String SELLER_FULL_NAME = "Alkosto";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   public ColombiaAlkostoCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-ids span", true);
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div.yotpo.bottomLine", "data-product-id");

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name h1", true);
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-img-box img", "src");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".image-extra a", Arrays.asList("href"), "https", "media.aws.alkosto.com", primaryImage);

         //Site hasn't categories
         String description = crawlDescription(doc);
         boolean available = !doc.select(".availability.in-stock").isEmpty(); //I didn't find any product unavailable to test
         Offers offers = available ? scrapOffers(doc) : new Offers();
         RatingsReviews ratingsReviews = crawlRating(internalPid);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-essential") != null;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();
      Elements elements = doc.select(".data-table tbody");
      if (elements != null) {
         for (Element el : elements) {
            description.append(el.selectFirst(".label"));
            description.append(": ");
            description.append(el.selectFirst(".data"));

         }
      }

      return description.toString();
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      if (scrapSaleDiscount(pricing) != null) {
         sales.add(scrapSaleDiscount(pricing));

      }

      return sales;
   }

   private String scrapSaleDiscount(Pricing pricing) {

      return CrawlerUtils.calculateSales(pricing);
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setSales(sales)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price .price", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-old", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
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

   private String getLoadPayload(String internalPid) throws UnsupportedEncodingException {
      JSONObject payload1 = new JSONObject();
      JSONObject payload2 = new JSONObject();
      JSONObject pidJson = new JSONObject();
      pidJson.put("pid", internalPid);
      pidJson.put("order_metadata_fields", new JSONObject());
      pidJson.put("index", 0);
      pidJson.put("element_id", "1");
      JSONObject pidJson2 = new JSONObject();
      pidJson2.put("pid", internalPid);
      pidJson2.put("link", session.getOriginalURL());
      pidJson2.put("skip_average_score", false);
      pidJson2.put("main_widget_pid", internalPid);
      pidJson2.put("index", 1);
      pidJson2.put("element_id", "2");

      payload1.put("method", "main_widget");
      payload1.put("params", pidJson);
      payload2.put("method", "bottomline");
      payload2.put("params", pidJson2);

      JSONArray array = new JSONArray();
      array.add(0, payload1);
      array.add(1, payload2);

      String jsonString = array.toString();
      StringBuilder payload = new StringBuilder();
      payload.append("methods=");
      payload.append(URLEncoder.encode(jsonString,"UTF-8"));

      return "methods=%5B%7B%22method%22%3A%22main_widget%22%2C%22params%22%3A%7B%22pid%22%3A%22100735%22%2C%22order_metadata_fields%22%3A%7B%7D%2C%22index%22%3A0%2C%22element_id%22%3A%221%22%7D%7D%2C%7B%22method%22%3A%22bottomline%22%2C%22params%22%3A%7B%22pid%22%3A%22100735%22%2C%22link%22%3A%22http%3A%2F%2Fwww.alkosto.com%2Fcelular-motorola-e7-plus-64gb-azul-mystic-blue%3F___store%3Dvalk1%26___from_store%3Dvalk1%22%2C%22skip_average_score%22%3Afalse%2C%22main_widget_pid%22%3A%22100735%22%2C%22index%22%3A1%2C%22element_id%22%3A%222%22%7D%7D%5D&app_key=RLyMqbdTOZKQpib0my4aigX84TiDqpFXeYeBIwbn&is_mobile=false&widget_version=2020-07-22_13-49-20";

  //   return payload.toString();
//      {"method":"main_widget","params":{"index":0,"pid":"100735","element_id":"1","order_metadata_fields":{}}},
//      {"method":"bottomline","params":{"link":"https://www.alkosto.com/celular-motorola-e7-plus-64gb-azul-mystic-blue#review-yotpo","index":1,"pid":"100735","main_widget_pid":"100735","element_id":"2","skip_average_score":false}}
//
//     [{"method":"main_widget","params":{"pid":"100735","order_metadata_fields":{},"index":0,"element_id":"1"}},
//      {"method":"bottomline","params":{"pid":"100735","link":"https://www.alkosto.com/celular-motorola-e7-plus-64gb-azul-mystic-blue#review-yotpo","skip_average_score":false,"main_widget_pid":"100735","index":1,"element_id":"2"}}]

   }


   private RatingsReviews crawlRating(String internalPid) throws UnsupportedEncodingException {
      RatingsReviews ratingReviews = new RatingsReviews();
      String payload = getLoadPayload(internalPid);


      String apiUrl = "https://staticw2.yotpo.com/batch/RLyMqbdTOZKQpib0my4aigX84TiDqpFXeYeBIwbn/" + internalPid;
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type","text/plain");



      Request request = Request.RequestBuilder.create().setUrl("https://staticw2.yotpo.com/batch/RLyMqbdTOZKQpib0my4aigX84TiDqpFXeYeBIwbn/100735").setHeaders(headers).setPayload(payload).build();

      JSONObject json = CrawlerUtils.stringToJson(dataFetcher.post(session, request).getBody());
      String html = JSONUtils.getValueRecursive(json, "0.result", String.class);

      Document doc = Jsoup.parse(html);

//      Integer totalNumOfEvaluations = CrawlerUtils.
//
//      Double avgRating = JSONUtils.getDoubleValueFromJSON(reviewStatistics, "AverageOverallRating", true);
//
//      ratingReviews.setInternalId(internalPid);
//      ratingReviews.setTotalRating(totalNumOfEvaluations);
//      ratingReviews.setAverageOverallRating(avgRating);
//      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      return ratingReviews;
   }

//   private RatingsReviews ratingsReviews (){
////      https://staticw2.yotpo.com/batch/RLyMqbdTOZKQpib0my4aigX84TiDqpFXeYeBIwbn/100735
//   }

   //Site hasn't rating

}
