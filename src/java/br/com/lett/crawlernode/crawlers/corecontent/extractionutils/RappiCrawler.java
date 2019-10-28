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
  private static final String DETAILS_API_URL = "https://services.rappi.com.br/api/cpgs-graphql-domain/";
  private static final String IMAGES_DOMAIN = "images.rappi.com.br/products";

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
    String productUrl = session.getOriginalURL();
    JSONObject jsonSku = crawlProductInformatioFromApi(productUrl, storeType);

    if (isProductPage(jsonSku)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(jsonSku);
      String internalPid = crawlInternalPid(jsonSku);
      String description = crawlDescription(jsonSku);
      boolean available = crawlAvailability(jsonSku);
      Float price = available ? crawlPrice(jsonSku) : null;
      Double priceFrom = available ? crawlPriceFrom(jsonSku) : null;
      String primaryImage = crawlPrimaryImage(jsonSku);
      String secondaryImages = crawlSecondaryImages(jsonSku, primaryImage);
      String name = crawlName(jsonSku);
      Prices prices = crawlPrices(price, priceFrom);
      Marketplace marketplace = crawlMarketplace(price, prices, jsonSku);
      List<String> eans = scrapEan(jsonSku);
      Offers offers = scrapBuyBox(jsonSku);

      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(productUrl)
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setPrices(prices)
          .setPrice(price)
          .setAvailable(available)
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setMarketplace(marketplace)
          .setEans(eans)
          .setOffers(offers)
          .build();

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
        mainPrice = CrawlerUtils.getDoubleValueFromJSON(jsonSku, "price", false, false);
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
    String ean = null;

    if (jsonSku.has("ean")) {
      ean = jsonSku.getString("ean");

      if (ean != null) {
        eans.add(ean);

      }
    }

    return eans;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(JSONObject jsonSku) {
    return jsonSku.length() > 0;
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

    if (json.has("image") && json.get("image") instanceof String) {
      primaryImage = CrawlerUtils.completeUrl(json.getString("image"), "https", IMAGES_DOMAIN);
    }

    return primaryImage;
  }
  
  private String crawlSecondaryImages(JSONObject json, String primaryImage) {
    JSONArray imagesArray = new JSONArray();

    if(json.has("store_id") && !json.isNull("store_id") && json.has("product_id") && !json.isNull("product_id")) {
      JSONArray jsonImagesArray = crawlProductImagesFromApi(json.get("product_id").toString(), json.get("store_id").toString());

      for(Object obj : jsonImagesArray) {
        if(obj instanceof JSONObject) {
          JSONObject imageObj = (JSONObject) obj;
          
          if(imageObj.has("name") && imageObj.get("name") instanceof String) {
            String secondaryImage = CrawlerUtils.completeUrl(imageObj.getString("name"), "https", IMAGES_DOMAIN);
            
            if(!secondaryImage.equals(primaryImage)) {
              imagesArray.put(CrawlerUtils.completeUrl(imageObj.getString("name"), "https", IMAGES_DOMAIN));
            }
          }
        }
      }
    }

    return imagesArray.toString();
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
      productId = CommonMethods.getLast(productUrl.split("\\?")[0].split("_"));
    }

    if (productId != null && storeType != null && storeId != null) {
      Map<String, String> headers = new HashMap<>();

      String url = "https://services.rappi.com.br/windu/products/store/" + storeId + "/product/" + productId;
      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).mustSendContentEncoding(false).build();

      String page = this.dataFetcher.get(session, request).getBody();

      if (page.startsWith("{") && page.endsWith("}")) {
        try {
          JSONObject apiResponse = new JSONObject(page);

          if (apiResponse.has("product") && apiResponse.get("product") instanceof JSONObject) {
            productsInfo = apiResponse.getJSONObject("product");
          }

        } catch (Exception e) {
          Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
        }
      }
    }

    return productsInfo;
  }
  
  private JSONArray crawlProductImagesFromApi(String productId, String storeId) {
    JSONArray productImages = new JSONArray();
    
    if (productId != null && storeId != null) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      
      long productIdNumber = Long.parseLong(productId);
      long storeIdNumber = Long.parseLong(storeId);
      
      JSONObject payload = new JSONObject();
      payload.put("operationName", "fetchProductDetails");
      payload.put("variables", new JSONObject().put("productId", productIdNumber).put("storeId", storeIdNumber));
      payload.put("query", "query fetchProductDetails($productId: Int!, $storeId: Int!) " +
          "{\n productDetail(productId: $productId, storeId: $storeId) {\n longDescription\n " +
          "images {\n name\n }\n toppings {\n id\n description\n toppingTypeId\n " + 
          "minToppingsForCategories\n maxToppingsForCategories\n index\n presentationTypeId\n " + 
          "topping {\n id\n description\n toppingCategoryId\n price\n maxLimit\n index\n }\n " + 
          "}\n nutritionFactsImg {\n name\n }\n additionalInformation {\n attribute\n value\n " + 
          "}\n ingredients {\n name\n }\n }\n}\n");

      Request request = RequestBuilder.create()
          .setUrl(DETAILS_API_URL)
          .setHeaders(headers)
          .mustSendContentEncoding(false)
          .setPayload(payload.toString())
          .build();
      
      String page = this.dataFetcher.post(session, request).getBody();

      if (page.startsWith("{") && page.endsWith("}")) {
        try {
          JSONObject apiResponse = new JSONObject(page);

          if (apiResponse.has("data") && apiResponse.get("data") instanceof JSONObject) {
            apiResponse = apiResponse.getJSONObject("data");
            
            if (apiResponse.has("productDetail") && apiResponse.get("productDetail") instanceof JSONObject) {
              apiResponse = apiResponse.getJSONObject("productDetail");
              
              if (apiResponse.has("images") && apiResponse.get("images") instanceof JSONArray) {
                productImages = apiResponse.getJSONArray("images");
              }
            }
          }

        } catch (Exception e) {
          Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
        }
      }
    }
    
    return productImages;
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
