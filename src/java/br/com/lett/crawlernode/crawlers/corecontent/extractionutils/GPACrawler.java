package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class GPACrawler {

  private Session session;
  private String homePage;
  private String homePageHttp;
  private Logger logger;
  private String storeId;
  private String store;
  private List<Cookie> cookies = new ArrayList<>();

  /**
   * Cep's gpa
   * 
   * São paulo: 01007-040 Curitiba: 80010-080 Brasília: 70330-500 Rio de Janeiro: 20021-020
   * 
   * @param logger
   * @param session
   * @param homePage - https
   * @param homePageHttp - http
   * @param storeId - needed to access api
   * @param cookies
   * @param store - "ex" to extra and "pa" to paodeacucar
   */
  public GPACrawler(Logger logger, Session session, String homePage, String homePageHttp, String storeId, List<Cookie> cookies, String store) {
    this.logger = logger;
    this.session = session;
    this.homePage = homePage;
    this.homePageHttp = homePageHttp;
    this.storeId = storeId;
    this.store = store;
    this.cookies = cookies;
  }

  public List<Product> extractInformation() throws Exception {
    List<Product> products = new ArrayList<>();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // Url
      String productUrl = session.getOriginalURL();

      JSONObject jsonSku = crawlProductInformatioFromGPAApi(productUrl);

      String internalId = crawlInternalId(jsonSku);
      String internalPid = crawlInternalPid(jsonSku);
      CategoryCollection categories = crawlCategories(jsonSku);
      String description = crawlDescription(jsonSku);
      boolean available = crawlAvailability(jsonSku);
      Float price = available ? crawlPrice(jsonSku) : null;
      Double priceFrom = available ? crawlPriceFrom(jsonSku) : null;
      String primaryImage = crawlPrimaryImage(jsonSku);
      String name = crawlName(jsonSku);
      String secondaryImages = crawlSecondaryImages(jsonSku, primaryImage);
      Prices prices = crawlPrices(price, priceFrom);
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
    if (url.contains("/produto/")) {
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

  private Double crawlPriceFrom(JSONObject json) {
    Double price = null;

    if (json.has("priceFrom")) {
      Object pObj = json.get("priceFrom");

      if (pObj instanceof Double) {
        price = (Double) pObj;
      }
    }

    return price;
  }

  private Float crawlPrice(JSONObject json) {
    Float price = null;

    if (json.has("currentPrice")) {
      Object pObj = json.get("currentPrice");

      if (pObj instanceof Double) {
        price = MathUtils.normalizeTwoDecimalPlaces(((Double) pObj).floatValue());
      }
    }

    if (json.has("productPromotion")) {
      JSONObject productPromotion = json.getJSONObject("productPromotion");

      if (productPromotion.has("unitPrice") && productPromotion.has("promotionPercentOffOnUnity")) {
        Object promotionPercentOffOnUnity = productPromotion.get("promotionPercentOffOnUnity");

        if (promotionPercentOffOnUnity instanceof Integer) {
          Integer promotion = (Integer) promotionPercentOffOnUnity;

          if (promotion == 1) {
            price = CrawlerUtils.getFloatValueFromJSON(productPromotion, "unitPrice");
          }
        }
      }
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject json) {
    return json.has("stock") && json.getBoolean("stock");
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

      for (String key : images.keySet()) { // index 0 may be a primary Image
        JSONObject imageObj = images.getJSONObject(key);

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

      Set<String> listCategories = new HashSet<>(); // It is a "set" because it has been noticed that there are repeated categories

      if (shelfList.length() > 0) {
        JSONObject cat1 = shelfList.getJSONObject(shelfList.length() - 1);
        JSONObject cat2 = shelfList.getJSONObject(0);

        if (cat1.has("name")) {
          listCategories.add(cat1.getString("name"));
        }

        if (cat2.has("name")) {
          listCategories.add(cat2.getString("name"));
        }
      }

      for (String category : listCategories) {
        categories.add(category);
      }
    }

    return categories;
  }


  private String crawlDescription(JSONObject json) {
    StringBuilder description = new StringBuilder();

    if (json.has("shortDescription") && json.get("shortDescription") instanceof String) {
      description.append(json.getString("shortDescription"));
    }

    // Ex: https://www.paodeacucar.com/produto/329137
    if (json.has("itemMap")) {
      JSONArray itemMap = json.getJSONArray("itemMap");

      if (itemMap.length() > 0) {
        description.append("<table class=\"nutritional-table table product-table\">\n" + "                                <thead>\n"
            + "                                    <tr>\n"
            + "                                        <th colspan=\"2\" class=\"title\">Produtos no kit</th>\n"
            + "                                    </tr>\n" + "                                    <tr>\n"
            + "                                        <th>Nome</th>\n" + "                                        <th>Quantidade</th>\n"
            + "                                    </tr>\n" + "                                </thead>\n"
            + "                                <tbody>\n");
        for (int i = 0; i < itemMap.length(); i++) {
          JSONObject productInfo = itemMap.getJSONObject(i);

          if (productInfo.has("quantity") && productInfo.get("quantity") instanceof Integer && productInfo.has("name")) {
            int quantity = productInfo.getInt("quantity");
            String name = productInfo.get("name").toString();

            if (quantity > 1 || itemMap.length() > 1) {
              description.append("<tr ng-repeat=\"item in productDetailCtrl.product.itemMap\" class=\"ng-scope\">\n"
                  + "        <td ng-class=\"{'last':$last}\" class=\"ng-binding last\">" + name + "l</td>\n"
                  + "        <td ng-class=\"{'last':$last}\" class=\"ng-binding last\">" + quantity + "</td>\n"
                  + "     </tr><!-- end ngRepeat: item in productDetailCtrl.product.itemMap -->\n");
            }
          }
        }

        description.append("</tbody>\n</table>");
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

      description.append(str.toString());
    }

    return description.toString();
  }

  private String crawlNutritionalTableAttributes(JSONObject nutritionalMap) {
    StringBuilder str = new StringBuilder();
    str.append("<tbody>");

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
  private Prices crawlPrices(Float price, Double priceFrom) {
    Prices p = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      p.setPriceFrom(priceFrom);
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

    String url = "https://api.gpa.digital/" + this.store + "/products/" + id + "?storeId=" + storeId + "&isClienteMais=false";

    String res = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);

    try {
      JSONObject apiGPA = new JSONObject(res);
      if (apiGPA.has("content")) {
        productsInfo = apiGPA.getJSONObject("content");
      }
    } catch (JSONException e) {
      Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
    }

    return productsInfo;
  }
}
