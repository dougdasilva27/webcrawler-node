package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (04/08/2016):
 * 
 * 1) For this crawler, we have one URL for multiple skus.
 * 
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) If the sku is unavailable, it's price is displayed.
 * 
 * 6) The price of sku, found in json script, is wrong when the same is unavailable, then it is not
 * crawled.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same
 * for all the variations of a given sku.
 * 
 * 7) The primary image is the first image on the secondary images.
 * 
 * 8) To get price, availability and Name sku Variations, for all products is accessed a page with
 * json.
 * 
 * 9) In json has variations of product.
 * 
 * 10)Apparently this market when accessing the api prices may see a different product because of a
 * "hash" in the url of api, then was found a script with the information that appears to be
 * commented on html, so when the api returns another product, we took the script information.
 * 
 * Examples: ex1 (available):
 * http://www.mobly.com.br/ar-condicionado-portatil-quente-e-frio-10500-btus-branco-160252.html ex2
 * (unavailable):
 * http://www.mobly.com.br/armario-multiuso-madeira-178cm-decoracao-marrom-venus-158006.html
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilMoblyCrawler extends Crawler {

  private final String HOME_PAGE = "https://www.mobly.com.br/";

  public BrasilMoblyCrawler(Session session) {
    super(session);
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

      // Pid
      String internalPid = crawlInternalPid(doc);

      // Name
      String nameMainPage = crawlName(doc);

      // Categories
      ArrayList<String> categories = crawlCategories(doc);
      String category1 = getCategory(categories, 0);
      String category2 = getCategory(categories, 1);
      String category3 = getCategory(categories, 2);

      // Primary image
      String primaryImage = crawlPrimaryImage(doc);

      // Secondary images
      String secondaryImages = crawlSecondaryImages(doc, primaryImage);

      // Description
      String description = crawlDescription(doc);

      // Stock
      Integer stock = null;

      // Sku variations
      Elements skus = doc.select(".product-option .custom-select option[data-js-function]");

      // JSON of Product in Html
      JSONObject jsonProduct = crawlJSONProduct(doc, internalPid);

      if (!skus.isEmpty()) {

        // IntenalIDS para requisição
        String internalIDS = crawlInternalIDS(skus);

        // Json from api
        JSONObject jsonProductsApi = this.fetchSkuInformation(internalIDS);

        for (Element sku : skus) {
          // InternalId
          String internalID = crawlInternalIdForMutipleVariations(sku);

          // Sku Json
          JSONObject jsonSku = this.assembleJsonProduct(internalID, internalPid, jsonProductsApi, jsonProduct);

          // Name
          String name = crawlNameForMutipleVariations(sku, nameMainPage);

          // Marketplace map
          Map<String, Float> marketplaceMap = crawlMarketplace(doc, jsonSku);

          // Price
          Float price = crawlPrice(marketplaceMap);

          // Availability
          boolean available = crawlAvailability(marketplaceMap);

          // Prices
          Prices prices = crawlPrices(jsonSku, price);

          // Marketplace
          Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap, jsonSku);

          // Creating the product
          Product product = new Product();
          product.setUrl(this.session.getOriginalURL());
          product.setInternalId(internalID);
          product.setInternalPid(internalPid);
          product.setName(name);
          product.setPrice(price);
          product.setPrices(prices);
          product.setAvailable(available);
          product.setCategory1(category1);
          product.setCategory2(category2);
          product.setCategory3(category3);
          product.setPrimaryImage(primaryImage);
          product.setSecondaryImages(secondaryImages);
          product.setDescription(description);
          product.setStock(stock);
          product.setMarketplace(marketplace);

          products.add(product);
        }

        /*
         * ********************************* crawling data of single product *
         ***********************************/

      } else {

        // InternalId
        String internalID = crawlInternalIdSingleProduct(doc);

        // Json from api
        JSONObject jsonProductApi = this.fetchSkuInformation(internalID);

        // Sku Json
        JSONObject jsonSku = this.assembleJsonProduct(internalID, internalPid, jsonProductApi, jsonProduct);

        // Marketplace map
        Map<String, Float> marketplaceMap = crawlMarketplace(doc, jsonSku);

        // Price
        Float price = crawlPrice(marketplaceMap);

        // Availability
        boolean available = crawlAvailability(marketplaceMap);

        // Prices
        Prices prices = crawlPrices(jsonSku, price);

        // Marketplace
        Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap, jsonSku);

        // Creating the product
        Product product = new Product();
        product.setUrl(this.session.getOriginalURL());
        product.setInternalId(internalID);
        product.setInternalPid(internalPid);
        product.setName(nameMainPage);
        product.setPrice(price);
        product.setPrices(prices);
        product.setAvailable(available);
        product.setCategory1(category1);
        product.setCategory2(category2);
        product.setCategory3(category3);
        product.setPrimaryImage(primaryImage);
        product.setSecondaryImages(secondaryImages);
        product.setDescription(description);
        product.setStock(stock);
        product.setMarketplace(marketplace);

        products.add(product);

      }


    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
    if (document.select("#product-info").first() != null) {
      return true;
    }
    return false;
  }

  /*********************
   * Variation methods *
   *********************/

  private String crawlInternalIdForMutipleVariations(Element sku) {
    String internalId = null;

    internalId = sku.val().trim();

    return internalId;
  }

  private String crawlInternalIDS(Elements skus) {
    String internalId = "";

    for (Element e : skus) {
      internalId = internalId + " " + e.val();
    }

    internalId = internalId.trim().replaceAll(" ", ",");

    return internalId;
  }

  private String crawlNameForMutipleVariations(Element sku, String name) {
    String nameVariation = name;

    if (sku.hasText()) {
      nameVariation = nameVariation + " " + sku.text();
    }

    return nameVariation;
  }

  /**********************
   * Single Sku methods *
   **********************/

  private String crawlInternalIdSingleProduct(Document document) {
    String internalId = null;
    Element internalIdElement = document.select(".add-wishlistsel-product-move-to-wishlist").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.attr("data-simplesku");
    }

    return internalId;
  }

  private Float crawlPrice(Map<String, Float> marketplaces) {
    Float price = null;

    if (marketplaces.containsKey("mobly")) {
      price = marketplaces.get("mobly");
    }

    return price;
  }

  private boolean crawlAvailability(Map<String, Float> marketplaces) {

    if (marketplaces.containsKey("mobly")) {
      return true;
    }

    return false;
  }

  /*******************
   * General methods *
   *******************/

  private JSONObject fetchSkuInformation(String internalIDS) {
    String url =
        "https://secure.mobly.com.br/api/catalog/price/hash/92b1f91a6df23cfc5e00a6fc26bcb27d2b2d9128/?skus=" + internalIDS + "&_=1550515484107";

    return CrawlerUtils.stringToJson(DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies));
  }

  private JSONObject assembleJsonProduct(String internalID, String internalPid, JSONObject jsonPage, JSONObject jsonProduct) {
    JSONObject jsonSku = new JSONObject();
    JSONObject jsonInformation = new JSONObject();

    if (jsonPage.has("priceStore")) {
      JSONObject jsonTemp = jsonPage.getJSONObject("priceStore");
      if (jsonTemp.has(internalPid)) {
        JSONObject pidJson = jsonTemp.getJSONObject(internalPid);

        if (pidJson.has("prices")) {
          JSONObject jsonPrices = pidJson.getJSONObject("prices");

          if (jsonPrices.has(internalID)) {
            jsonInformation = jsonPrices.getJSONObject(internalID);
          }
        }

        /**
         * in which case the api prices did not return the correct product, so the json is caught in a
         * javascript script in hmtl
         */

      } else {
        if (jsonProduct.has("prices")) {
          JSONObject jsonPrices = jsonProduct.getJSONObject("prices");
          if (jsonPrices.has(internalID)) {
            jsonInformation = jsonPrices.getJSONObject(internalID);
          }
        }
      }
    } else {
      if (jsonProduct.has("prices")) {
        JSONObject jsonPrices = jsonProduct.getJSONObject("prices");

        if (jsonPrices.has(internalID)) {
          jsonInformation = jsonPrices.getJSONObject(internalID);
        }
      }
    }

    if (jsonInformation.has("finalPrice")) {
      jsonSku.put("Price", jsonInformation.getString("finalPrice"));
    }

    if (jsonPage.has("stockStore")) {
      JSONObject stockStore = jsonPage.getJSONObject("stockStore");

      if (stockStore.has(internalID)) {
        Integer stock = stockStore.getInt(internalID);

        jsonSku.put("Available", stock > 0);
        jsonSku.put("Stock", stock);
      }
    }

    if (jsonInformation.has("option")) {
      jsonSku.put("NameVariation", jsonInformation.getString("option"));
    }
    if (jsonInformation.has("installmentsCount")) {
      jsonSku.put("InstallmentMax", Integer.parseInt(jsonInformation.get("installmentsCount").toString()));
    }
    if (jsonInformation.has("installmentsValue")) {
      jsonSku.put("InstallmentMaxValue",
          MathUtils.normalizeTwoDecimalPlaces(Float.parseFloat(jsonInformation.getString("installmentsValue").replace(",", "."))));
    }

    return jsonSku;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;

    Element internalPidElement = document.select("#configSku").first();

    if (internalPidElement != null) {
      internalPid = internalPidElement.attr("value").toString().trim();
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("h1.prd-title").first();

    if (nameElement != null) {
      name = nameElement.text().toString().trim();
    }

    return name;
  }

  private Map<String, Float> crawlMarketplace(Document document, JSONObject jsonSku) {
    Map<String, Float> marketplaces = new HashMap<>();

    Element marketplace = document.select(".prd-supplier").first();
    if (marketplace != null) {
      String text = marketplace.text().toLowerCase().trim();
      if (jsonSku.has("Available")) {
        if (jsonSku.getBoolean("Available")) {
          if (jsonSku.has("Price")) {
            Float price = Float.parseFloat(jsonSku.getString("Price").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
            marketplaces.put(text, price);
          }
        }
      }
    } else {
      Float price = null;
      if (jsonSku.has("Available")) {
        if (jsonSku.getBoolean("Available")) {
          if (jsonSku.has("Price")) {
            price = Float.parseFloat(jsonSku.getString("Price").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
            marketplaces.put("mobly", price);
          }
        }
      }
    }

    return marketplaces;
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap, JSONObject jsonSku) {
    Marketplace marketplace = new Marketplace();

    for (String sellerName : marketplaceMap.keySet()) {
      if (!sellerName.equals("mobly")) {
        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", sellerName);
        sellerJSON.put("price", marketplaceMap.get(sellerName));
        sellerJSON.put("prices", crawlPrices(jsonSku, marketplaceMap.get(sellerName)).toJSON());

        try {
          Seller seller = new Seller(sellerJSON);
          marketplace.add(seller);
        } catch (Exception e) {
          Logging.printLogError(logger, session, Util.getStackTraceString(e));
        }
      }
    }

    return marketplace;
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".picture > a").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("href").trim();

      if (!primaryImage.startsWith("http")) {
        primaryImage = "https:" + primaryImage;
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();
    Set<String> secondaryImagesSet = new HashSet<>();

    Elements imagesElement = document.select(".product-thumbs .images img");

    for (int i = 1; i < imagesElement.size(); i++) { // start with indez 1 because the first image
                                                     // is the primary image
      Element e = imagesElement.get(i);
      String image = null;

      if (e.hasAttr("data-image-big") && !e.attr("data-image-big").isEmpty()) {
        image = e.attr("data-image-big").trim();
      } else if (e.hasAttr("data-image-product") && !e.attr("data-image-product").isEmpty()) {
        image = e.attr("data-image-product").trim();
      } else {
        Element img = e.select("img").first();

        if (img != null) {
          image = e.attr("src").trim();
        }
      }

      if (image != null) {
        if (!image.startsWith("http")) {
          image = "https:" + image;
        }

        if (!image.equals(primaryImage)) {
          secondaryImagesSet.add(image);
        }
      }
    }

    for (String image : secondaryImagesSet) {
      secondaryImagesArray.put(image);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private ArrayList<String> crawlCategories(Document document) {
    ArrayList<String> categories = new ArrayList<String>();
    Elements elementCategories = document.select(".breadcrumb ul li a");

    for (int i = 0; i < elementCategories.size(); i++) {
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String getCategory(ArrayList<String> categories, int n) {
    if (n < categories.size()) {
      return categories.get(n);
    }

    return "";
  }

  private String crawlDescription(Document document) {
    String description = "";
    Element specElement = document.select("#product-attributes").first();
    Element info = document.select(".tab-box.description-text article").first();

    if (info != null) {
      description = description + info.html();
    }

    if (specElement != null) {
      description = description + specElement.html();
    }

    return description;
  }

  private Prices crawlPrices(JSONObject jsonSku, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      // Preço 1 vez no cartão é igual do boleto
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      if (jsonSku.has("InstallmentMax")) {
        Integer installment = jsonSku.getInt("InstallmentMax");

        if (jsonSku.has("InstallmentMaxValue")) {
          Double valueDouble = jsonSku.getDouble("InstallmentMaxValue");
          Float value = valueDouble.floatValue();

          installmentPriceMap.put(installment, value);

          prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
        }
      }
    }

    return prices;
  }

  private JSONObject crawlJSONProduct(Document doc, String internalPid) {
    JSONObject jsonProduct = new JSONObject();
    Element scriptElement = doc.select("#lazyJavascriptInFileCode").first();

    String indexPid = "detail.priceStore[\"" + internalPid + "\"] =";
    String script = scriptElement.outerHtml();

    if (script.contains(indexPid)) {
      int x = script.indexOf(indexPid) + indexPid.length();
      int y = script.indexOf(";", x);

      String json = script.substring(x, y).trim();

      try {
        jsonProduct = new JSONObject(json);
      } catch (JSONException e) {
      }
    }

    return jsonProduct;
  }
}
