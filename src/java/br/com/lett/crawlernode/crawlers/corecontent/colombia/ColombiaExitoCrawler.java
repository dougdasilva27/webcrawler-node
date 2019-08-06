package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
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



public class ColombiaExitoCrawler extends Crawler {
  private static final String HOME_PAGE = "https://www.exito.com/";
  private static final String MAIN_SELLER_NAME_LOWER = "EXITO";

  public ColombiaExitoCrawler(Session session) {
    super(session);
  }

  public List<Product> extractInformation(Document doc) throws Exception {
    List<Product> products = new ArrayList<>();

    String redirectUrl = CrawlerUtils.getRedirectedUrl(session.getOriginalURL(), session);
    JSONObject stateJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "__STATE__ =", null, false, true);
    JSONObject productJson = scrapProductJson(stateJson, redirectUrl);

    if (productJson.has("productId")) {
      String internalPid = productJson.has("productId") && !productJson.isNull("productId") ? productJson.get("productId").toString() : null;
      CategoryCollection categories = scrapCategories(productJson, stateJson);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".exito-product-details-2-x-informationsContainer"));

      JSONArray items = productJson.has("items") && !productJson.isNull("items") ? productJson.getJSONArray("items") : new JSONArray();

      for (int i = 0; i < items.length(); i++) {
        JSONObject jsonSku = getInformationJson(items.getJSONObject(i), stateJson);

        String internalId = jsonSku.has("itemId") ? jsonSku.get("itemId").toString() : null;
        String name = jsonSku.has("nameComplete") ? jsonSku.get("nameComplete").toString() : null;
        Map<String, Prices> marketplaceMap = scrapMarketplace(jsonSku, stateJson);
        List<String> mainSellers = CrawlerUtils.getMainSellers(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER));
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, mainSellers, Card.AMEX, session);
        boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, mainSellers);

        List<String> images = scrapImages(jsonSku, stateJson);
        String primaryImage = !images.isEmpty() ? images.get(0) : null;
        String secondaryImages = scrapSecondaryImages(images);

        Prices prices = CrawlerUtils.getPrices(marketplaceMap, mainSellers);
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.AMEX);

        List<String> eans = jsonSku.has("ean") ? Arrays.asList(jsonSku.get("ean").toString()) : null;

        // Creating the product
        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrice(price)
            .setPrices(prices)
            .setAvailable(available)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setMarketplace(marketplace)
            .setEans(eans)
            .build();

        products.add(product);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
    }

    return products;
  }

  /**
   * stateJson is like this:
   * 
   * "Product.undefined@undefined.x::celular-redmi-note-7-dual-camara-48mp5mp-128gb-4gb-negro-100163854-mp":{
   * "cacheId":"celular-redmi-note-7-dual-camara-48mp5mp-128gb-4gb-negro-100163854-mp",
   * "productName":"Celular Redmi Note 7 Dual Camara 48MP+5MP 128GB 4GB Negro",
   * "productId":"100163854", ...
   * 
   * Url:
   * https://www.exito.com/celular-redmi-note-7-dual-camara-48mp5mp-128gb-4gb-negro-100163854-mp/p
   * FirstKey:
   * 
   * Can be Product:celular-redmi-note-7-dual-camara-48mp5mp-128gb-4gb-negro-100163854-mp
   * 
   * Or
   * Product.undefined@undefined.x::celular-redmi-note-7-dual-camara-48mp5mp-128gb-4gb-negro-100163854-mp
   * 
   * We need to build this key with url path
   * 
   * @param doc
   * @param url
   * @return
   */
  private JSONObject scrapProductJson(JSONObject stateJson, String url) {
    JSONObject jsonSku = new JSONObject();

    if (url.contains("/p")) {
      String urlPath = url.replace(HOME_PAGE, "").split("/p")[0];

      String key = "Product:" + urlPath;
      String specialKey = "Product.undefined@undefined.x::" + urlPath;

      if (stateJson.has(key) && !stateJson.isNull(key)) {
        jsonSku = stateJson.getJSONObject(key);
      } else if (stateJson.has(specialKey) && !stateJson.isNull(specialKey)) {
        jsonSku = stateJson.getJSONObject(specialKey);
      }
    }

    return jsonSku;
  }

  /**
   * In this market we have this case:
   * 
   * {
   * 
   * ---"Product.undefined@undefined.x::celular": {
   * 
   * ------"name": "blabla",
   * 
   * ------"description": "nada",
   * 
   * ------"items": {
   * 
   * --------"id":"Product.undefined@undefined.x::celular.items.0"
   * 
   * ------}
   * 
   * ---},
   * 
   * ---"Product.undefined@undefined.x::celular.items.0":{
   * 
   * ------"itemId": 25,
   * 
   * ------"avaiable": true
   * 
   * ---}
   * 
   * }
   * 
   * For this, we need that function for scrap items for example, but are more cases like this
   * 
   * @param json
   * @param stateJson
   * @return
   */
  private JSONObject getInformationJson(JSONObject json, JSONObject stateJson) {
    JSONObject specificJson = new JSONObject();

    if (json.has("id") && !json.isNull("id")) {
      String key = json.get("id").toString();

      if (stateJson.has(key) && !stateJson.isNull(key)) {
        specificJson = stateJson.getJSONObject(key);
      }
    }

    return specificJson;
  }

  private CategoryCollection scrapCategories(JSONObject productJson, JSONObject stateJson) {
    CategoryCollection categories = new CategoryCollection();

    if (productJson.has("categoryTree") && !productJson.isNull("categoryTree")) {
      JSONArray categoriesArray = productJson.getJSONArray("categoryTree");

      for (Object c : categoriesArray) {
        JSONObject categoryJson = getInformationJson((JSONObject) c, stateJson);

        if (categoryJson.has("name") && !categoryJson.isNull("name")) {
          categories.add(categoryJson.get("name").toString());
        }
      }
    }

    return categories;
  }

  private List<String> scrapImages(JSONObject skuJson, JSONObject stateJson) {
    List<String> images = new ArrayList<>();

    for (String key : skuJson.keySet()) {
      if (key.startsWith("images")) {
        JSONArray imagesArray = skuJson.getJSONArray(key);

        for (Object o : imagesArray) {
          JSONObject image = getInformationJson((JSONObject) o, stateJson);

          if (image.has("imageUrl") && !image.isNull("imageUrl")) {
            images.add(CrawlerUtils.completeUrl(image.get("imageUrl").toString(), "https", "jumbo.vteximg.com.br"));
          }
        }

        break;
      }
    }

    return images;
  }

  private String scrapSecondaryImages(List<String> images) {
    String secondaryImages = null;
    JSONArray imagesArray = new JSONArray();

    if (!images.isEmpty()) {
      images.remove(0);

      for (String image : images) {
        imagesArray.put(image);
      }
    }

    if (imagesArray.length() > 0) {
      secondaryImages = imagesArray.toString();
    }

    return secondaryImages;
  }

  private Map<String, Prices> scrapMarketplace(JSONObject jsonSku, JSONObject stateJson) {
    Map<String, Prices> map = new HashMap<>();

    if (jsonSku.has("sellers") && !jsonSku.isNull("sellers")) {
      JSONArray sellers = jsonSku.getJSONArray("sellers");

      for (Object o : sellers) {
        JSONObject seller = getInformationJson((JSONObject) o, stateJson);

        if (seller.has("sellerName") && !seller.isNull("sellerName") && seller.has("commertialOffer") && !seller.isNull("commertialOffer")) {
          Prices prices = scrapPrices(getInformationJson(seller.getJSONObject("commertialOffer"), stateJson));

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
        prices.setPriceFrom(CrawlerUtils.getDoubleValueFromJSON(comertial, "ListPrice", true, false));

        prices.insertCardInstallment(Card.AMEX.toString(), installments);
        prices.insertCardInstallment(Card.SHOP_CARD.toString(), installments);
      }
    }

    return prices;
  }
}
