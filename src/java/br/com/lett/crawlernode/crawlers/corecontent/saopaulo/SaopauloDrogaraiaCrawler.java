package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaopauloDrogaraiaCrawler extends Crawler {

   private final String HOME_PAGE = "http://www.drogaraia.com.br/";

   public SaopauloDrogaraiaCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         // ID interno
         String internalId = crawlInternalId(doc);

         // Pid
         String internalPid = null;
         Element elementInternalPid = doc.select("input[name=product]").first();
         if (elementInternalPid != null) {
            internalPid = elementInternalPid.attr("value").trim();
         }

         // Disponibilidade
         boolean available = true;
         Element buyButton = doc.select(".add-to-cart button").first();

         if (buyButton == null) {
            available = false;
         }

         // Nome
         String name = crawlName(doc);

         // Preço
         Float price = null;
         Element elementPrice = doc.select(".product-shop .regular-price").first();
         if (elementPrice == null) {
            elementPrice = doc.select(".product-shop .price-box .special-price .price").first();
         }
         if (elementPrice != null) {
            price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
         }

         // Categorias
         Elements elementsCategories = doc.select(".breadcrumbs ul li:not(.home):not(.product) a");
         String category1 = "";
         String category2 = "";
         String category3 = "";
         for (Element category : elementsCategories) {
            if (category1.isEmpty()) {
               category1 = category.text();
            } else if (category2.isEmpty()) {
               category2 = category.text();
            } else if (category3.isEmpty()) {
               category3 = category.text();
            }
         }

         String description = scrapDescription(doc);
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = crawlSecondaryImages(doc, primaryImage);
         Integer stock = null;
         Marketplace marketplace = new Marketplace();
         Prices prices = crawlPrices(doc, price);
         String ean = scrapEan(doc);
         RatingsReviews ratingReviews = crawRating(doc);
         List<String> eans = new ArrayList<>();
         eans.add(ean);

         Product product = new Product();

         product.setUrl(session.getOriginalURL());
         product.setInternalId(internalId);
         product.setInternalPid(internalPid);
         product.setName(name);
         product.setPrice(price);
         product.setPrices(prices);
         product.setCategory1(category1);
         product.setCategory2(category2);
         product.setCategory3(category3);
         product.setPrimaryImage(primaryImage);
         product.setSecondaryImages(secondaryImages);
         product.setDescription(description);
         product.setStock(stock);
         product.setMarketplace(marketplace);
         product.setAvailable(available);
         product.setRatingReviews(ratingReviews);
         product.setEans(eans);

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(Document document) {
      return !document.select(".product-name h1").isEmpty();
   }

   private String crawlName(Document doc) {
      StringBuilder name = new StringBuilder();

      Element firstName = doc.selectFirst(".product-name h1");
      if (firstName != null) {
         name.append(firstName.text());

         Elements attributes = doc.select(".product-attributes .show-hover");
         for (Element e : attributes) {
            name.append(" ").append(e.ownText().trim());
         }
      }

      return name.toString().replace("  ", " ").trim();
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;
      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer.push(", ");", true, false);

      if (json.has("ecommerce")) {
         JSONObject ecommerce = json.getJSONObject("ecommerce");

         if (ecommerce.has("detail")) {
            JSONObject detail = ecommerce.getJSONObject("detail");

            if (detail.has("products")) {
               JSONArray products = detail.getJSONArray("products");

               if (products.length() > 0) {
                  JSONObject product = products.getJSONObject(0);

                  if (product.has("id")) {
                     internalId = product.getString("id");
                  }
               }
            }
         }
      }

      return internalId;
   }

   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;

      Element elementPrimaryImage = doc.select(".product-image-gallery img.gallery-image").first();
      if (elementPrimaryImage != null) {
         primaryImage = elementPrimaryImage.attr("data-zoom-image");
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document doc, String primaryImage) {
      String secondaryImages = null;
      JSONArray secundaryImagesArray = new JSONArray();
      Elements elementImages = doc.select(".product-image-gallery img.gallery-image");

      for (Element e : elementImages) {
         String image = e.attr("data-zoom-image").trim();

         if (!isPrimaryImage(image, primaryImage)) {
            secundaryImagesArray.put(image);
         }
      }

      if (secundaryImagesArray.length() > 0) {
         secondaryImages = secundaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private boolean isPrimaryImage(String image, String primaryImage) {
      if (primaryImage == null) {
         return false;
      }

      String x = CommonMethods.getLast(image.split("/"));
      String y = CommonMethods.getLast(primaryImage.split("/"));

      return x.equals(y);
   }

   /**
    * In this market, installments not appear in product page
    * 
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Document doc, Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new HashMap<>();

         installmentPriceMap.put(1, price);
         prices.setBankTicketPrice(price);

         Element priceFrom = doc.select(".old-price span[id]").first();
         if (priceFrom != null) {
            prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
         }

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      }

      return prices;
   }

   private String scrapEan(Element e) {
      String ean = null;
      Elements trElements = e.select(".farmaBox .data-table tr");

      if (trElements != null && !trElements.isEmpty()) {
         for (Element tr : trElements) {
            if (tr.text().contains("EAN")) {
               Element td = tr.selectFirst("td");
               ean = td != null ? td.text().trim() : null;
            }
         }
      }

      return ean;
   }

   private String scrapDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element shortDescription = doc.selectFirst(".product-short-description");
      if (shortDescription != null) {
         description.append(shortDescription.html().trim());
      }

      Element elementDescription = doc.selectFirst("#details");
      if (elementDescription != null) {
         description.append(elementDescription.html().trim());
      }

      Elements iframes = doc.select(".product-essential iframe");
      for (Element iframe : iframes) {
         String url = iframe.attr("src");
         if (!url.contains("youtube")) {
            description
                  .append(Jsoup.parse(this.dataFetcher.get(session, RequestBuilder.create().setUrl(url).setCookies(cookies).build()).getBody()).html());
         }
      }

      return description.toString();
   }


   private RatingsReviews crawRating(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();

      String internalId = crawlInternalId(doc);
      ratingReviews.setDate(session.getDate());
      ratingReviews.setInternalId(internalId);
      JSONObject trustVoxResponse = requestTrustVoxEndpoint(internalId);
      Integer total = getTotalNumOfRatings(trustVoxResponse);
      AdvancedRatingReview advancedRatingReview = TrustvoxRatingCrawler.getTotalStarsFromEachValueWithRate(trustVoxResponse);

      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setTotalRating(total);
      ratingReviews.setTotalWrittenReviews(total);
      ratingReviews.setAverageOverallRating(getTotalRating(trustVoxResponse));

      return ratingReviews;
   }



   private Integer getTotalNumOfRatings(JSONObject trustVoxResponse) {
      if (trustVoxResponse.has("rate")) {
         JSONObject rate = trustVoxResponse.getJSONObject("rate");

         if (rate.has("count")) {
            return rate.getInt("count");
         }
      }
      return 0;
   }

   private Double getTotalRating(JSONObject trustVoxResponse) {
      Double totalRating = 0.0;
      if (trustVoxResponse.has("rate")) {
         JSONObject rate = trustVoxResponse.getJSONObject("rate");

         if (rate.has("average")) {
            totalRating = rate.getDouble("average");
         }
      }

      return totalRating;
   }

   private JSONObject requestTrustVoxEndpoint(String id) {
      StringBuilder requestURL = new StringBuilder();

      requestURL.append("http://trustvox.com.br/widget/root?code=");
      requestURL.append(id);

      requestURL.append("&");
      requestURL.append("store_id=71450");

      requestURL.append("&");
      try {
         requestURL.append(URLEncoder.encode(session.getOriginalURL(), "UTF-8"));
      } catch (UnsupportedEncodingException e1) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e1));
      }

      requestURL.append("&product_extra_attributes%5Bsubgroup%5D");

      Map<String, String> headerMap = new HashMap<>();
      headerMap.put(HttpHeaders.ACCEPT, "application/vnd.trustvox-v2+json");
      headerMap.put(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");

      Request request = RequestBuilder.create().setUrl(requestURL.toString()).setCookies(cookies).setHeaders(headerMap).build();
      String response = this.dataFetcher.get(session, request).getBody();

      JSONObject trustVoxResponse;
      try {
         trustVoxResponse = new JSONObject(response);
      } catch (JSONException e) {
         Logging.printLogWarn(logger, session, "Error creating JSONObject from trustvox response.");
         Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));

         trustVoxResponse = new JSONObject();
      }

      return trustVoxResponse;
   }
}
