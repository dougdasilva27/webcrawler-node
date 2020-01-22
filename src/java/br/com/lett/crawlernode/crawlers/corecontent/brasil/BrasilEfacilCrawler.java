package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilEfacilCrawler extends Crawler {

   public BrasilEfacilCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && href.startsWith("https://www.efacil.com.br/");
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (!doc.select("h1.product-name").isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         Element variationSelector = doc.select(".options_attributes").first();

         // Nome
         Element elementName = doc.select("h1.product-name").first();
         String name = null;
         if (elementName != null) {
            name = elementName.text().trim();
         }

         // Categorias
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#widget_breadcrumb ul li a", true);

         // Imagem primária
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "div.product-photo a", Arrays.asList("href"), "https:", "www.efacil.com.br");

         // Imagem secundária
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "div.thumbnails a", Arrays.asList("data-original", "href"), "https:",
               "www.efacil.com.br", primaryImage);

         // Descrição
         String description = "";
         Element elementSpecs = doc.select("#especificacoes").first();
         Element elementTabContainer = doc.select("#tabContainer").first();
         if (elementTabContainer != null)
            description += elementTabContainer.html();
         if (elementSpecs != null)
            description = description + elementSpecs.html();

         // Filtragem
         boolean mustInsert = true;

         // Estoque
         Integer stock = null;

         // Marketplace
         Marketplace marketplace = new Marketplace();

         if (variationSelector == null) { // sem variações

            // InternalPid
            String internalPid = null;
            Element elementInternalPid = doc.select("input#productId").first();
            if (elementInternalPid != null) {
               internalPid = elementInternalPid.attr("value").trim();
            } else {
               elementInternalPid = doc.select("input[name=productId]").first();
               if (elementInternalPid != null) {
                  internalPid = elementInternalPid.attr("value").trim();
               }
            }

            // ID interno
            String internalId = null;
            Element internalIdElement = doc.select("#entitledItem_" + internalPid).first();
            JSONObject info = new JSONObject();
            if (internalIdElement != null) {
               JSONArray infoArray = new JSONArray(internalIdElement.text().trim());
               info = infoArray.getJSONObject(0);

               internalId = info.getString("catentry_id");
            }

            // Disponibilidade
            boolean available = true;
            Element elementAvailable = doc.select("#disponibilidade-estoque").first();
            if (elementAvailable == null) {
               available = false;
            }

            // Prices
            Prices prices = new Prices();

            // Price
            Float price = null;

            if (available) {
               // Price BankTicket 1x
               Float priceBank = null;

               Element priceElement = doc.select(".priceby span[itemprop=price]").first();

               if (priceElement != null) {
                  priceBank = MathUtils.parseFloatWithComma(priceElement.ownText());
               }


               // Prices Json
               JSONArray jsonPrices = crawlPriceFromApi(internalId, priceBank, doc);

               // Prices
               prices = crawlPrices(priceBank, internalId, internalPid, jsonPrices);

               // Price
               price = crawlPrice(jsonPrices);
            }
            RatingsReviews ratingReviews = crawRating(doc, internalId);

            if (mustInsert) {

               Product product = new Product();
               product.setUrl(session.getOriginalURL());
               product.setInternalId(internalId);
               product.setInternalPid(internalPid);
               product.setName(name);
               product.setAvailable(available);
               product.setPrice(price);
               product.setPrices(prices);
               product.setCategory1(categories.getCategory(0));
               product.setCategory2(categories.getCategory(1));
               product.setCategory3(categories.getCategory(2));
               product.setPrimaryImage(primaryImage);
               product.setSecondaryImages(secondaryImages);
               product.setDescription(description);
               product.setStock(stock);
               product.setMarketplace(marketplace);
               product.setRatingReviews(ratingReviews);

               products.add(product);
            }

         }

         else { // múltiplas variações

            Element tmpIdElement = doc.select("input[id=productId]").first();
            String tmpId = null;
            if (tmpIdElement != null) {
               tmpId = tmpIdElement.attr("value").trim();
            }

            try {

               JSONArray variationsJsonInfo = new JSONArray(doc.select("#entitledItem_" + tmpId).text().trim());

               for (int i = 0; i < variationsJsonInfo.length(); i++) {

                  JSONObject variationJsonObject = variationsJsonInfo.getJSONObject(i);

                  // ID interno
                  String internalId = variationJsonObject.getString("catentry_id").trim();

                  // InternalPid
                  String internalPid = null;
                  Element elementInternalPid = doc.select("input#productId").first();
                  if (elementInternalPid != null) {
                     internalPid = elementInternalPid.attr("value").trim();
                  }

                  // Nome
                  JSONObject attributes = variationJsonObject.getJSONObject("Attributes");
                  String variationName = null;
                  if (attributes.has("Voltagem_110V")) {
                     if (name.contains("110V")) {
                        variationName = name;
                     } else if (name.contains("220V")) {
                        variationName = name.replace("220V", "110V");
                     } else {
                        variationName = name + " 110V";
                     }
                  } else if (attributes.has("Voltagem_220V")) {
                     if (name.contains("220V")) {
                        variationName = name;
                     } else if (name.contains("110V")) {
                        variationName = name.replace("110V", "220V");
                     } else {
                        variationName = name + " 220V";
                     }
                  } else {
                     variationName = name;
                  }

                  // Disponibilidade
                  boolean available = true;
                  if (variationJsonObject.getString("hasInventory").equals("false")) {
                     available = false;
                  }

                  // Prices
                  Prices prices = new Prices();

                  // Preço
                  Float price = null;

                  if (available) {
                     // Price BankTicket 1x
                     Float priceBank =
                           Float.parseFloat(variationJsonObject.getString("offerPrice").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

                     // Prices Json
                     JSONArray jsonPrices = crawlPriceFromApi(internalId, priceBank, doc);

                     // Prices
                     prices = crawlPrices(priceBank, internalId, internalPid, jsonPrices);

                     // Price
                     price = crawlPrice(jsonPrices);

                  }
                  RatingsReviews ratingReviews = crawRating(doc, internalId);


                  if (mustInsert) {

                     Product product = new Product();
                     product.setUrl(session.getOriginalURL());
                     product.setInternalId(internalId);
                     product.setInternalPid(internalPid);
                     product.setName(variationName);
                     product.setAvailable(available);
                     product.setPrice(price);
                     product.setPrices(prices);
                     product.setCategory1(categories.getCategory(0));
                     product.setCategory2(categories.getCategory(1));
                     product.setCategory3(categories.getCategory(2));
                     product.setPrimaryImage(primaryImage);
                     product.setSecondaryImages(secondaryImages);
                     product.setDescription(description);
                     product.setStock(stock);
                     product.setMarketplace(marketplace);
                     product.setRatingReviews(ratingReviews);
                     products.add(product);
                  }

               }
            } catch (Exception e) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
            }
         } // fim do caso de múltiplas variacoes

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   /**
    * Foi definido que o preço principal seria a menor parcela a prazo Logo tento sempre pegar a 2
    * parcela do produto caso esse produto tenha somente uma parcela, o preço principal será o preço da
    * primeira parcela.
    * 
    * @param jsonPrices
    * @return
    */
   private Float crawlPrice(JSONArray jsonPrices) {
      Float price = null;

      for (int i = 0; i < jsonPrices.length(); i++) {
         JSONObject json = jsonPrices.getJSONObject(i);

         if (json.has("installmentOptions")) {
            JSONArray installments = json.getJSONArray("installmentOptions");

            for (int j = 0; j < installments.length(); j++) {
               JSONObject installmentJSON = installments.getJSONObject(j);

               if (installmentJSON.has("option")) {
                  String installment = installmentJSON.getString("option");

                  // Vai rodando os cartões até achar a segunda parcela
                  // se achar para o loop
                  if (installment.contains("2")) {
                     Float valueInstallment = Float.parseFloat(installmentJSON.getString("amount"));
                     Float result = valueInstallment * 2;

                     price = MathUtils.normalizeTwoDecimalPlaces(result);

                     break;

                  } else if (installment.contains("1")) { // se não achar o preço será o da primeira parcela
                     Float valueInstallment = Float.parseFloat(installmentJSON.getString("amount"));
                     price = MathUtils.normalizeTwoDecimalPlaces(valueInstallment);
                  }
               }
            }
         }
      }



      return price;
   }

   private Prices crawlPrices(Float price, String internalId, String internalPid, JSONArray jsonPrices) {
      Prices prices = new Prices();

      if (price != null) {

         JSONObject priceSimpleJson = crawlPriceFromApi(internalId, internalPid);

         prices.setBankTicketPrice(CrawlerUtils.getDoubleValueFromJSON(priceSimpleJson, "offerPriceAV", false, true));
         prices.setPriceFrom(CrawlerUtils.getDoubleValueFromJSON(priceSimpleJson, "listPrice", false, true));

         try {
            for (int i = 0; i < jsonPrices.length(); i++) {
               JSONObject json = jsonPrices.getJSONObject(i);
               Map<Integer, Float> installmentPriceMap = new HashMap<>();
               if (json.has("paymentMethodName")) {
                  String cardName = json.getString("paymentMethodName").replaceAll(" ", "").toLowerCase().trim();

                  if (json.has("installmentOptions")) {
                     JSONArray installments = json.getJSONArray("installmentOptions");

                     for (int j = 0; j < installments.length(); j++) {
                        JSONObject installmentJSON = installments.getJSONObject(j);

                        if (installmentJSON.has("option")) {
                           String text = installmentJSON.getString("option").toLowerCase();
                           int x = text.indexOf('x');

                           Integer installment = Integer.parseInt(text.substring(0, x));

                           if (installment == 1) {
                              installmentPriceMap.put(installment, price);
                              continue;
                           }

                           if (installmentJSON.has("amount")) {
                              Float priceBig = Float.parseFloat(installmentJSON.getString("amount"));
                              Float value = MathUtils.normalizeTwoDecimalPlaces(priceBig);

                              installmentPriceMap.put(installment, value);
                           }
                        }
                     }

                     if (cardName.equals("amex")) {
                        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

                     } else if (cardName.equals("visa")) {
                        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

                     } else if (cardName.equals("mastercard")) {
                        prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

                     } else if (cardName.equals("diners")) {
                        prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

                     } else if (cardName.equals("americanexpress")) {
                        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

                     } else if (cardName.equals("elo")) {
                        prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
                     }
                  }
               }
            }
         } catch (Exception e) {

         }
      }

      return prices;
   }

   /**
    * Pega o JSON filho que possui informações sobre as parecelas.
    * 
    * @param internalId
    * @param price
    * @return
    */
   private JSONArray crawlPriceFromApi(String internalId, Float price, Document doc) {
      Float priceApi = price;

      Element priceOffer = doc.select("input[id^=\"offerPrice_hd_\"]").first();
      if (priceOffer != null) {
         priceApi = Float.parseFloat(priceOffer.val());
      }

      String url = "https://www.efacil.com.br/webapp/wcs/stores/servlet/GetCatalogEntryInstallmentPrice?storeId=10154&langId=-6&catalogId=10051"
            + "&catalogEntryId=" + internalId + "&nonInstallmentPrice=" + priceApi;

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      String json = this.dataFetcher.get(session, request).getBody();

      if (json.contains("/*") && json.contains("*/")) {
         int x = json.indexOf("/*");
         int y = json.indexOf("*/", x + 2);

         json = json.substring(x + 2, y);
      }

      return CrawlerUtils.stringToJsonArray(json);
   }

   private JSONObject crawlPriceFromApi(String internalId, String internalPid) {
      JSONObject priceJson = new JSONObject();

      String url = "https://www.efacil.com.br/webapp/wcs/stores/servlet/GetCatalogEntryDetailsByIDView?storeId=10154&langId=-6&catalogId=10051"
            + "&catalogEntryId=" + internalId + "&productId=" + internalPid + "&parcelaEmDestaque=";

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      String json = this.dataFetcher.get(session, request).getBody();

      if (json.contains("/*") && json.contains("*/")) {
         int x = json.indexOf("/*");
         int y = json.lastIndexOf("*/");

         json = json.substring(x + 2, y);
      }

      JSONObject catalog = CrawlerUtils.stringToJson(json);

      if (catalog.has("catalogEntry")) {
         priceJson = catalog.getJSONObject("catalogEntry");
      }

      return priceJson;
   }

   private RatingsReviews crawRating(Document doc, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();


      String trustVoxId = crawlTrustVoxId(doc);
      JSONObject trustVoxResponse = requestTrustVoxEndpoint(trustVoxId);
      Integer totalNumOfEvaluations = getTotalNumOfRatings(trustVoxResponse);

      Double totalRating = getTotalRating(trustVoxResponse);
      AdvancedRatingReview advancedRatingReview = getTotalStarsFromEachValue(trustVoxResponse);

      Double avgRating = null;
      if (totalNumOfEvaluations > 0) {
         avgRating = MathUtils.normalizeTwoDecimalPlaces(totalRating / totalNumOfEvaluations);
      }


      ratingReviews.setDate(session.getDate());
      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Integer getTotalNumOfRatings(JSONObject trustVoxResponse) {
      if (trustVoxResponse.has("items")) {
         JSONArray ratings = trustVoxResponse.getJSONArray("items");
         return ratings.length();
      }
      return 0;
   }

   private Double getTotalRating(JSONObject trustVoxResponse) {
      Double totalRating = 0.0;
      if (trustVoxResponse.has("items")) {
         JSONArray ratings = trustVoxResponse.getJSONArray("items");

         for (int i = 0; i < ratings.length(); i++) {
            JSONObject rating = ratings.getJSONObject(i);

            if (rating.has("rate")) {
               totalRating += rating.getInt("rate");
            }
         }
      }
      return totalRating;
   }

   public static AdvancedRatingReview getTotalStarsFromEachValue(JSONObject trustVoxResponse) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;


      if (trustVoxResponse.has("items")) {
         JSONArray ratingDistribution = JSONUtils.getJSONArrayValue(trustVoxResponse, "items");

         for (int i = 0; i < ratingDistribution.length(); i++) {

            JSONObject valueCount = ratingDistribution.get(i) instanceof JSONObject ? ratingDistribution.getJSONObject(i) : new JSONObject();

            int numberOfStars = JSONUtils.getIntegerValueFromJSON(valueCount, "rate", 0);


            switch (numberOfStars) {
               case 5:
                  star5 += 1;
                  break;
               case 4:
                  star4 += 1;
                  break;
               case 3:
                  star3 += 1;
                  break;
               case 2:
                  star2 += 1;
                  break;
               case 1:
                  star1 += 1;
                  break;
               default:
                  break;
            }
         }

      }


      return new AdvancedRatingReview.Builder()
            .totalStar1(star1)
            .totalStar2(star2)
            .totalStar3(star3)
            .totalStar4(star4)
            .totalStar5(star5)
            .build();
   }

   private JSONObject requestTrustVoxEndpoint(String id) {
      StringBuilder requestURL = new StringBuilder();

      requestURL.append("http://trustvox.com.br/widget/opinions?code=");
      requestURL.append(id);

      requestURL.append("&");
      requestURL.append("store_id=545");

      requestURL.append("&url=");
      requestURL.append(session.getOriginalURL());

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


   private String crawlTrustVoxId(Document doc) {
      String trustVoxId = null;

      Element elementInternalPid = doc.select(".product-code #product-code").first();
      if (elementInternalPid != null) {
         trustVoxId = elementInternalPid.text().trim();
      }

      return trustVoxId;
   }

}
