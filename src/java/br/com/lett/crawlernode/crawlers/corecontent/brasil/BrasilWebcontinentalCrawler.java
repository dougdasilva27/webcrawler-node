package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
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

public class BrasilWebcontinentalCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.webcontinental.com.br";
  private static final String SELLER_NAME_LOWER = "webcontinental";

  public BrasilWebcontinentalCrawler(Session session) {
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

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = crawlInternalPid(doc);
      JSONObject productInformation = fetchJsonProduct(internalPid);
      String primaryImage = crawlPrimaryImage(productInformation);
      String secondaryImages = crawlSecondaryImages(productInformation, primaryImage);
      CategoryCollection categories = crawlCategories(productInformation);
      String description = crawlDescription(productInformation);

      if (productInformation.has("skus")) {
        // sku data in json
        JSONArray arraySkus = productInformation.getJSONArray("skus");

        for (int i = 0; i < arraySkus.length(); i++) {
          JSONObject jsonSku = arraySkus.getJSONObject(i);

          String internalId = crawlInternalId(jsonSku);
          String name = crawlName(productInformation, jsonSku);
          JSONObject sellerInfo = internalId != null ? crawlPricesJson(internalId) : new JSONObject();
          boolean availableForBuy = sellerInfo.has("estoque") && sellerInfo.getBoolean("estoque");
          Marketplace marketplace = availableForBuy ? crawlMarketplace(sellerInfo) : new Marketplace();
          boolean available = availableForBuy && marketplace.isEmpty();
          Float price = available ? crawlPrice(jsonSku) : null;
          Prices prices = available ? crawlPrices(jsonSku, price) : new Prices();

          Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid)
              .setName(name).setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
              .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
              .setSecondaryImages(secondaryImages).setDescription(description).setStock(null).setMarketplace(marketplace).build();

          products.add(product);
        }
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url) {
    if (url.contains("/product/") && url.startsWith(HOME_PAGE)) {
      return true;
    }
    return false;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("skuId")) {
      internalId = json.getString("skuId");
    }

    return internalId;
  }


  private String crawlInternalPid(Document doc) {
    String internalPid = null;
    Elements scripts = doc.select("script[type=\"text/javascript\"]");
    String token = "varrequire=";

    for (Element e : scripts) {
      String script = e.outerHtml().replaceAll(" ", "");

      if (script.contains(token)) {
        int x = script.indexOf(token) + token.length();
        int y = script.indexOf(";", x);

        String json = script.substring(x, y);

        try {
          JSONObject require = new JSONObject(json);

          if (require.has("config")) {
            JSONObject config = require.getJSONObject("config");

            if (config.has("ccNavState")) {
              JSONObject product = config.getJSONObject("ccNavState");

              if (product.has("pageContext")) {
                internalPid = product.getString("pageContext");
              }
            }
          }

        } catch (JSONException ex) {
          Logging.printLogWarn(logger, CommonMethods.getStackTrace(ex));
        }

        break;
      }
    }

    return internalPid;
  }

  private String crawlName(JSONObject infoProduct, JSONObject sku) {
    String name = null;

    if (infoProduct.has("name")) {
      name = infoProduct.getString("name").replace("'", "").replace("’", "").trim();
    }

    if (sku.has("skuName")) {
      name += " " + sku.getString("skuName");
    }

    return name;
  }

  private Float crawlPrice(JSONObject sku) {
    Float price = null;

    if (sku.has("prices")) {
      JSONObject prices = sku.getJSONObject("prices");

      if (prices.has("realaPrazo") && prices.get("realaPrazo") instanceof Double) {
        Double priceDouble = prices.getDouble("realaPrazo");
        price = MathUtils.normalizeTwoDecimalPlaces(priceDouble.floatValue());
      }
    }

    return price;
  }

  private String crawlPrimaryImage(JSONObject productInfo) {
    String primaryImage = null;

    if (productInfo.has("primaryImage")) {
      primaryImage = productInfo.getString("primaryImage");
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(JSONObject productInfo, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (productInfo.has("secondaryImages")) {
      JSONArray imagesArray = productInfo.getJSONArray("secondaryImages");

      for (int i = 0; i < imagesArray.length(); i++) {
        String image = imagesArray.getString(i);

        if (!primaryImage.contains(image) && !primaryImage.equals(image)) {
          if (image.startsWith(HOME_PAGE)) {
            secondaryImagesArray.put(CommonMethods.sanitizeUrl(image));
          } else {
            secondaryImagesArray.put(CommonMethods.sanitizeUrl(HOME_PAGE + image));
          }
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(JSONObject productInfo) {
    CategoryCollection categories = new CategoryCollection();

    if (productInfo.has("categories")) {
      JSONArray catArray = productInfo.getJSONArray("categories");

      for (int i = 0; i < catArray.length(); i++) {
        categories.add(catArray.getString(0));
      }
    }

    return categories;

  }


  private String crawlDescription(JSONObject productInfo) {
    String description = "";

    if (productInfo.has("description")) {
      description = productInfo.getString("description");
    }

    return description;
  }

  private JSONObject crawlPricesJson(String internalId) {
    JSONObject product = new JSONObject();
    String payload = "data=" + internalId;

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");

    JSONObject json = new JSONObject();
    String page = POSTFetcher
        .fetchPagePOSTWithHeaders("https://mid.webcontinental.com.br/webservice/pricesOfSkus.json", session, payload, cookies, 1, headers).trim();

    if (page.startsWith("{") && page.endsWith("}")) {
      json = new JSONObject(page);
    }

    if (json.has("response")) {
      JSONArray response = json.getJSONArray("response");

      for (int i = 0; i < response.length(); i++) {
        JSONObject temp = response.getJSONObject(0);

        if (temp.has("sku") && internalId.equals(temp.getString("sku"))) {
          product = temp;
          break;
        }
      }
    }

    return product;
  }

  private Marketplace crawlMarketplace(JSONObject sellerInfo) {
    Marketplace marketplace = new Marketplace();

    String sellerName = SELLER_NAME_LOWER;

    Float priceSeller = null;
    Float priceSeller1x = null;
    Float priceFrom = null;

    if (sellerInfo.has("vendidopor")) {
      sellerName = sellerInfo.get("vendidopor").toString().toLowerCase().trim();
    }

    if (sellerInfo.has("precoprazo")) {
      Object priceObj = sellerInfo.get("precoprazo");

      if (priceObj instanceof Double) {
        priceSeller = MathUtils.normalizeTwoDecimalPlaces(((Double) priceObj).floatValue());
      } else if (priceObj instanceof Integer) {
        priceSeller = MathUtils.normalizeTwoDecimalPlaces(((Integer) priceObj).floatValue());
      }
    }

    if (sellerInfo.has("precovista")) {
      Object priceObj = sellerInfo.get("precovista");

      if (priceObj instanceof Double) {
        priceSeller1x = MathUtils.normalizeTwoDecimalPlaces(((Double) priceObj).floatValue());
      } else if (priceObj instanceof Integer) {
        priceSeller1x = MathUtils.normalizeTwoDecimalPlaces(((Integer) priceObj).floatValue());
      }
    }

    if (sellerInfo.has("precode") && sellerInfo.get("precode") instanceof Double) {
      Double p = sellerInfo.getDouble("precode");
      priceFrom = MathUtils.normalizeTwoDecimalPlaces(p.floatValue());
    }

    if (!sellerName.equals(SELLER_NAME_LOWER) && !sellerName.isEmpty() && priceSeller != null) {
      JSONObject sellerJSON = new JSONObject();
      sellerJSON.put("name", sellerName);

      Prices prices = crawlPricesSeller(priceSeller, priceSeller1x, priceFrom);

      sellerJSON.put("price", priceSeller);
      sellerJSON.put("prices", prices.toJSON());

      try {
        Seller seller = new Seller(sellerJSON);
        marketplace.add(seller);
      } catch (Exception e) {
        Logging.printLogError(logger, session, Util.getStackTraceString(e));
      }
    }

    return marketplace;
  }

  private Prices crawlPricesSeller(Float price, Float price1x, Float priceFrom) {
    Prices p = new Prices();

    if (price != null || price1x != null) {
      Map<Integer, Float> installments = new HashMap<>();

      if (price1x != null) {
        p.setBankTicketPrice(price1x);
        installments.put(1, price1x);
      } else {
        p.setBankTicketPrice(price);
        installments.put(1, price);
      }

      if (priceFrom != null) {
        p.setPriceFrom(MathUtils.normalizeTwoDecimalPlaces(priceFrom.doubleValue()));
      }

      installments.put(10, MathUtils.normalizeTwoDecimalPlaces(price / 10f));

      p.insertCardInstallment(Card.AMEX.toString(), installments);
      p.insertCardInstallment(Card.VISA.toString(), installments);
      p.insertCardInstallment(Card.MASTERCARD.toString(), installments);
      p.insertCardInstallment(Card.DINERS.toString(), installments);
      p.insertCardInstallment(Card.HIPERCARD.toString(), installments);
      p.insertCardInstallment(Card.ELO.toString(), installments);
    }

    return p;
  }

  private Prices crawlPrices(JSONObject sku, Float price) {
    Prices p = new Prices();

    if (price != null && sku.has("prices")) {
      JSONObject pricesJson = sku.getJSONObject("prices");
      Map<Integer, Float> installments = new HashMap<>();

      installments.put(1, price);

      if (pricesJson.has("realaVista") && pricesJson.get("realaVista") instanceof Double) {
        Double boleto = pricesJson.getDouble("realaVista");
        p.setBankTicketPrice(boleto);
      } else {
        p.setBankTicketPrice(price);
      }

      installments.put(10, MathUtils.normalizeTwoDecimalPlaces(price / 10f));

      p.insertCardInstallment(Card.AMEX.toString(), installments);
      p.insertCardInstallment(Card.VISA.toString(), installments);
      p.insertCardInstallment(Card.MASTERCARD.toString(), installments);
      p.insertCardInstallment(Card.DINERS.toString(), installments);
      p.insertCardInstallment(Card.HIPERCARD.toString(), installments);
      p.insertCardInstallment(Card.ELO.toString(), installments);
    }

    return p;
  }

  /**
   * This json must be found in
   * https://www.webcontinental.com.br/ccstoreui/v1/pages/product?pageParam=26131&dataOnly=false&pageId=product&cacheableDataOnly=true
   * 
   * @param internalPid
   * @return
   */
  private JSONObject fetchJsonProduct(String internalPid) {
    if (internalPid != null) {
      String url = "https://www.webcontinental.com.br/ccstoreui/v1/pages/product?" + "pageParam=" + internalPid
          + "&dataOnly=false&pageId=product&cacheableDataOnly=true";

      JSONObject fetcherResponse = POSTFetcher.fetcherRequest(url, cookies, null, null, DataFetcher.GET_REQUEST, session, false);
      String page = null;

      if (fetcherResponse.has("response") && fetcherResponse.has("request_status_code") && fetcherResponse.getInt("request_status_code") >= 200
          && fetcherResponse.getInt("request_status_code") < 400) {
        JSONObject response = fetcherResponse.getJSONObject("response");

        if (response.has("body")) {
          page = response.get("body").toString();
        }
      } else {
        // normal request
        page = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);
      }

      try {
        JSONObject api = page != null ? new JSONObject(page) : new JSONObject();

        return sanityzeJsonAPI(api);
      } catch (JSONException e) {
        Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }
    }

    return new JSONObject();
  }

  private JSONObject sanityzeJsonAPI(JSONObject api) {
    JSONObject product = new JSONObject();

    if (api.has("data")) {
      JSONObject data = api.getJSONObject("data");

      if (data.has("page")) {
        JSONObject page = data.getJSONObject("page");

        if (page.has("product")) {
          JSONObject prod = page.getJSONObject("product");

          if (prod.has("displayName")) {
            product.put("name", prod.getString("displayName"));
          }

          computeImages(prod, product);
          computeDescription(prod, product);
          computeCategories(prod, product);
          computeSkus(prod, product);
        }
      }
    }

    return product;
  }

  private void computeSkus(JSONObject prod, JSONObject product) {
    JSONArray products = new JSONArray();

    if (prod.has("childSKUs")) {
      JSONArray skus = prod.getJSONArray("childSKUs");

      for (int i = 0; i < skus.length(); i++) {
        JSONObject child = new JSONObject();
        JSONObject sku = skus.getJSONObject(i);

        if (sku.has("repositoryId")) {
          child.put("skuId", sku.getString("repositoryId"));
        }

        if (sku.has("voltagem") && !sku.isNull("voltagem")) {
          child.put("skuName", sku.get("voltagem").toString().replace("[", "").replace("]", "").replace("\"", ""));
        }

        if (sku.has("salePrices")) {
          child.put("prices", sku.getJSONObject("salePrices"));
        }

        products.put(child);
      }
    }

    product.put("skus", products);
  }

  private void computeImages(JSONObject prod, JSONObject product) {
    if (prod.has("primaryFullImageURL")) {
      String image = prod.getString("primaryFullImageURL");

      if (image.startsWith(HOME_PAGE)) {
        product.put("primaryImage", image);
      } else {
        product.put("primaryImage", HOME_PAGE + image);
      }
    }

    if (prod.has("fullImageURLs")) {
      product.put("secondaryImages", prod.getJSONArray("fullImageURLs"));
    }
  }

  private void computeDescription(JSONObject prod, JSONObject product) {
    StringBuilder descriptions = new StringBuilder();

    // if (prod.has("mobileDescription") && prod.get("mobileDescription") instanceof String) {
    // descriptions.append(prod.getString("mobileDescription"));
    // }

    // if (prod.has("description") && prod.get("description") instanceof String) {
    // descriptions.append(prod.getString("description"));
    // }

    if (prod.has("longDescription") && prod.get("longDescription") instanceof String) {
      descriptions.append(prod.getString("longDescription"));
    }

    if (prod.has("caracteristicasTecnicas") && prod.get("caracteristicasTecnicas") instanceof String) {
      descriptions.append(prod.getString("caracteristicasTecnicas"));
    }

    product.put("description", descriptions.toString());
  }

  private void computeCategories(JSONObject prod, JSONObject product) {
    JSONArray categories = new JSONArray();

    if (prod.has("parentCategories")) {
      JSONArray cat = prod.getJSONArray("parentCategories");

      for (int i = 0; i < cat.length(); i++) {
        JSONObject obj = cat.getJSONObject(i);

        if (obj.has("displayName")) {
          categories.put(obj.getString("displayName"));
        }
      }
    }

    product.put("categories", categories);
  }
}
