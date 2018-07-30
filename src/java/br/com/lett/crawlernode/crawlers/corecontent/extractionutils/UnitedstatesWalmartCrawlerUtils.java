package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class UnitedstatesWalmartCrawlerUtils {

  public static final String SELLER_NAME_LOWER = "walmart.com";

  /**
   * This field in santizedJson is of type {@link String}
   */
  public static final String INTERNAL_PID = "internalPid";

  /**
   * This field in santizedJson is of type {@link String}
   */
  public static final String INTERNAL_ID = "internalId";

  /**
   * This field in santizedJson is of type {@link JSONArray} of {@link JSONObject}
   */
  public static final String OFFERS = "offers";

  /**
   * This field in santizedJson is of type {@link Double} or {@link Integer}
   */
  public static final String OFFERS_PRICE = "price";

  /**
   * This field in santizedJson is of type {@link Boolean}
   */
  public static final String OFFERS_AVAILABLE = "available";

  /**
   * This field in santizedJson is of type {@link Double} or {@link Integer}
   */
  public static final String OFFERS_OLD_PRICE = "oldPrice";

  /**
   * This field in santizedJson is of type {@link String}
   */
  public static final String OFFERS_SELLER_NAME = "sellerName";

  /**
   * This field in santizedJson is of type {@link JSONObject}
   */
  public static final String IMAGES = "images";

  /**
   * This field in santizedJson is of type {@link String}
   */
  public static final String IMAGES_PRIMARY = "primary";

  /**
   * This field in santizedJson is of type {@link JSONArray} of {@link String}
   */
  public static final String IMAGES_SECONDARY = "secondary";

  /**
   * This field in santizedJson is of type {@link String}
   */
  public static final String NAME = "variationName";

  /**
   * This field in santizedJson is of type {@link String}
   */
  public static final String DESCRIPTION = "description";


  /**
   * This field in santizedJson is of type {@link JSONObject}
   */
  public static final String RATING = "rating";

  /**
   * This field in santizedJson is of type {@link Double} or {@link Integer}
   */
  public static final String RATING_AVERAGE = "average";

  /**
   * This field in santizedJson is of type {@link Integer}
   */
  public static final String RATING_COUNT = "count";

  /**
   * Essa função pega o json INITIAL_STATE do html e o deixa mais simples para capturar as informações
   * 
   * @param doc
   * @return
   */
  public static JSONArray sanitizeINITIALSTATEJson(Document doc) {
    JSONArray products = new JSONArray();
    JSONObject initialState = CrawlerUtils.selectJsonFromHtml(doc, "script ", "window.__WML_REDUX_INITIAL_STATE__ =", ";};", false);

    if (initialState.has("product")) {
      JSONObject product = initialState.getJSONObject("product");

      CommonMethods.saveDataToAFile(product, "/home/gabriel/htmls/WALMART.txt");

      JSONObject images = crawlJsonImages(product);
      JSONObject offers = crawlJsonOffers(product);
      JSONObject rating = crawlRating(product);

      products = crawlProductsInfo(product, images, offers, rating);
    }

    return products;
  }

  private static JSONArray crawlProductsInfo(JSONObject product, JSONObject images, JSONObject offers, JSONObject rating) {
    JSONArray products = new JSONArray();

    if (product.has("products")) {
      JSONObject productsJson = product.getJSONObject("products");

      for (String internalId : productsJson.keySet()) {
        JSONObject productJson = productsJson.getJSONObject(internalId);

        JSONObject santizedProductInfo = new JSONObject();
        santizedProductInfo.put(RATING, rating);
        santizedProductInfo.put(INTERNAL_ID, internalId);

        // nesse json existem muitos id's, esse foi escolhido por estar presente na url
        if (productJson.has("usItemId")) {
          santizedProductInfo.put(INTERNAL_PID, productJson.get("usItemId"));
        }

        santizedProductInfo.put(DESCRIPTION, crawlDescription(product));
        santizedProductInfo.put(NAME, crawlName(productJson));
        santizedProductInfo.put(OFFERS, crawlOffers(productJson, offers));
        santizedProductInfo.put(IMAGES, crawlImages(productJson, images));
        products.put(santizedProductInfo);
      }
    }

    return products;
  }

  private static String crawlDescription(JSONObject product) {
    StringBuilder desc = new StringBuilder();

    if (product.has("idmlMap")) {
      JSONObject idmlMap = product.getJSONObject("idmlMap");
      JSONObject descriptions = idmlMap.getJSONObject(idmlMap.keys().next());

      if (descriptions.has("modules")) {
        JSONObject modules = descriptions.getJSONObject("modules");

        for (String key : modules.keySet()) {
          if (!key.equalsIgnoreCase("GeneralInfo")) {
            desc.append(crawlSpecificDescription(modules, key));
          }
        }
      }
    }

    return desc.toString();
  }

  private static String crawlSpecificDescription(JSONObject modules, String key) {
    StringBuilder desc = new StringBuilder();

    if (modules.has(key)) {
      JSONObject module = modules.getJSONObject(key);
      desc.append("<div id=\"" + key + "\">");

      for (String descKey : module.keySet()) {
        Object descObject = module.get(descKey);
        JSONObject descJSONObject = descObject instanceof JSONObject ? (JSONObject) descObject : new JSONObject();

        if (descJSONObject.has("displayName")) {
          desc.append("<div id=\"" + descKey + "\">");
          desc.append("<strong> " + descJSONObject.get("displayName") + " &nbsp</strong>");

          if (descJSONObject.has("displayValue")) {
            desc.append("<span> " + descJSONObject.get("displayValue") + " &nbsp</span>");
          } else if (descJSONObject.has("values")) {
            desc.append("<table id=\"" + descKey + "\">");
            desc.append(crawlDescriptionValues(descJSONObject));
            desc.append("</table>");
          } else if (descJSONObject.has("children")) {
            JSONArray children = descJSONObject.getJSONArray("children");
            desc.append("<table id=\"" + descKey + "\">");

            for (Object child : children) {
              JSONObject childObj = (JSONObject) child;

              if (descJSONObject.has("displayName")) {
                desc.append("<div id=\"" + descKey + "\">");
                desc.append("<strong> " + descJSONObject.get("displayName") + " &nbsp</strong>");

                if (childObj.has("values")) {
                  desc.append("<table id=\"" + descJSONObject.get("displayName") + "\">");
                  desc.append(crawlDescriptionValues(childObj));
                  desc.append("<\table>");
                }
              }
            }

            desc.append("<\table>");
          }

          desc.append("</div>");
        }
      }

      desc.append("</div>");
    }

    return desc.toString();
  }


  private static String crawlDescriptionValues(JSONObject descObject) {
    StringBuilder desc = new StringBuilder();

    JSONArray values = descObject.getJSONArray("values");

    for (Object value : values) {
      desc.append("<tr>");

      if (value instanceof String) {
        desc.append(value);
      } else {
        JSONObject valueObj = new JSONObject();

        if (value instanceof JSONObject) {
          valueObj = (JSONObject) value;
        } else if (value instanceof JSONArray) {
          valueObj = ((JSONArray) value).length() > 0 ? ((JSONArray) value).getJSONObject(0) : new JSONObject();
        }

        if (valueObj.length() > 0) {
          JSONObject valueContent = valueObj.getJSONObject(valueObj.keys().next());

          if (valueContent.has("displayName") && valueContent.has("displayValue")) {
            desc.append("<td> " + valueContent.get("displayName") + "</td>");
            desc.append("<td> " + valueContent.get("displayValue") + "</td>");
          }
        }
      }
      desc.append("<\tr>");
    }

    return desc.toString();
  }

  private static JSONObject crawlRating(JSONObject product) {
    JSONObject rating = new JSONObject();

    if (product.has("primaryProduct")) {
      String primaryProduct = product.getString("primaryProduct");

      if (product.has("reviews")) {
        JSONObject reviews = product.getJSONObject("reviews");

        if (reviews.has(primaryProduct)) {
          JSONObject productRating = reviews.getJSONObject(primaryProduct);

          if (productRating.has("roundedAverageOverallRating")) {
            rating.put(RATING_AVERAGE, productRating.get("roundedAverageOverallRating"));
          }

          if (productRating.has("totalReviewCount")) {
            rating.put(RATING_COUNT, productRating.get("totalReviewCount"));
          }
        }
      }
    }

    return rating;
  }

  private static JSONObject crawlImages(JSONObject productJson, JSONObject images) {
    JSONObject productImages = new JSONObject();

    if (productJson.has("images")) {
      JSONArray secondaryImages = new JSONArray();
      JSONArray imagesIds = productJson.getJSONArray("images");

      for (Object imageId : imagesIds) {
        if (images.has(imageId.toString())) {
          JSONObject imageJson = images.getJSONObject(imageId.toString());

          if (imageJson.has("type") && imageJson.has("assetSizeUrls")) {
            String image = null;
            JSONObject assetSizeUrls = imageJson.getJSONObject("assetSizeUrls");

            if (assetSizeUrls.has("zoom")) {
              image = assetSizeUrls.get("zoom").toString();
            } else if (assetSizeUrls.has("main")) {
              image = assetSizeUrls.get("main").toString();
            } else if (assetSizeUrls.has("inspiration")) {
              image = assetSizeUrls.get("inspiration").toString();
            } else if (assetSizeUrls.has("tile")) {
              image = assetSizeUrls.get("tile").toString();
            } else if (assetSizeUrls.has("thumb")) {
              image = assetSizeUrls.get("thumb").toString();
            }

            String type = imageJson.get("type").toString();
            if (type.equalsIgnoreCase("PRIMARY") && !productImages.has(IMAGES_PRIMARY)) {
              productImages.put(IMAGES_PRIMARY, image);
            } else {
              secondaryImages.put(image);
            }
          }
        }
      }

      productImages.put(IMAGES_SECONDARY, secondaryImages);
    }

    return productImages;
  }

  private static JSONArray crawlOffers(JSONObject productJson, JSONObject offers) {
    JSONArray productOffers = new JSONArray();

    if (productJson.has("offers")) {
      JSONArray offersIds = productJson.getJSONArray("offers");

      for (Object offerId : offersIds) {
        if (offers.has(offerId.toString())) {
          productOffers.put(offers.getJSONObject(offerId.toString()));
        }
      }
    }

    return productOffers;
  }

  private static String crawlName(JSONObject productJson) {
    StringBuilder name = new StringBuilder();

    if (productJson.has("variants")) {
      JSONObject variants = productJson.getJSONObject("variants");

      for (String variantKey : variants.keySet()) {
        String variant = variants.getString(variantKey);

        if (variant.contains("-")) {
          name.append(CommonMethods.upperCaseFirstCharacter(CommonMethods.getLast(variant.split("-"))));
        } else {
          name.append(CommonMethods.upperCaseFirstCharacter(variant));
        }
      }
    }

    return name.toString();
  }

  private static JSONObject crawlJsonOffers(JSONObject product) {
    JSONObject sanitizedOffers = new JSONObject();
    Map<String, String> sellersIdsMap = crawlSellersIds(product);

    if (product.has("offers")) {
      JSONObject offers = product.getJSONObject("offers");
      for (String offerId : offers.keySet()) {
        JSONObject offer = offers.getJSONObject(offerId);
        JSONObject sanitizedOffer = new JSONObject();
        sanitizedOffer.put("available", false);

        if (offer.has("pricesInfo")) {
          JSONObject pricesInfo = offer.getJSONObject("pricesInfo");

          if (pricesInfo.has("priceMap")) {
            JSONObject priceMap = pricesInfo.getJSONObject("priceMap");

            sanitizedOffer.put(OFFERS_PRICE, crawlPriceFromPriceMapJSON(priceMap, "CURRENT"));
            sanitizedOffer.put(OFFERS_OLD_PRICE, crawlPriceFromPriceMapJSON(priceMap, "WAS"));
          }
        }

        if (offer.has("productAvailability")) {
          JSONObject productAvailability = offer.getJSONObject("productAvailability");

          sanitizedOffer.put("available",
              productAvailability.has("availabilityStatus") && productAvailability.get("availabilityStatus").toString().equalsIgnoreCase("IN_STOCK"));
        }

        if (offer.has("sellerId")) {
          String sellerId = offer.get("sellerId").toString();

          if (sellersIdsMap.containsKey(sellerId)) {
            sanitizedOffer.put(OFFERS_SELLER_NAME, sellersIdsMap.get(sellerId));
          }
        } else {
          sanitizedOffer.put(OFFERS_SELLER_NAME, SELLER_NAME_LOWER);
        }

        sanitizedOffers.put(offerId, sanitizedOffer);
      }
    }

    return sanitizedOffers;
  }

  private static Double crawlPriceFromPriceMapJSON(JSONObject priceMap, String type) {
    Double price = null;

    if (priceMap.has(type)) {
      JSONObject priceJson = priceMap.getJSONObject(type);

      if (priceJson.has("price")) {
        Object priceObj = priceJson.get("price");

        if (priceObj instanceof Integer) {
          price = ((Integer) priceObj).doubleValue();
        } else if (priceObj instanceof Double) {
          price = (Double) priceObj;
        }
      }
    }

    return price;
  }

  private static Map<String, String> crawlSellersIds(JSONObject product) {
    Map<String, String> sellersIdsMap = new HashMap<>();

    if (product.has("sellers")) {
      JSONObject sellers = product.getJSONObject("sellers");

      for (String sellerId : sellers.keySet()) {
        JSONObject seller = sellers.getJSONObject(sellerId);

        if (seller.has(OFFERS_SELLER_NAME)) {
          sellersIdsMap.put(sellerId, seller.get(OFFERS_SELLER_NAME).toString().toLowerCase());
        }
      }
    }

    return sellersIdsMap;
  }

  private static JSONObject crawlJsonImages(JSONObject product) {
    JSONObject images = new JSONObject();

    if (product.has("images")) {
      images = product.getJSONObject("images");
    }

    return images;
  }
}
