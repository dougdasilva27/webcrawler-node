package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class RappiCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.rappi.com";
  private static final String STORES_API_URL = "https://services.rappi.com.br/api/base-crack/principal?lat=-23.584&lng=-46.671&device=2";
  public static final String PRODUCTS_API_URL = "https://services.rappi.com.br/api/search-client/search/v2/products";

  private final String storeType;

  public RappiCrawler(Session session, String storeType) {
    super(session);
    this.storeType = storeType;
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected Object fetch() {
    return new Document("");
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String productUrl = session.getOriginalURL();
      JSONObject jsonSku = crawlProductInformatioFromApi(productUrl, storeType);
      String internalId = crawlInternalId(jsonSku);
      String internalPid = crawlInternalPid(jsonSku);
      String description = crawlDescription(jsonSku);
      boolean available = crawlAvailability(jsonSku);
      Float price = available ? crawlPrice(jsonSku) : null;
      Double priceFrom = available ? crawlPriceFrom(jsonSku) : null;
      String primaryImage = crawlPrimaryImage(jsonSku);
      String name = crawlName(jsonSku);
      Prices prices = crawlPrices(price, priceFrom);
      Marketplace marketplace = crawlMarketplace(price, prices, jsonSku);
      List<String> eans = scrapEan(jsonSku);
      Offers offers = scrapBuyBox(jsonSku);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(productUrl).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrices(prices).setPrice(price).setAvailable(available).setPrimaryImage(primaryImage).setDescription(description)
          .setMarketplace(marketplace).setEans(eans).setOffers(offers).build();

      products.add(product);


    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private Offers scrapBuyBox(JSONObject jsonSku) {
    Offers offers = new Offers();
    try {
      String sellerFullName = null;
      String slugSellerName = null;
      String internalSellerId = null;
      Double mainPrice = null;

      if (jsonSku.has("store_type")) {
        sellerFullName = jsonSku.getString("store_type");
        slugSellerName = CrawlerUtils.toSlug(sellerFullName);
      }

      if (jsonSku.has("store_id")) {
        internalSellerId = jsonSku.get("store_id").toString();
      }

      if (jsonSku.has("price")) {
        mainPrice = jsonSku.getDouble("price");
      }

      Offer offer = new OfferBuilder().setSellerFullName(sellerFullName).setSlugSellerName(slugSellerName).setInternalSellerId(internalSellerId)
          .setMainPagePosition(1).setIsBuybox(false).setMainPrice(mainPrice).build();

      offers.add(offer);

    } catch (OfferException e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }

    return offers;
  }

  private List<String> scrapEan(JSONObject jsonSku) {
    List<String> eans = new ArrayList<>();

    if (jsonSku.has("ean")) {
      eans.add(jsonSku.getString("ean"));
    }

    return eans;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url) {
    return (url.contains("store_type") && url.contains("query")) || url.contains("product/");
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

    if (json.has("product_id")) {
      internalPid = json.getString("product_id");
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

    if (json.has("real_price")) {
      Object pObj = json.get("real_price");

      if (pObj instanceof Double) {
        price = (Double) pObj;
      }
    }

    return price;
  }

  private Float crawlPrice(JSONObject json) {
    Float price = null;

    if (json.has("price")) {
      String pObj = json.get("price").toString();
      price = MathUtils.parseFloatWithDots(pObj);
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject json) {
    return json.has("available") && json.getBoolean("available");
  }

  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("image")) {
      primaryImage = "https://images.rappi.com.br/products/" + json.get("image");
    }

    return primaryImage;
  }


  private String crawlDescription(JSONObject json) {
    StringBuilder description = new StringBuilder();

    if (json.has("description") && json.get("description") instanceof String) {
      description.append(json.getString("description"));
    }


    return description.toString();
  }

  private Marketplace crawlMarketplace(Float price, Prices prices, JSONObject jsonSku) {
    Marketplace marketplace = new Marketplace();

    if (price != null && jsonSku.has("store_name")) {
      String sellerName = jsonSku.get("store_name").toString().replace("null", "").trim();

      if (!sellerName.isEmpty()) {

        try {
          Seller seller = new Seller(new JSONObject().put("price", price).put("prices", prices.toJSON()).put("name", sellerName));
          marketplace.add(seller);
        } catch (Exception e) {
          Logging.printLogError(logger, session, Util.getStackTraceString(e));
        }
      }
    }

    return marketplace;
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
   * - Get the json of api, this api has all info of product - Spected url like this
   * https://www.rappi.com.br/search?store_type=market&query=2089952206
   * 
   * @param storeType2
   * 
   * @return
   */
  private JSONObject crawlProductInformatioFromApi(String productUrl, String storeType) {
    JSONObject productsInfo = new JSONObject();
    Map<String, String> stores = crawlStores();

    String storeId = stores.containsKey(storeType) ? stores.get(storeType) : null;
    String productId = null;

    if (productUrl.contains("_")) {
      productId = CommonMethods.getLast(productUrl.split("_"));
    }

    if (productId != null && storeType != null && storeId != null) {

      // String payload = "{\"query\":\"" + productId + "\",\"stores\":[\"" + storeId + "\"]," +
      // "\"helpers\":{\"type\":\"by_products\",\"storeType\":\""
      // + storeType + "\"},\"page\":1,\"store_type\":\"" + storeType + "\",\"options\":{}}";

      Map<String, String> headers = new HashMap<>();

      String url = "https://services.rappi.com.br/windu/products/store/" + storeId + "/product/" + productId;

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).mustSendContentEncoding(false).build();

      String page = this.dataFetcher.get(session, request).getBody();

      if (page.startsWith("{") && page.endsWith("}")) {
        try {
          JSONObject apiResponse = new JSONObject(page);

          if (apiResponse.has("product") && apiResponse.get("product") instanceof JSONObject) {
            productsInfo = apiResponse.getJSONObject("product");
            System.err.println(productsInfo);
          }
        } catch (Exception e) {
          Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
        }
      }
    }

    return productsInfo;
  }

  private Map<String, String> crawlStores() {
    Map<String, String> stores = new HashMap<>();
    Request request = RequestBuilder.create().setUrl(STORES_API_URL).setCookies(cookies).build();
    JSONArray options = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

    for (Object o : options) {
      JSONObject option = (JSONObject) o;

      if (option.has("suboptions")) {
        JSONArray suboptions = option.getJSONArray("suboptions");

        for (Object ob : suboptions) {
          JSONObject suboption = (JSONObject) ob;
          if (suboption.has("stores")) {
            setStores(suboption.getJSONArray("stores"), stores);
          }
        }
      } else if (option.has("stores")) {
        setStores(option.getJSONArray("stores"), stores);
      }
    }

    return stores;
  }

  private void setStores(JSONArray storesArray, Map<String, String> stores) {
    for (Object o : storesArray) {
      JSONObject store = (JSONObject) o;

      if (store.has("store_id") && store.has("store_type")) {
        stores.put(store.getString("store_type"), store.getString("store_id"));
      }
    }
  }
}
