package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

/**
 * date: 27/03/2018
 * 
 * @author gabriel
 *
 */

public class BrasilCentauroCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.centauro.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "centauro";

  public BrasilCentauroCrawler(Session session) {
    super(session);
  }

  // private String userAgent;
  //
  // @Override
  // public void handleCookiesBeforeFetch() {
  // this.userAgent = DataFetcher.randUserAgent();
  // Map<String, String> cookiesMap = DataFetcher.fetchCookies(session, HOME_PAGE, cookies, null, 1);
  //
  // for (Entry<String, String> entry : cookiesMap.entrySet()) {
  // BasicClientCookie cookie = new BasicClientCookie(entry.getKey(), entry.getValue());
  // cookie.setDomain(".netshoes.com.br");
  // cookie.setPath("/");
  // this.cookies.add(cookie);
  // }
  // }
  //
  // @Override
  // protected Object fetch() {
  // LettProxy proxy = session.getRequestProxy(HOME_PAGE);
  // String page = GETFetcher.fetchPageGET(session, session.getOriginalURL(), cookies, this.userAgent,
  // proxy, 1);
  //
  // return Jsoup.parse(page);
  // }

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

      JSONObject chaordicJson = crawlChaordicJson(doc);
      String internalPid = crawlInternalPid(chaordicJson);
      CategoryCollection categories = crawlCategories(doc);
      String description = crawlDescription(doc);

      Map<String, String> specsMap = crawlSpecsMap(chaordicJson);

      // sku data in json
      JSONArray arraySkus = chaordicJson != null && chaordicJson.has("skus") ? chaordicJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = crawlInternalId(jsonSku);
        String name = crawlName(chaordicJson, jsonSku, specsMap);
        boolean availableToBuy = jsonSku.has("status") && jsonSku.get("status").toString().equals("available");
        Map<String, Prices> marketplaceMap = availableToBuy ? crawlMarketplace(doc, jsonSku) : new HashMap<>();
        boolean available = availableToBuy ? marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) : false;

        Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);
        Prices prices = available ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
        Float price = crawlPrice(prices);
        String colorId = crawlColorId(jsonSku);
        String primaryImage = crawlPrimaryImage(doc, colorId);
        String secondaryImages = crawlSecondaryImages(doc, colorId);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setMarketplace(marketplace).build();

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
    return document.select(".product-item").first() != null;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("sku")) {
      internalId = json.getString("sku").trim();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject skuJson) {
    String internalPid = null;

    if (skuJson.has("id")) {
      internalPid = skuJson.get("id").toString();
    }

    return internalPid;
  }

  private String crawlName(JSONObject chaordicJson, JSONObject skuJson, Map<String, String> specsMap) {
    StringBuilder name = new StringBuilder();

    if (chaordicJson.has("name")) {
      name.append(chaordicJson.getString("name"));

      if (skuJson.has("specs")) {
        JSONObject specs = skuJson.getJSONObject("specs");

        Set<String> keys = specs.keySet();

        for (String key : keys) {
          String id = specs.get(key).toString();

          if (specsMap.containsKey(id)) {
            name.append(" " + specsMap.get(id));
          }
        }
      }
    }

    return name.toString();
  }

  private String crawlColorId(JSONObject skuJson) {
    String colorId = null;

    if (skuJson.has("specs")) {
      JSONObject specs = skuJson.getJSONObject("specs");

      if (specs.has("cor")) {
        colorId = specs.getString("cor");
      }
    }

    return colorId;
  }

  /**
   * This site has ids for each specification of product variation like this: { "id": "201", "label":
   * "GG" }, { "id": "302", "label": "preto" }
   * 
   * @param chaordicJson
   * @return
   */
  private Map<String, String> crawlSpecsMap(JSONObject chaordicJson) {
    Map<String, String> specsMap = new HashMap<>();

    if (chaordicJson.has("specs")) {
      JSONObject specs = chaordicJson.getJSONObject("specs");
      Set<String> keys = specs.keySet();

      for (String key : keys) {
        JSONArray specsArray = specs.getJSONArray(key);

        for (int i = 0; i < specsArray.length(); i++) {
          JSONObject spec = specsArray.getJSONObject(i);

          if (spec.has("id") && spec.has("label")) {
            specsMap.put(spec.get("id").toString(), spec.get("label").toString());
          }
        }
      }
    }

    return specsMap;
  }

  private Float crawlPrice(Prices prices) {
    Float price = null;

    if (!prices.isEmpty() && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
      Double priceDouble = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
      price = priceDouble.floatValue();
    }

    return price;
  }

  private String crawlPrimaryImage(Document doc, String colorId) {
    String primaryImage = null;

    String selector = colorId != null ? ".main-images[data-ref=" + colorId + "] a.main-image" : ".main-images a.main-image";

    Element image = doc.select(selector).first();

    if (image != null) {
      primaryImage = image.attr("href");
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc, String colorId) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    String selector = colorId != null ? ".main-images[data-ref=" + colorId + "] a.main-image" : ".main-images a.main-image";

    Elements images = doc.select(selector);

    for (int i = 1; i < images.size(); i++) { // first index is the primaryimage
      Element e = images.get(i);
      secondaryImagesArray.put(e.attr("href"));
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private Map<String, Prices> crawlMarketplace(Document doc, JSONObject jsonSku) {
    Map<String, Prices> marketplace = new HashMap<>();

    String sellerName = MAIN_SELLER_NAME_LOWER;
    Element sellerNameElement = doc.select(".product-seller-name").first();

    if (sellerNameElement != null) {
      sellerName = sellerNameElement.ownText().toLowerCase();
    }

    marketplace.put(sellerName, crawlPrices(doc, jsonSku));

    return marketplace;

  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap) {
    Marketplace marketplace = new Marketplace();

    for (Entry<String, Prices> seller : marketplaceMap.entrySet()) {
      if (!seller.getKey().equalsIgnoreCase(MAIN_SELLER_NAME_LOWER)) {
        Prices prices = seller.getValue();

        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", seller.getKey());
        sellerJSON.put("price", crawlPrice(prices));
        sellerJSON.put("prices", prices.toJSON());

        try {
          Seller s = new Seller(sellerJSON);
          marketplace.add(s);
        } catch (Exception e) {
          Logging.printLogWarn(logger, session, Util.getStackTraceString(e));
        }
      }
    }

    return marketplace;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumbs li > a span");

    for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element shortDescription = doc.select(".classFichaDivPrincipalLinha").first();
    if (shortDescription != null) {
      description.append(shortDescription.html());
    }

    return description.toString();
  }

  /**
   * To crawl this prices is accessed a api Is removed all accents for crawl price 1x like this: Visa
   * à vista R$ 1.790,00
   * 
   * @param internalId
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, JSONObject jsonSku) {
    Prices prices = new Prices();

    if (jsonSku.has("price") && jsonSku.get("price") instanceof Double) {
      Map<Integer, Float> mapInstallments = new HashMap<>();
      Float price = MathUtils.normalizeTwoDecimalPlaces(((Double) jsonSku.getDouble("price")).floatValue());

      mapInstallments.put(1, price);
      prices.setBankTicketPrice(price);

      if (jsonSku.has("installment")) {
        JSONObject objInstallment = jsonSku.getJSONObject("installment");

        if (objInstallment.has("count") && objInstallment.get("count") instanceof Integer && objInstallment.has("price")
            && objInstallment.get("price") instanceof Double) {

          mapInstallments.put(objInstallment.getInt("count"),
              MathUtils.normalizeTwoDecimalPlaces(((Double) objInstallment.getDouble("price")).floatValue()));
        }
      }

      if (jsonSku.has("old_price") && jsonSku.get("old_price") instanceof Double) {
        prices.setPriceFrom(jsonSku.getDouble("old_price"));
      }

      prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);
      prices.insertCardInstallment(Card.DINERS.toString(), mapInstallments);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.ELO.toString(), mapInstallments);
      prices.insertCardInstallment(Card.SHOP_CARD.toString(), mapInstallments);

    }

    return prices;
  }

  private JSONObject crawlChaordicJson(Document doc) {
    JSONObject skuJson = new JSONObject();

    Elements scripts = doc.select("script");

    for (Element e : scripts) {
      String script = e.outerHtml();


      if (script.contains("window.chaordic_meta = ")) {
        String token = "window.chaordic_meta = ";
        int x = script.indexOf(token) + token.length();
        int y = script.lastIndexOf(';');

        String json = script.substring(x, y);

        if (json.startsWith("{") && json.endsWith("}")) {
          try {
            JSONObject chaordic = new JSONObject(json);

            if (chaordic.has("product")) {
              skuJson = chaordic.getJSONObject("product");
            }
          } catch (Exception e1) {
            Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e1));
          }
        }

        break;
      }
    }

    return skuJson;
  }
}
