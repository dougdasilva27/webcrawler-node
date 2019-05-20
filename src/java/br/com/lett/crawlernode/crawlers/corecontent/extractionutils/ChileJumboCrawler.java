package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class ChileJumboCrawler extends Crawler {

  public static final String JUMBO_DEHESA_ID = "13";
  public static final String JUMBO_LAFLORIDA_ID = "18";
  public static final String JUMBO_LAREINA_ID = "11";
  public static final String JUMBO_LOSDOMINICOS_ID = "12";
  public static final String JUMBO_LASCONDES_ID = "12";
  public static final String JUMBO_VINA_ID = "16";

  private static final String MAIN_SELLER_NAME_LOWER = "jumbo";
  public static final String HOME_PAGE = "https://www2.jumbo.cl/";
  public static final String HOST = "www2.jumbo.cl";

  private JSONObject promotionsJson = new JSONObject();

  public ChileJumboCrawler(Session session) {
    super(session);
  }

  @Override
  protected Object fetch() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");

    Request request = RequestBuilder.create().setCookies(cookies).setHeaders(headers).setUrl(session.getOriginalURL()).build();
    return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    List<Product> products = new ArrayList<>();

    JSONObject skuJson = scrapProductJson(doc);

    if (skuJson.has("productId")) {

      String internalPid = skuJson.get("productId").toString();
      CategoryCollection categories = scrapCategories(skuJson);

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("items") ? skuJson.getJSONArray("items") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = jsonSku.has("itemId") ? jsonSku.get("itemId").toString() : null;
        String name = jsonSku.has("nameComplete") ? jsonSku.get("nameComplete").toString() : null;
        String description = scrapDescription(jsonSku, skuJson);

        Float pricePromotion = getPromotionShopCardPrice(skuJson, internalId);
        Map<String, Prices> marketplaceMap = scrapMarketplace(jsonSku);
        if (pricePromotion != null) {
          setPricePromotionInMarketplaceMap(pricePromotion, marketplaceMap);
        }

        List<String> mainSellers = CrawlerUtils.getMainSellers(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER));
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, mainSellers, Card.AMEX, session);
        boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, mainSellers);

        String primaryImage = scrapPrimaryImage(jsonSku);
        String secondaryImages = scrapSecondaryImages(jsonSku, primaryImage);
        Prices prices = CrawlerUtils.getPrices(marketplaceMap, mainSellers);
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.AMEX);

        List<String> eans = jsonSku.has("ean") ? Arrays.asList(jsonSku.get("ean").toString()) : new ArrayList<>();

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price).setPrices(prices)
            .setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages).setDescription(description).setMarketplace(marketplace).setEans(eans).build();

        products.add(product);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
    }

    return products;
  }

  private JSONObject scrapProductJson(Document doc) {
    JSONObject jsonSku = new JSONObject();

    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "__renderData =", ";", false, true);

    if (json.has("pdp")) {
      JSONObject pdp = json.getJSONObject("pdp");

      if (pdp.has("product") && !pdp.isNull("product")) {
        JSONArray product = pdp.getJSONArray("product");

        if (product.length() > 0) {
          jsonSku = product.getJSONObject(0);
        }
      }
    }

    return jsonSku;
  }

  private CategoryCollection scrapCategories(JSONObject skuJson) {
    CategoryCollection categories = new CategoryCollection();

    if (skuJson.has("categories") && !skuJson.isNull("categories")) {
      JSONArray categoriesArray = skuJson.getJSONArray("categories");

      for (Object c : categoriesArray) {
        if (c.toString().contains("/")) {
          String[] split = c.toString().split("/");

          if (split.length > 1) {
            categories.add(split[split.length - 1]);
          }

        } else {
          categories.add(c.toString());
        }
      }
    }

    return categories;
  }

  private String scrapDescription(JSONObject jsonSku, JSONObject skuJson) {
    StringBuilder description = new StringBuilder();

    if (jsonSku.has("complementName") && !jsonSku.isNull("complementName")) {
      String complementName = jsonSku.get("complementName").toString();

      if (!complementName.isEmpty()) {
        description.append("<div> <h3> Características principales </h3>");
        description.append(complementName);
        description.append("</div>");
      }
    }

    if (skuJson.has("description") && !skuJson.isNull("description")) {
      String descriptionText = skuJson.get("description").toString();

      if (!descriptionText.isEmpty()) {
        description.append("<div> <h3> Descripción </h3>");
        description.append(descriptionText);
        description.append("</div>");
      }
    }

    return description.toString();
  }

  private String scrapPrimaryImage(JSONObject jsonSku) {
    String primaryImage = null;

    if (jsonSku.has("images") && !jsonSku.isNull("images")) {
      JSONArray images = jsonSku.getJSONArray("images");

      for (Object o : images) {
        JSONObject image = (JSONObject) o;

        if (image.has("imageUrl")) {
          primaryImage = CrawlerUtils.completeUrl(image.get("imageUrl").toString(), "https", "jumbo.vteximg.com.br");
          break;
        }
      }
    }

    return primaryImage;
  }

  private String scrapSecondaryImages(JSONObject jsonSku, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (jsonSku.has("images") && !jsonSku.isNull("images")) {
      JSONArray images = jsonSku.getJSONArray("images");

      for (Object o : images) {
        JSONObject image = (JSONObject) o;

        if (image.has("imageUrl")) {
          String imageUrl = CrawlerUtils.completeUrl(image.get("imageUrl").toString(), "https", "jumbo.vteximg.com.br");

          if (!imageUrl.equalsIgnoreCase(primaryImage)) {
            secondaryImagesArray.put(imageUrl);
          }
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private Map<String, Prices> scrapMarketplace(JSONObject jsonSku) {
    Map<String, Prices> map = new HashMap<>();

    if (jsonSku.has("sellers") && !jsonSku.isNull("sellers")) {
      JSONArray sellers = jsonSku.getJSONArray("sellers");

      for (Object o : sellers) {
        JSONObject seller = (JSONObject) o;

        if (seller.has("sellerName") && seller.has("commertialOffer")) {
          Prices prices = scrapPrices(seller.getJSONObject("commertialOffer"));

          if (!prices.isEmpty()) {
            map.put(seller.get("sellerName").toString(), prices);
          }
        }
      }
    }

    return map;
  }

  private Prices scrapPrices(JSONObject comertial) {
    Prices prices = new Prices();

    if (comertial.has("Price") && !comertial.isNull("Price")) {
      Float price = CrawlerUtils.getFloatValueFromJSON(comertial, "Price", true, false);

      if (price > 0) {
        Map<Integer, Float> installments = new HashMap<>();
        installments.put(1, price);
        prices.setBankTicketPrice(price);
        prices.setPriceFrom(CrawlerUtils.getDoubleValueFromJSON(comertial, "ListPrice", true, false));

        if (comertial.has("Installments") && !comertial.isNull("Installments")) {
          JSONArray parcels = comertial.getJSONArray("Installments");

          for (Object o : parcels) {
            JSONObject parcel = (JSONObject) o;

            Integer number = CrawlerUtils.getIntegerValueFromJSON(parcel, "NumberOfInstallments", null);
            Float priceParcel = CrawlerUtils.getFloatValueFromJSON(parcel, "Value", true, false);

            if (number != null && priceParcel != null) {
              installments.put(number, priceParcel);
            }
          }
        }

        prices.insertCardInstallment(Card.AMEX.toString(), installments);
        prices.insertCardInstallment(Card.SHOP_CARD.toString(), installments);
      }
    }

    return prices;
  }

  private void setPricePromotionInMarketplaceMap(Float price, Map<String, Prices> marketplaceMap) {
    Prices pricesToBeSet = new Prices();
    String selletToBeSet = null;

    for (Entry<String, Prices> entry : marketplaceMap.entrySet()) {
      Prices prices = entry.getValue();
      Map<Integer, Float> shopCardMap = new HashMap<>();
      shopCardMap.put(1, price);
      prices.insertCardInstallment(Card.SHOP_CARD.toString(), shopCardMap);

      pricesToBeSet = prices;
      selletToBeSet = entry.getKey();
      break;
    }

    if (selletToBeSet != null) {
      marketplaceMap.put(selletToBeSet, pricesToBeSet);
    }
  }

  private JSONObject crawlPromotionsAPI() {
    Request request = RequestBuilder.create().setUrl("https://" + HOST + "/jumbo/dataentities/PM/documents/Promos?_fields=value%2Cid").setCookies(cookies).build();
    return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
  }

  private Float getPromotionShopCardPrice(JSONObject skuJson, String id) {
    Float pricePromotion = null;

    if (skuJson.has("JumboAhora")) {
      String ahora = skuJson.get("JumboAhora").toString().toLowerCase();

      if (ahora.contains("si") && skuJson.has("SkuData")) {
        JSONObject promotionsData = new JSONObject();
        JSONArray skuData = skuJson.getJSONArray("SkuData");

        for (Object o : skuData) {
          JSONObject sku = CrawlerUtils.stringToJson(o.toString().replace("\\\\", ""));

          if (sku.has(id)) {
            promotionsData = sku.getJSONObject(id);
            break;
          }
        }

        if (this.promotionsJson.length() < 1) {
          this.promotionsJson = crawlPromotionsAPI();
        }

        JSONObject promotionsList = this.promotionsJson.has("value") ? this.promotionsJson.getJSONObject("value") : new JSONObject();

        if (promotionsData.has("promotions")) {
          for (Object o : promotionsData.getJSONArray("promotions")) {
            if (promotionsList.has(o.toString())) {
              JSONObject promotionJson = promotionsList.getJSONObject(o.toString());

              if (promotionJson.has("group") && promotionJson.has("discountType") && promotionJson.has("value")) {
                String group = promotionJson.get("group").toString();
                String discountType = promotionJson.get("discountType").toString();

                if (group.equalsIgnoreCase("t-cenco") && !discountType.equalsIgnoreCase("percentual")) {
                  pricePromotion = CrawlerUtils.getFloatValueFromJSON(promotionJson, "value");
                  break;
                }
              }
            }
          }
        }
      }
    }

    return pricePromotion;
  }
}
