package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class FalabellaCrawlerUtils extends Crawler {

  protected static final String IMAGE_URL_FIRST_PART = "https://falabella.scene7.com/is/image/";

  // OBS: Change for each city (default: Chile)
  private String HOME_PAGE = "https://www.falabella.com";
  private String IMAGE_URL_CITY = "Falabella/";
  private boolean HAS_CENTS = false;
  private String API_KEY = "mk9fosfh4vxv20y8u5pcbwipl";

  protected void setApiKey(String apiKey) {
    this.API_KEY = apiKey;
  }

  public FalabellaCrawlerUtils(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }

  /**
   * Method to be called from child to set specific home page URL.
   * 
   * @param homePage
   */
  protected void setHomePage(String homePage) {
    this.HOME_PAGE = homePage;
  }

  /**
   * Method to be called from child to set specific image URL.
   * 
   * @param imageUrl
   */
  protected void setImageUrl(String imageUrl) {
    this.IMAGE_URL_CITY = imageUrl;
  }

  /**
   * Method to be called from child to set cents calculation if the market has it. <br>
   * default: false
   * 
   * @param hasCents
   */
  protected void setCurrencyHasCents(boolean hasCents) {
    this.HAS_CENTS = hasCents;
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
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject productJson = extractProductJson(doc);

      CategoryCollection categories = crawlCategories(doc);
      String description = crawlDescription(doc);


      if (productJson.length() > 0) {
        JSONArray arraySkus = productJson.has("skus") ? productJson.getJSONArray("skus") : new JSONArray();
        String internalPid = crawlInternalPid(productJson);
        Map<String, List<String>> colorsMap = new HashMap<>();

        for (int i = 0; i < arraySkus.length(); i++) {
          JSONObject skuJson = arraySkus.getJSONObject(i);

          String internalId = crawlInternalId(skuJson);
          String name = crawlName(productJson, skuJson);
          Integer stock = crawlStock(skuJson);
          boolean available = stock != null && stock > 0;

          JSONObject pricesJson = fetchPrices(internalId, available);
          Prices prices = available ? crawlPrices(skuJson, pricesJson) : new Prices();
          Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.AMEX);

          List<String> images = crawlImages(skuJson, internalId, colorsMap);
          String primaryImage = images.isEmpty() ? null : images.get(0);
          String secondaryImages = crawlSecondaryImages(images, primaryImage);

          RatingsReviews clonedRatingReviews = crawlRating(internalPid).clone();
          clonedRatingReviews.setInternalId(crawlInternalId(arraySkus.getJSONObject(i)));
          ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
          RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(internalId);

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
              .setStock(stock)
              .setMarketplace(new Marketplace())
              .setRatingReviews(ratingReviews)
              .build();

          products.add(product);
        }
      } else {
        String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".fb-product-cta__title", true);
        String internalId = crawlInternalId(doc);
        String internalPid = internalId;
        String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".fb-pp-photo img", Arrays.asList("src"), "https:", "falabella.scene7.com");

        RatingsReviews ratingReviewsScraped = crawlRating(internalId);
        ratingReviewsScraped.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(ratingReviewsScraped);
        RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(internalId);

        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrice(null)
            .setPrices(new Prices())
            .setAvailable(false)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setMarketplace(new Marketplace())
            .setRatingReviews(ratingReviews)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private RatingsReviews crawlRating(String id) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());

    String endpointRequest = assembleBazaarVoiceEndpointRequest(id, 0, 50);

    Request request = RequestBuilder.create().setUrl(endpointRequest).setCookies(cookies).build();
    JSONObject ratingReviewsEndpointResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
    JSONObject reviewStatistics = getReviewStatisticsJSON(ratingReviewsEndpointResponse, id);

    Integer totalNumOfEvaluations = getTotalReviewCount(reviewStatistics);
    Double avgRating = getAverageOverallRating(reviewStatistics);

    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

    return ratingReviews;
  }

  private Integer getTotalReviewCount(JSONObject reviewStatistics) {
    Integer totalReviewCount = 0;
    if (reviewStatistics.has("TotalReviewCount")) {
      totalReviewCount = reviewStatistics.getInt("TotalReviewCount");
    }
    return totalReviewCount;
  }

  private Double getAverageOverallRating(JSONObject reviewStatistics) {
    Double avgOverallRating = 0d;
    if (reviewStatistics.has("AverageOverallRating")) {
      avgOverallRating = reviewStatistics.getDouble("AverageOverallRating");
    }
    return avgOverallRating;
  }


  /**
   * e.g: http://api.bazaarvoice.com/data/reviews.json?apiversion=5.4
   * &passkey=oqu6lchjs2mb5jp55bl55ov0d &Offset=0 &Limit=5 &Sort=SubmissionTime:desc
   * &Filter=ProductId:113048617 &Include=Products &Stats=Reviews
   * 
   * Endpoint request parameters:
   * <p>
   * &passKey: the password used to request the bazaar voice endpoint. This pass key e crawled inside
   * the html of the sku page, inside a script tag. More details on how to crawl this passKey
   * </p>
   * <p>
   * &Offset: the number of the chunk of data retrieved by the endpoint. If we want the second chunk,
   * we must add this value by the &Limit parameter.
   * </p>
   * <p>
   * &Limit: the number of reviews that a request will return, at maximum.
   * </p>
   * 
   * The others parameters we left as default.
   * 
   * Request Method: GET
   */
  private String assembleBazaarVoiceEndpointRequest(String skuInternalPid, Integer offset, Integer limit) {

    StringBuilder request = new StringBuilder();

    request.append("http://api.bazaarvoice.com/data/reviews.json?apiversion=5.4");
    request.append("&passkey=" + this.API_KEY);
    request.append("&Offset=" + offset);
    request.append("&Limit=" + limit);
    request.append("&Sort=SubmissionTime:desc");
    request.append("&Filter=ProductId:" + skuInternalPid);
    request.append("&Include=Products");
    request.append("&Stats=Reviews");

    return request.toString();
  }


  private JSONObject getReviewStatisticsJSON(JSONObject ratingReviewsEndpointResponse, String skuInternalPid) {
    if (ratingReviewsEndpointResponse.has("Includes")) {
      JSONObject includes = ratingReviewsEndpointResponse.getJSONObject("Includes");

      if (includes.has("Products")) {
        JSONObject products = includes.getJSONObject("Products");

        if (products.has(skuInternalPid)) {
          JSONObject product = products.getJSONObject(skuInternalPid);

          if (product.has("ReviewStatistics")) {
            return product.getJSONObject("ReviewStatistics");
          }
        }
      }
    }

    return new JSONObject();
  }

  /**
   * 
   * @param doc
   * @return
   */
  protected boolean isProductPage(Document doc) {
    return !doc.select(".fb-product-cta").isEmpty();
  }

  protected String crawlInternalId(Document doc) {
    String internalId = null;

    String text = CrawlerUtils.scrapStringSimpleInfo(doc, ".fb-product-sets__product-code", true);
    if (text != null) {
      internalId = CommonMethods.getLast(text.split(":"));
    }

    return internalId;
  }

  /**
   * 
   * @param doc
   * @return
   */
  protected JSONObject extractProductJson(Document doc) {
    JSONObject productJson = new JSONObject();

    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "var fbra_browseMainProductConfig =", "};", false, false);

    if (json.has("state")) {
      JSONObject state = json.getJSONObject("state");

      if (state.has("product")) {
        productJson = state.getJSONObject("product");
      }
    }

    return productJson;
  }

  protected CategoryCollection crawlCategories(Document doc) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = doc.select(".fb-masthead__breadcrumb__links > span a");

    for (Element e : elementCategories) {
      categories.add(e.text().replace("/", "").trim());
    }

    return categories;
  }

  /**
   * @param skuJson
   * @return
   */
  protected Integer crawlStock(JSONObject skuJson) {
    Integer stock = null;

    if (skuJson.has("onlineStock")) {
      stock = skuJson.getInt("onlineStock");
    }

    return stock;
  }

  protected String crawlInternalId(JSONObject skuJson) {
    String internalId = null;

    if (skuJson.has("skuId")) {
      internalId = skuJson.getString("skuId");
    }

    return internalId;
  }


  protected String crawlInternalPid(JSONObject productJson) {
    String internalPid = null;

    if (productJson.has("id")) {
      internalPid = productJson.get("id").toString();
    }

    return internalPid;
  }

  protected String crawlName(JSONObject productJson, JSONObject skuJson) {
    StringBuilder name = new StringBuilder();

    if (productJson.has("displayName")) {
      name.append(productJson.get("displayName"));

      if (skuJson.has("size")) {
        name.append(" ").append(skuJson.get("size"));
      }

      if (skuJson.has("color")) {
        name.append(" ").append(skuJson.get("color"));
      }
    }

    return name.toString();
  }

  protected String crawlSecondaryImages(List<String> images, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    for (String image : images) {
      if (!image.equalsIgnoreCase(primaryImage)) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  protected List<String> crawlImages(JSONObject skuJson, String internalId, Map<String, List<String>> colorsMap) {
    List<String> images = new ArrayList<>();

    String colorName = "normal";

    if (skuJson.has("color")) {
      colorName = skuJson.get("color").toString();
    }

    if (colorsMap.containsKey(colorName)) {
      images = colorsMap.get(colorName);
    } else {
      String url = IMAGE_URL_FIRST_PART + IMAGE_URL_CITY + internalId + "?req=set,json";

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      String response = dataFetcher.get(session, request).getBody();

      JSONObject json = CrawlerUtils.stringToJson(CrawlerUtils.extractSpecificStringFromScript(response, "esponse(", ",", true));

      if (json.has("set")) {
        JSONObject set = json.getJSONObject("set");

        if (set.has("item")) {
          JSONArray items = new JSONArray();

          if (set.get("item") instanceof JSONArray) {
            items = set.getJSONArray("item");
          } else if (set.get("item") instanceof JSONObject) {
            items.put(set.get("item"));
          }

          for (Object o : items) {
            JSONObject item = (JSONObject) o;
            JSONObject imageJson = new JSONObject();

            if (item.has("s")) {
              imageJson = item.getJSONObject("s");
            } else if (item.has("i")) {
              imageJson = item.getJSONObject("i");
            }

            if (imageJson.has("n")) {
              images.add(IMAGE_URL_FIRST_PART + imageJson.get("n") + "?wid=1080&fmt=jpg");
            }
          }

          colorsMap.put(colorName, images);
        }
      }
    }

    return images;
  }

  protected String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Elements descriptions = doc.select("[data-panel=longDescription]");
    for (Element e : descriptions) {
      description.append(e.html());
    }

    return description.toString();
  }

  /**
   * Method to crawl prices that should be overriden on each market. <br>
   * default: Chile
   * 
   * @param skuJson
   * @param jsonPrices
   * @return
   */
  protected Prices crawlPrices(JSONObject skuJson, JSONObject jsonPrices) {
    Prices prices = new Prices();

    if (skuJson.has("price")) {
      Map<Integer, Float> mapInstallments = new HashMap<>();
      JSONArray arrayPrices = skuJson.getJSONArray("price");

      for (Object o : arrayPrices) {
        JSONObject priceJson = (JSONObject) o;

        if (priceJson.has("originalPrice")) {
          Float price;

          if (HAS_CENTS) {
            price = MathUtils.parseFloatWithDots(priceJson.get("originalPrice").toString());
          } else {
            price = MathUtils.parseFloatWithComma(priceJson.get("originalPrice").toString());
          }

          if (price != null) {
            if (priceJson.has("type")) {
              int type = priceJson.getInt("type");

              if (type == 1) {
                Map<Integer, Float> temp = new HashMap<>();
                temp.put(1, price);
                prices.insertCardInstallment(Card.SHOP_CARD.toString(), temp);
              }

              if (type == 3) {
                mapInstallments.put(1, price);
              }

              if (type == 2) {
                prices.setPriceFrom(MathUtils.normalizeTwoDecimalPlaces(price.doubleValue()));
              }
            }
          }
        }
      }

      if (mapInstallments.isEmpty() && prices.getPriceFrom() != null) {
        mapInstallments.put(1, MathUtils.normalizeTwoDecimalPlaces(prices.getPriceFrom().floatValue()));
        prices.setPriceFrom(null);
      }


      if (jsonPrices.has("selectedInstallment") && jsonPrices.has("formattedInstallmentAmount")) {
        String installment = jsonPrices.get("selectedInstallment").toString().replaceAll("[^0-9]", "");
        Float value;

        if (HAS_CENTS) {
          value = MathUtils.parseFloatWithDots(jsonPrices.get("formattedInstallmentAmount").toString());
        } else {
          value = MathUtils.parseFloatWithComma(jsonPrices.get("formattedInstallmentAmount").toString());
        }

        if (!installment.isEmpty() && value != null) {
          mapInstallments.put(Integer.parseInt(installment), value);
        }
      }

      prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);
    }

    return prices;
  }

  public JSONObject fetchPrices(String internalId, boolean available) {
    JSONObject jsonPrice = new JSONObject();

    if (available) {
      String url = HOME_PAGE + "rest/model/falabella/rest/browse/BrowseActor/init-monthly-installment?" + "%7B%22skus%22%3A%5B%7B%22skuId%22%3A%22"
          + internalId + "%22%2C%22quantity%22%3A1%7D%5D%2C%22installmentNum%22%3A10%7D";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
      JSONObject json = CrawlerUtils.stringToJson(new FetcherDataFetcher().get(session, request).getBody());

      if (json.has("state")) {
        jsonPrice = json.getJSONObject("state");
      }
    }

    return jsonPrice;
  }
}
