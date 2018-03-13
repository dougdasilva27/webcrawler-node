package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

public class GPACrawler {

  private Session session;
  private String homePage;
  private String homePageHttp;
  private Logger logger;
  private String storeId;
  private List<Cookie> cookies = new ArrayList<>();

  public GPACrawler(Logger logger, Session session, String homePage, String homePageHttp, String storeId, List<Cookie> cookies) {
    this.logger = logger;
    this.session = session;
    this.homePage = homePage;
    this.homePageHttp = homePageHttp;
    this.storeId = storeId;
    this.cookies = cookies;
  }

  protected Object fetch() {
    JSONObject productsInfo = new JSONObject();

    String productUrl = session.getOriginalURL();

    String id;
    if (productUrl.startsWith(homePage)) {
      id = productUrl.replace(homePage, "").split("/")[2];
    } else {
      id = productUrl.replace(homePageHttp, "").split("/")[2];
    }

    String url = "https://api.gpa.digital/ex/products/" + id + "?storeId=" + storeId + "&isClienteMais=false";

    String res = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);

    try {
      JSONObject apiGPA = new JSONObject(res);
      if (apiGPA.has("content")) {
        productsInfo = apiGPA.getJSONObject("content");
      }
    } catch (JSONException e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }

    return productsInfo;
  }

  public List<Product> extractInformation() throws Exception {
    List<Product> products = new ArrayList<>();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // Url
      String productUrl = session.getOriginalURL();

      JSONObject jsonSku = crawlProductInformatioFromGPAApi(productUrl);

      // InternalId
      String internalId = crawlInternalId(jsonSku);

      // Pid
      String internalPid = crawlInternalPid(jsonSku);

      // Categories
      CategoryCollection categories = crawlCategories(jsonSku);

      // Description
      String description = crawlDescription(jsonSku);

      // Availability
      boolean available = crawlAvailability(jsonSku);

      // Price
      Float price = available ? crawlPrice(jsonSku) : null;

      // Primary image
      String primaryImage = crawlPrimaryImage(jsonSku);

      // Name
      String name = crawlName(jsonSku);

      // Secondary images
      String secondaryImages = crawlSecondaryImages(jsonSku, primaryImage);

      // Prices
      Prices prices = crawlPrices(price);

      // Stock
      Integer stock = null;

      // Pid não sendo null, o produto não está void
      if (internalPid != null && session.getRedirectedToURL(productUrl) != null) {
        productUrl = session.getRedirectedToURL(productUrl);
      }

      // Creating the product
      Product product = ProductBuilder.create().setUrl(productUrl).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(new Marketplace()).build();

      products.add(product);


    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url) {
    if (url.contains("paodeacucar.com/produto/")) {
      return true;
    }
    return false;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("id")) {
      internalId = json.get("id").toString();
    }

    return internalId;
  }


  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("id")) {
      internalPid = json.getString("sku");
    }

    return internalPid;
  }

  private String crawlName(JSONObject json) {
    String name = null;

    if (json.has("name")) {
      name = json.getString("name");
    }

    return name;
  }

  private Float crawlPrice(JSONObject json) {
    Float price = null;

    if (json.has("currentPrice")) {
      Object pObj = json.get("currentPrice");

      if (pObj instanceof Double) {
        price = MathCommonsMethods.normalizeTwoDecimalPlaces(((Double) pObj).floatValue());
      }
    }


    if (json.has("productPromotion") && json.get("productPromotion") instanceof JSONObject) {
      JSONObject productPromotion = json.getJSONObject("productPromotion");

      if (productPromotion.has("unitPrice")) {
        Object pObj = productPromotion.get("unitPrice");

        if (pObj instanceof Double) {
          price = MathCommonsMethods.normalizeTwoDecimalPlaces(((Double) pObj).floatValue());
        }
      }
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject json) {
    if (json.has("stock") && json.getBoolean("stock")) {
      return true;
    }

    return false;
  }

  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("mapOfImages")) {
      JSONObject images = json.getJSONObject("mapOfImages");

      for (int i = 0; i < images.length(); i++) {
        if (images.length() > 0 && images.has(Integer.toString(i))) {
          JSONObject imageObj = images.getJSONObject(Integer.toString(i));

          if (imageObj.has("BIG") && !imageObj.getString("BIG").isEmpty()) {
            String image = homePage + imageObj.getString("BIG");

            if (image.contains("img")) {
              primaryImage = homePage + imageObj.getString("BIG");
            }
          } else if (imageObj.has("MEDIUM") && !imageObj.getString("MEDIUM").isEmpty()) {
            String image = homePage + imageObj.getString("MEDIUM");;

            if (image.contains("img")) {
              primaryImage = homePage + imageObj.getString("MEDIUM");
            }
          } else if (imageObj.has("SMALL") && !imageObj.getString("SMALL").isEmpty()) {
            String image = homePage + imageObj.getString("SMALL");;

            if (image.contains("img")) {
              primaryImage = homePage + imageObj.getString("SMALL");
            }
          }
        }

        if (primaryImage != null) {
          break;
        }
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(JSONObject json, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    String primaryImageId = getImageId(primaryImage);

    if (json.has("mapOfImages")) {
      JSONObject images = json.getJSONObject("mapOfImages");

      for (int i = 1; i < images.length(); i++) { // index 0 may be a primary Image
        if (images.length() > 0 && images.has(Integer.toString(i))) {
          JSONObject imageObj = images.getJSONObject(Integer.toString(i));

          if (imageObj.has("BIG") && !imageObj.getString("BIG").isEmpty()) {
            String image = homePage + imageObj.getString("BIG");
            String imageId = getImageId(image);

            if (image.contains("img") && !imageId.equals(primaryImageId)) {
              secondaryImagesArray.put(homePage + imageObj.getString("BIG"));
            }
          } else if (imageObj.has("MEDIUM") && !imageObj.getString("MEDIUM").isEmpty()) {
            String image = homePage + imageObj.getString("MEDIUM");
            String imageId = getImageId(image);

            if (image.contains("img") && !imageId.equals(primaryImageId)) {
              secondaryImagesArray.put(homePage + imageObj.getString("MEDIUM"));
            }
          } else if (imageObj.has("SMALL") && !imageObj.getString("SMALL").isEmpty()) {
            String image = homePage + imageObj.getString("SMALL");
            String imageId = getImageId(image);

            if (image.contains("img") && !imageId.equals(primaryImageId)) {
              secondaryImagesArray.put(homePage + imageObj.getString("SMALL"));
            }
          }
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String getImageId(String imageUrl) {
    if (imageUrl != null) {
      return imageUrl.replace(homePage, "").split("/")[4];
    }

    return null;
  }

  private CategoryCollection crawlCategories(JSONObject json) {
    CategoryCollection categories = new CategoryCollection();

    if (json.has("shelfList")) {
      JSONArray shelfList = json.getJSONArray("shelfList");

      List<String> listCategories = new ArrayList<>(); // It is a "set" because it has been noticed that there are repeated categories

      for (int i = shelfList.length() - 1; i >= 0; i--) { // the last item is the first category and the first item is the last category
        JSONObject cat = shelfList.getJSONObject(i);

        if (cat.has("name") && !listCategories.contains(cat.getString("name"))) {
          listCategories.add(cat.getString("name"));
        }
      }

      for (String category : listCategories) {
        categories.add(category);
      }
    }

    return categories;
  }


  private String crawlDescription(JSONObject json) {
    String description = "";

    if (json.has("shortDescription")) {
      if (json.get("shortDescription") instanceof String) {
        description += json.getString("shortDescription");
      }
    }

    // This key in json has a map of attributes -> {label: "", value = ""} , For crawl niutritional
    // table we make the html and put the values in html
    if (json.has("nutritionalMap") && json.getJSONObject("nutritionalMap").length() > 0) {
      JSONObject nutritionalJson = json.getJSONObject("nutritionalMap");

      StringBuilder str = new StringBuilder();

      str.append("<div class=\"product-nutritional-table\">\n" + "  <p class=\"title\">Tabela nutricional</p>\n"
          + "   <!-- ngIf: productDetailCtrl.product.nutritionalMap.cabecalho -->"
          + "<div class=\"main-infos ng-scope\" ng-if=\"productDetailCtrl.product.nutritionalMap.cabecalho\">\n"
          + "           <p ng-bind-html=\"productDetailCtrl.product.nutritionalMap.cabecalho || "
          + "productDetailCtrl.product.nutritionalMap.cabecalho.value\" class=\"ng-binding\"></p>\n"
          + "       </div><!-- end ngIf: productDetailCtrl.product.nutritionalMap.cabecalho -->\n"
          + "       <table class=\"table table-responsive\">\n" + "         <thead>\n" + "              <tr>\n" + "                 <th>Item</th>\n"
          + "                   <th>Quantidade por porção</th>\n" + "                   <th>Valores diários</th>\n" + "             </tr>\n"
          + "           </thead>\n");
      str.append(crawlNutritionalTableAttributes(nutritionalJson));
      str.append("</table>\n</div>");

      description += str.toString();
    }

    return description;
  }

  private String crawlNutritionalTableAttributes(JSONObject nutritionalMap) {
    StringBuilder str = new StringBuilder();
    str.append("<tbody>");

    @SuppressWarnings("unchecked")
    Set<String> attributesList = nutritionalMap.keySet();

    for (String attribute : attributesList) {
      if (!(nutritionalMap.get(attribute) instanceof String)) {
        JSONObject attributeJson = nutritionalMap.getJSONObject(attribute);

        if (attributeJson.has("value") && attributeJson.has("label")) {
          str.append(putAttribute(attributeJson.getString("value"), attributeJson.getString("label")));
        }
      } else {
        str.append("<div class=\"main-infos ng-scope\" ng-if=\"productDetailCtrl.product.nutritionalMap.cabecalho\">\n"
            + "<p ng-bind-html=\"productDetailCtrl.product.nutritionalMap.cabecalho "
            + "|| productDetailCtrl.product.nutritionalMap.cabecalho.value\" class=\"ng-binding\">" + nutritionalMap.getString(attribute) + "</p>\n"
            + "</div>");
      }
    }

    str.append("</tbody");
    return str.toString();
  }

  private String putAttribute(String value, String label) {
    if (label != null) {
      if (label.equalsIgnoreCase("rodape")) {
        return "<tfoot>\n" + "  <tr>\n" + "     <td colspan=\"3\" ng-bind-html=\"productDetailCtrl.product.nutritionalMap.rodape.value\""
            + "class=\"last ng-binding\">" + value + "</td>\n" + "  </tr>\n" + "</tfoot>\n";
      } else {
        return "    <tr ng-repeat=\"(key, item) in productDetailCtrl.product.nutritionalMap \" ng-if=\"[ 'cabecalho', 'rodape'].indexOf(key) === -1 \" class=\"ng-scope\">\n"
            + "     <td class=\"ng-binding\">" + label + "</td>\n" + "      <td class=\"ng-binding\">" + value + "</td>\n"
            + "     <td class=\"ng-binding\"></td>\n" + "   </tr><!-- end ngIf: [ 'cabecalho', 'rodape'].indexOf(key) === -1 --><!-- end ngRepeat: "
            + "(key, item) in productDetailCtrl.product.nutritionalMap --><!-- ngIf: [ 'cabecalho', 'rodape'].indexOf(key) === -1 -->"
            + "<tr ng-repeat=\"(key, item) in productDetailCtrl.product.nutritionalMap \" ng-if=\"[ 'cabecalho', 'rodape'].indexOf(key) === -1 "
            + "\" class=\"ng-scope\">\n";
      }
    }

    return "";
  }

  /**
   * In this site has no information of installments
   * 
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price) {
    Prices p = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      p.setBankTicketPrice(price);

      p.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);

    }

    return p;
  }


  /**
   * Get the json of gpa api, this api has all info of product
   * 
   * @return
   */
  private JSONObject crawlProductInformatioFromGPAApi(String productUrl) {
    JSONObject productsInfo = new JSONObject();

    String id;
    if (productUrl.startsWith(homePage)) {
      id = productUrl.replace(homePage, "").split("/")[2];
    } else {
      id = productUrl.replace(homePageHttp, "").split("/")[2];
    }

    String url = "https://api.gpa.digital/pa/products/" + id + "?storeId=" + storeId + "&isClienteMais=false";

    String res = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);

    try {
      JSONObject apiGPA = new JSONObject(res);
      if (apiGPA.has("content")) {
        productsInfo = apiGPA.getJSONObject("content");
      }
    } catch (JSONException e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }

    return productsInfo;
  }
}
