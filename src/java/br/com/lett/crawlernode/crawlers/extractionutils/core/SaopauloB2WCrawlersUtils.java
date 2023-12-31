package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SaopauloB2WCrawlersUtils {

   /**
    * B2W has two types of crawler
    * <p>
    * Old Way: Shoptime New Way: Americanas and Submarino
    * <p>
    * this happen because americanas and submarino have changed their sites, but shoptime no
    */

   public static final String AMERICANAS = "americanas";
   public static final String SUBMARINO = "submarino";
   public static final String SHOPTIME = "shoptime";

   /**
    * New way
    */

   /**
    * @param doc
    * @return
    */
   public static JSONObject getDataLayer(Document doc) {
      JSONObject skus = new JSONObject();
      Elements scripts = doc.select("script");

      for (Element e : scripts) {
         String json = e.outerHtml();

         if ((json.contains("__PRELOADED_STATE__") || json.contains("__INITIAL_STATE__")) && json.contains("}")) {

            int x = json.indexOf("_ =") + 3;
            int y = json.lastIndexOf('}');

            json = json.substring(x, y + 1);
            skus = CrawlerUtils.stringToJson(json);

            break;
         }
      }

      return skus;
   }

   /**
    * Nesse novo site da americanas todas as principais informações dos skus estão em um json no html,
    * esse json é muito grande, por isso pego somente o que preciso e coloco em outro json para
    * facilitar a captura de informações
    * <p>
    * { internalPid = '51612', skus:[ { internal_id: '546', variationName: '110v' } ], images:{
    * primaryImage: '123.jpg'. secondaryImages: [ '1.jpg', '2.jpg' ] }, categories:[ { id: '123', name:
    * 'cafeteira' } ], prices:{ 546:{ stock: 1706 bankTicket: 59.86 installments: [ { quantity: 1,
    * value: 54.20 } ] } }
    * <p>
    * }
    */

   public static JSONObject assembleJsonProductWithNewWay(JSONObject apolloJson) {
      JSONObject jsonProduct = new JSONObject();

      JSONObject productJson = extractProductFromApollo(apolloJson);

      JSONArray skus = optJSONSkusApollo(apolloJson);

      jsonProduct.put("skus", skus);

      String internalPid = null;

      if (productJson.has("id")) {
         internalPid = productJson.optString("id");
         jsonProduct.put("internalPid", internalPid);
      }

      if (productJson.has("name")) {
         jsonProduct.put("name", productJson.get("name"));
      }

      JSONObject jsonImages = optJSONImages(productJson);
      jsonProduct.put("images", jsonImages);

      JSONArray jsonCategories = optJSONCategories(productJson);
      jsonProduct.put("categories", jsonCategories);

      JSONObject rating = optJSONRating(productJson);
      jsonProduct.put("rating", rating);


      return jsonProduct;

   }


   private static JSONObject extractProductFromApollo(JSONObject apollo) {
      JSONObject product = new JSONObject();

      JSONObject root = apollo.optJSONObject("ROOT_QUERY");
      if (root != null) {
         for (String key : root.keySet()) {
            if (key.startsWith("product")) {
               product = root.optJSONObject(key);
               break;
            }
         }
      }

      return product;
   }

   private static JSONArray optJSONSkus(JSONObject productJson) {
      JSONArray skus = new JSONArray();

      if (productJson.has("skus")) {
         JSONArray skusJson = productJson.optJSONArray("skus");

         for (int i = 0; i < skusJson.length(); i++) {
            JSONObject skuJson = skusJson.optJSONObject(i);
            skus.put(extractSingleSku(skuJson));
         }
      }

      return skus;
   }

   private static JSONArray optJSONSkusApollo(JSONObject apollo) {
      JSONArray skus = new JSONArray();

      for (String key : apollo.keySet()) {
         if (key.startsWith("Sku:")) {
            skus.put(extractSingleSku(apollo.optJSONObject(key)));
         }
      }

      return skus;
   }

   private static JSONObject extractSingleSku(JSONObject skuJson) {
      JSONObject sku = new JSONObject();

      if (skuJson.has("id")) {
         sku.put("internalId", skuJson.optString("id"));

         if (skuJson.has("name")) {
            StringBuilder name = new StringBuilder();
            sku.put("name", skuJson.optString("name"));

            if (skuJson.has("diffs") && !skuJson.isNull("diffs")) {
               JSONArray diffs = skuJson.optJSONArray("diffs");

               for (int j = 0; j < diffs.length(); j++) {
                  JSONObject variation = diffs.optJSONObject(j);

                  if (variation.has("value")) {
                     name.append(" " + variation.optString("value").trim());
                  }
               }

               sku.put("variationName", name.toString().trim());
            }
         }
         if (skuJson.has("eans") && !skuJson.isNull("eans")) {
            sku.put("eans", skuJson.optJSONArray("eans"));
         }

         JSONObject offers = getJson(skuJson, "offers({\"promoted\":true,\"sellerId\":\"\"})");
         if (offers != null && offers.has("result")) {
            sku.put("offers", offers.optJSONArray("result"));
         }
      }

      return sku;
   }

   private static JSONArray optJSONCategories(JSONObject productJson) {
      JSONArray jsonCategories = new JSONArray();

      if (productJson.has("category")) {
         JSONObject category = productJson.optJSONObject("category");

         if (category.has("breadcrumb")) {
            jsonCategories = category.optJSONArray("breadcrumb");
         }
      }

      return jsonCategories;
   }

   private static JSONObject optJSONRating(JSONObject productJson) {
      JSONObject jsonRating = new JSONObject();

      if (productJson.has("rating")) {
         JSONObject rating = productJson.optJSONObject("rating");
         jsonRating.put("rating", rating);
      }

      JSONObject reviews = getJson(productJson, "reviews");
      JSONArray result = reviews != null && !reviews.isEmpty() ? reviews.optJSONArray("result") : new JSONArray();
      if (result != null && !result.isEmpty()) {
         jsonRating.put("reviews", reviews);
      }


      return jsonRating;
   }


   private static JSONObject optJSONImages(JSONObject productJson) {
      JSONObject jsonImages = new JSONObject();

      if (productJson.has("images")) {
         JSONArray imagesArray = productJson.optJSONArray("images");
         JSONArray secondaryImages = new JSONArray();

         for (int i = 0; i < imagesArray.length(); i++) {
            JSONObject images = imagesArray.optJSONObject(i);
            String image = null;

            if (!images.optString("extraLarge").isEmpty()) {
               image = images.optString("extraLarge").trim();
            } else if (!images.optString("large").isEmpty()) {
               image = images.optString("large").trim();
            } else if (!images.optString("big").isEmpty()) {
               image = images.optString("big").trim();
            } else if (!images.optString("medium").isEmpty()) {
               image = images.optString("medium").trim();
            }

            if (image != null && !image.isEmpty()) {
               if (!jsonImages.has("primaryImage")) {
                  jsonImages.put("primaryImage", image);
               } else {
                  secondaryImages.put(image);
               }
            }
         }

         jsonImages.put("secondaryImages", secondaryImages);
      }

      return jsonImages;
   }

   public static JSONObject newWayToExtractJsonOffers(JSONObject initialJson, String internalPid, int arrayPosition) {
      JSONObject jsonPrices = new JSONObject();

      JSONArray offersJsonArray = new JSONArray();
      if (initialJson.has("offers") && initialJson.get("offers") instanceof JSONObject) {
         JSONObject offerJson = initialJson.optJSONObject("offers");

         if (offerJson.has("result")) {
            offersJsonArray = offerJson.optJSONArray("result");
         }

      } else if (initialJson.has("products") && initialJson.get("products") instanceof JSONObject) {
         JSONObject products = initialJson.optJSONObject("products");
         String jsonPath = internalPid + ".offers.result";

         offersJsonArray = JSONUtils.getValueRecursive(products, jsonPath, JSONArray.class);

         if (offersJsonArray == null) {
            jsonPath = internalPid + ".skus";

            JSONArray skusArray = JSONUtils.getValueRecursive(products, jsonPath, JSONArray.class);

            if (skusArray != null) {

               if (!skusArray.isEmpty() && skusArray.length() > 0) {

                  JSONObject jsonObject = skusArray.optJSONObject(arrayPosition);
                  jsonPath = "offers.result";
                  offersJsonArray = JSONUtils.getValueRecursive(jsonObject, jsonPath, JSONArray.class);

               }
            }

         }

      }

      if (offersJsonArray != null && offersJsonArray.length() > 0) {
         JSONArray moreQuantityOfInstallments = new JSONArray();

         for (int i = 0; i < offersJsonArray.length(); i++) {
            JSONObject jsonOffer = offersJsonArray.optJSONObject(i);
            JSONObject jsonSeller = new JSONObject();

            String idProduct = crawlIdProduct(jsonOffer);

            jsonSeller.put("priceFrom", jsonOffer.optDouble("salesPrice", 0d));

            if (idProduct != null) {
               manageEmbedded(jsonOffer, jsonSeller);

               if (jsonOffer.has("bestPaymentOption")) {
                  JSONObject payment = jsonOffer.optJSONObject("bestPaymentOption");

                  setBoleto(payment, jsonSeller);
                  setCard(payment, jsonSeller, moreQuantityOfInstallments);
                  setSpotlightPriceForSellers(payment, jsonSeller);

               } else if (jsonOffer.has("paymentOptions")) {
                  JSONObject payment = jsonOffer.optJSONObject("paymentOptions");

                  setBoleto(payment, jsonSeller);
                  setCard(payment, jsonSeller, moreQuantityOfInstallments);
               }

               if (jsonPrices.has(idProduct)) {
                  JSONArray installments = jsonPrices.optJSONArray(idProduct);
                  installments.put(jsonSeller);

                  jsonPrices.put(idProduct, installments);
               } else {
                  JSONArray installments = new JSONArray();
                  installments.put(jsonSeller);

                  jsonPrices.put(idProduct, installments);
               }
            }
         }

         jsonPrices.put("moreQuantityOfInstallments", moreQuantityOfInstallments);
      }
      return jsonPrices;
   }

   public static JSONObject extractJsonOffers(JSONObject initialJson, String internalPid) {
      JSONObject jsonPrices = new JSONObject();

      JSONArray offersJsonArray = new JSONArray();

      if (initialJson.has("offers") && initialJson.get("offers") instanceof JSONArray) {
         offersJsonArray = initialJson.optJSONArray("offers");
      } else if (initialJson.has("entities")) {
         JSONObject entities = initialJson.optJSONObject("entities");

         if (entities.has("offers")) {
            JSONObject offerObject = entities.optJSONObject("offers");

            if (offerObject.has(internalPid)) {
               offersJsonArray = offerObject.optJSONArray(internalPid);
            }
         }

         if ((offersJsonArray == null || offersJsonArray.length() < 1) && entities.has("pickUpStoreOffers")) {
            JSONObject offerObject = entities.optJSONObject("pickUpStoreOffers");
            JSONObject productOffer = offerObject.optJSONObject(internalPid);
            if (productOffer != null && productOffer.has("result")) {
               offersJsonArray = productOffer.optJSONArray("result");
            }
         }
      } else if (initialJson.has("offers") && initialJson.get("offers") instanceof JSONObject) {
         JSONObject offerJson = initialJson.optJSONObject("offers");

         if (offerJson.has("result")) {
            offersJsonArray = offerJson.optJSONArray("result");
         }

      }

      if (offersJsonArray != null && offersJsonArray.length() > 0) {
         JSONArray moreQuantityOfInstallments = new JSONArray();

         for (int i = 0; i < offersJsonArray.length(); i++) {
            JSONObject jsonOffer = offersJsonArray.optJSONObject(i);
            JSONObject jsonSeller = new JSONObject();

            String idProduct = crawlIdProduct(jsonOffer);

            jsonSeller.put("priceFrom", jsonOffer.optDouble("salesPrice", 0d));

            if (idProduct != null) {
               manageEmbedded(jsonOffer, jsonSeller);

               if (jsonOffer.has("bestPaymentOption")) {
                  JSONObject payment = jsonOffer.optJSONObject("bestPaymentOption");

                  setBoleto(payment, jsonSeller);
                  setCard(payment, jsonSeller, moreQuantityOfInstallments);
                  setSpotlightPriceForSellers(payment, jsonSeller);

               } else if (jsonOffer.has("paymentOptions")) {
                  JSONObject payment = jsonOffer.optJSONObject("paymentOptions");

                  setBoleto(payment, jsonSeller);
                  setCard(payment, jsonSeller, moreQuantityOfInstallments);
               }

               if (jsonPrices.has(idProduct)) {
                  JSONArray installments = jsonPrices.optJSONArray(idProduct);
                  installments.put(jsonSeller);

                  jsonPrices.put(idProduct, installments);
               } else {
                  JSONArray installments = new JSONArray();
                  installments.put(jsonSeller);

                  jsonPrices.put(idProduct, installments);
               }
            }
         }

         jsonPrices.put("moreQuantityOfInstallments", moreQuantityOfInstallments);
      }

      return jsonPrices;
   }

   private static String crawlIdProduct(JSONObject jsonOffer) {
      String idProduct = null;

      if (jsonOffer.has("_links")) {
         JSONObject links = jsonOffer.optJSONObject("_links");

         if (links.has("sku")) {
            JSONObject sku = links.optJSONObject("sku");

            if (sku.has("id")) {
               idProduct = sku.optString("id");
            }
         }
      } else if (jsonOffer.has("sku")) {
         idProduct = jsonOffer.optString("sku");
      }

      return idProduct;
   }

   private static void manageEmbedded(JSONObject jsonOffer, JSONObject jsonSeller) {

      if (jsonOffer.has("_embedded")) {
         JSONObject embedded = jsonOffer.optJSONObject("_embedded");

         if (embedded.has("seller")) {
            JSONObject seller = embedded.optJSONObject("seller");

            setStock(seller, jsonSeller);

            if (seller.has("name")) {
               jsonSeller.put("sellerName", seller.get("name").toString().toLowerCase().trim());
            }

            if (seller.has("id")) {
               jsonSeller.put("id", seller.get("id"));
            }
         }
      } else if (!jsonOffer.isEmpty()) {
         if (jsonOffer.has("seller")) {
            JSONObject seller = jsonOffer.optJSONObject("seller");

            if (seller.has("name")) {
               jsonSeller.put("sellerName", seller.get("name").toString().toLowerCase().trim());
            }

            if (seller.has("id")) {
               jsonSeller.put("id", seller.get("id"));
            }

         }
      }
   }

   private static void setBoleto(JSONObject payment, JSONObject jsonSeller) {
      if (payment.has("boleto")) {
         JSONObject boleto = payment.optJSONObject("boleto");

         if (boleto.has("price")) {
            jsonSeller.put("bankSlip", boleto.getDouble("price"));

            JSONObject discountJson = boleto.optJSONObject("discount");
            jsonSeller.put("bankSlipDiscount", discountJson != null ? discountJson.optDouble("rate", 0d) / 100d : 0d);
         }
      }
   }

   private static void setSpotlightPriceForSellers(JSONObject payment, JSONObject jsonSeller) {
      if (payment.has("minQuantity")) {
         JSONArray minQuantity = payment.optJSONArray("minQuantity");
         for (Object o : minQuantity) {
            if (o instanceof JSONObject) {
               JSONObject paymentInfo = (JSONObject) o;
               Double setSpotlighPrice = paymentInfo.optDouble("total");

               jsonSeller.put("spotlightPrice", setSpotlighPrice);
            }
         }
      }
   }

   private static void setCard(JSONObject payment, JSONObject jsonSeller, JSONArray moreQuantityOfInstallments) {
      if (payment.has("CARTAO_VISA")) {
         JSONObject visa = payment.optJSONObject("CARTAO_VISA");

         if (visa.has("installments")) {
            JSONArray installments = visa.optJSONArray("installments");
            jsonSeller.put("installments", installments);

            if (installments.length() > moreQuantityOfInstallments.length()) {
               moreQuantityOfInstallments = installments;
            }
         }

         if (visa.has("price")) {
            jsonSeller.put("defaultPrice", visa.get("price"));
         }
      }

      if (payment.has("CARTAO_SUBA_MASTERCARD")) {
         JSONObject shopCard = payment.optJSONObject("CARTAO_SUBA_MASTERCARD");

         if (shopCard.has("installments")) {
            JSONArray installments = shopCard.optJSONArray("installments");
            jsonSeller.put("installmentsShopCard", installments);

            if (installments.length() > moreQuantityOfInstallments.length()) {
               moreQuantityOfInstallments = installments;
            }
         }
      }

      if (payment.has("CARTAO_ACOM_MASTERCARD")) {
         JSONObject shopCard = payment.optJSONObject("CARTAO_ACOM_MASTERCARD");

         if (shopCard.has("installments")) {
            JSONArray installments = shopCard.optJSONArray("installments");
            jsonSeller.put("installmentsShopCard", installments);

            if (installments.length() > moreQuantityOfInstallments.length()) {
               moreQuantityOfInstallments = installments;
            }
         }
      }

      if (payment.has("CARTAO_SHOP_MASTERCARD")) {
         JSONObject shopCard = payment.optJSONObject("CARTAO_SHOP_MASTERCARD");

         if (shopCard.has("installments")) {
            JSONArray installments = shopCard.optJSONArray("installments");
            jsonSeller.put("installmentsShopCard", installments);

            if (installments.length() > moreQuantityOfInstallments.length()) {
               moreQuantityOfInstallments = installments;
            }
         }
      }
   }

   private static void setStock(JSONObject seller, JSONObject jsonSeller) {
      if (seller.has("availability")) {
         JSONObject availability = seller.optJSONObject("availability");

         if (availability.has("_embedded")) {
            JSONObject embeddedStock = availability.optJSONObject("_embedded");

            if (embeddedStock.has("stock")) {
               JSONObject jsonStock = embeddedStock.optJSONObject("stock");

               if (jsonStock.has("quantity")) {
                  jsonSeller.put("stock", jsonStock.getInt("quantity"));
               }
            }
         }
      }
   }

   public static String crawlInternalPidShoptime(Document doc) {
      String internalID = null;

      Element elementInternalID = doc.select(".p-name#main-product-name .p-code").first();
      if (elementInternalID != null) {
         internalID = elementInternalID.text().split(" ")[1].replace(")", " ").trim();
      }

      return internalID;
   }

   public static JSONObject extractPricesJson(JSONObject offerJson) {
      JSONObject pricesJson = new JSONObject();

      JSONArray moreQuantityOfInstallments = new JSONArray();

      pricesJson.put("priceFrom", pricesJson.optDouble("salesPrice", 0d));
      extractBoleto(offerJson, pricesJson, "boleto");
      extractCards(offerJson, pricesJson, moreQuantityOfInstallments);

      pricesJson.put("moreQuantityOfInstallments", moreQuantityOfInstallments);

      return pricesJson;
   }

   private static void extractBoleto(JSONObject offerJson, JSONObject pricesJson, String key) {
      if (offerJson.has(key)) {
         JSONObject boleto = offerJson.optJSONObject(key);

         if (boleto.has("price")) {
            pricesJson.put("bankSlip", boleto.getDouble("price"));

            JSONObject discountJson = boleto.optJSONObject("discount");
            pricesJson.put("bankSlipDiscount", discountJson != null ? discountJson.optDouble("rate", 0d) / 100d : 0d);
         }
      }
   }

   private static void extractCards(JSONObject offerJson, JSONObject pricesJson, JSONArray moreQuantityOfInstallments) {
      List<String> cards = Arrays.asList("cartaoVisa");

      for (String card : cards) {
         if (offerJson.has(card)) {
            JSONObject visa = offerJson.optJSONObject(card);

            JSONArray minInstallments = visa.optJSONArray("minQuantity");
            JSONArray maxInstallments = visa.optJSONArray("maxQuantity");

            JSONArray installments = minInstallments;
            for (Object obj : maxInstallments) {
               installments.put((JSONObject) obj);
            }

            pricesJson.put("installments", installments);

            if (installments.length() > moreQuantityOfInstallments.length()) {
               moreQuantityOfInstallments = installments;
            }

            if (visa.has("price")) {
               pricesJson.put("defaultPrice", visa.get("price"));
            }
         }
      }
   }


   public static JSONObject getJson(JSONObject jsonSeller, String type) {
      if (jsonSeller != null) {
         for (Iterator<String> it = jsonSeller.keys(); it.hasNext(); ) {
            String key = it.next();
            if (key.contains(type)) {
               return jsonSeller.optJSONObject(key);
            }
         }
      }
      return new JSONObject();
   }

   private static void extractOffer(JSONObject apoloJson, String key, JSONObject offersJson) {
      JSONObject offerResult = apoloJson.optJSONObject(key);

      if (offerResult != null) {
         String sku = offerResult.optString("sku");

         if (sku != null) {
            JSONObject sellerJson = new JSONObject();

            String sellerKey = offerResult.optQuery("/seller/__ref").toString().trim();
            sellerJson.put("seller", apoloJson.optJSONObject(sellerKey));

            extractBoleto(offerResult, sellerJson, "paymentOptions({\"type\":\"BOLETO\"})");
            extractApolloCards(offerResult, sellerJson);

            if (!sellerJson.has("defaultPrice")) {
               Double defaultPrice = offerResult.optDouble("salesPrice", 0d);

               if (defaultPrice > 0d) {
                  sellerJson.put("defaultPrice", defaultPrice);
               }
            }

            if (sellerJson.has("defaultPrice")) {
               if (offersJson.has(sku)) {
                  offersJson.getJSONArray(sku).put(sellerJson);
               } else {
                  JSONArray skuOffers = new JSONArray();
                  skuOffers.put(sellerJson);

                  offersJson.put(sku, skuOffers);
               }
            }
         }
      }
   }

   private static void extractApolloCards(JSONObject offerJson, JSONObject pricesJson) {
      List<String> cards = Arrays.asList("paymentOptions({\"type\":\"CARTAO_VISA\"})", "paymentOptions({\"type\":\"CARTAO_ACOM_MASTERCARD\"})");

      for (String card : cards) {
         if (offerJson.has(card)) {
            JSONObject visa = offerJson.optJSONObject(card);

            JSONArray minInstallments = visa.optJSONArray("installment({\"filter\":\"min\"})");
            JSONArray maxInstallments = visa.optJSONArray("installment({\"filter\":\"max\"})");

            JSONArray installments = minInstallments;
            for (Object obj : maxInstallments) {
               installments.put((JSONObject) obj);
            }

            pricesJson.put("installments", installments);

            if (visa.has("price")) {
               pricesJson.put("defaultPrice", visa.get("price"));
            }
         }
      }
   }

   public static JSONArray getJsonArrayInstallment(JSONObject jsonObject) {
      for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
         String key = it.next();
         if (key.contains("installment") && key.contains("min")) {
            return jsonObject.optJSONArray(key);
         }

      }
      return new JSONArray();

   }

   public static JSONObject getJsonArray(JSONObject jsonObject) {
      for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
         String key = it.next();
         if (key.contains("installment")) {
            return jsonObject.optJSONArray(key).getJSONObject(0);
         }

      }
      return new JSONObject();

   }


}
