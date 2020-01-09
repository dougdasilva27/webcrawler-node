package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import models.prices.Prices;

public class AdidasCrawler extends Crawler {
  private String homePage = "";

  public AdidasCrawler(Session session, String homePage) {
    super(session);
    this.homePage = homePage;
  }

  @Override
  protected Object fetch() {
    return Jsoup.parse(fetchApi(session.getOriginalURL()));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);

    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      String id = scrapId(doc);
      String apiUrl = homePage + "/api/products/" + id;

      JSONObject productJson = CrawlerUtils.stringToJson(fetchApi(apiUrl));
      JSONObject pricingInformation = productJson.has("pricing_information") ? productJson.getJSONObject("pricing_information") : new JSONObject();
      JSONObject available = CrawlerUtils.stringToJson(fetchApi(apiUrl + "/availability"));
      JSONArray variations = available.has("variation_list") ? available.getJSONArray("variation_list") : new JSONArray();
      String internalPid = scrapInternalPid(productJson);
      String ratingUrl = homePage + "/api/models/" + internalPid + "/ratings";

      JSONObject ratingJson = CrawlerUtils.stringToJson(fetchApi(ratingUrl));

      Float price = scrapPrice(pricingInformation);
      Prices prices = scrapPrices(pricingInformation, price);
      CategoryCollection categories = scrapCategories(productJson);
      String primaryImage = scrapPrimaryImage(productJson);
      String secondaryImages = scrapSecondaryImages(productJson, primaryImage);
      String description = scrapDescription(productJson);

      for (Object object : variations) {
        JSONObject variation = (JSONObject) object;
        String name = scrapName(productJson, variation);
        Integer stock = variation.has("availability") ? variation.getInt("availability") : null;
        boolean availability = scrapAvailability(variation);
        String internalId = scrapInternalId(variation);
        RatingsReviews ratingAndReviews = scrapRatingAndReviews(internalId, available, variation, ratingJson);

        // Creating the product
        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrice(price)
            .setPrices(prices)
            .setAvailable(availability)
            .setCategory1(categories.getCategory(0))
            .setRatingReviews(ratingAndReviews)
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setStock(stock)
            .build();

        products.add(product);
      }
    }

    return products;
  }

  private String scrapId(Document doc) {
    String internalId = null;
    Element metaElement = doc.selectFirst("meta[itemprop=\"sku\"]");

    if (metaElement != null) {
      internalId = metaElement.attr("content");
    }
    return internalId;
  }

  private String scrapDescription(JSONObject productJson) {
    StringBuilder description = new StringBuilder();
    JSONObject descriptionJson = productJson.has("product_description") ? productJson.getJSONObject("product_description") : new JSONObject();
    if (descriptionJson.has("title")) {
      description.append("<h1>");
      description.append(descriptionJson.getString("title"));
      description.append("</h1>");
    }

    if (descriptionJson.has("subtitle")) {
      description.append("<h2>");
      description.append(descriptionJson.getString("subtitle"));
      description.append("</h2>");
    }

    if (descriptionJson.has("text")) {
      description.append("<p>");
      description.append(descriptionJson.getString("text"));
      description.append("</p>");
    }

    return description.toString();
  }

  private String scrapSecondaryImages(JSONObject productJson, String primaryImage) {
    JSONArray imageJsonArray = productJson.has("view_list") ? productJson.getJSONArray("view_list") : new JSONArray();
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    for (Object object : imageJsonArray) {
      JSONObject image = (JSONObject) object;
      if (image.has("image_url") && !image.getString("image_url").equalsIgnoreCase(primaryImage) && !image.has("video_url")) {
        secondaryImagesArray.put(image.getString("image_url"));
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }
    return secondaryImages;
  }

  private String scrapPrimaryImage(JSONObject productJson) {
    JSONArray imageJsonArray = productJson.has("view_list") ? productJson.getJSONArray("view_list") : new JSONArray();
    JSONObject image = imageJsonArray.length() > 0 ? imageJsonArray.getJSONObject(0) : new JSONObject();

    return image.has("image_url") ? image.getString("image_url") : null;
  }

  private CategoryCollection scrapCategories(JSONObject productJson) {
    CategoryCollection categories = new CategoryCollection();
    JSONArray categoriesJsonArray = productJson.getJSONArray("breadcrumb_list");

    for (Object object : categoriesJsonArray) {
      JSONObject categorie = (JSONObject) object;
      categories.add(categorie.has("text") ? categorie.getString("text") : null);
    }

    return categories;
  }

  private boolean scrapAvailability(JSONObject available) {
    return available.has("availability_status") && available.getInt("availability") > 0;
  }

  private Float scrapPrice(JSONObject pricingInformation) {
    return pricingInformation.has("currentPrice") ? MathUtils
        .parseFloatWithDots(pricingInformation.get("currentPrice").toString()) : null;
  }

  private Prices scrapPrices(JSONObject pricingInformation, Float price) {
    Prices prices = new Prices();
    prices.setPriceFrom(pricingInformation.has("standard_price") ? CrawlerUtils
        .getDoubleValueFromJSON(pricingInformation, "standard_price", true, null) : null);

    Map<Integer, Float> installmentPriceMap = new HashMap<>();
    installmentPriceMap.put(1, price);

    prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

    return prices;
  }

  private String scrapInternalPid(JSONObject productJson) {
    return productJson.has("model_number") ? productJson.getString("model_number") : null;
  }

  private String scrapName(JSONObject jsonSku, JSONObject variation) {
    StringBuilder name = new StringBuilder();

    if (jsonSku.has("name")) {
      name.append(jsonSku.getString("name"));
    }

    if (jsonSku.has("attribute_list")) {
      JSONObject attributeList = jsonSku.getJSONObject("attribute_list");
      if (attributeList.has("color")) {
        name.append(" ");
        name.append(attributeList.getString("color"));
      }
    }

    if (variation.has("size")) {
      name.append(" ");
      name.append(variation.get("size"));
    }

    return name.toString();
  }

  private String scrapInternalId(JSONObject variation) {
    return variation.has("sku") ? variation.getString("sku") : null;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".pdpBar > div[data-auto-id=\"product-information\"]") != null;
  }

  private String fetchApi(String url) {
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "text/html,application/xhtmlxml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
    headers.put("cache-control", "max-age=0");
    headers.put("upgrade-insecure-requests", "1");
    headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
    return new JavanetDataFetcher().get(session, request).getBody();
  }

  protected RatingsReviews scrapRatingAndReviews(String internalId, JSONObject available, JSONObject variation, JSONObject ratingJson) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());

    Integer totalNumOfEvaluations = getTotalNumOfRatings(ratingJson);
    Double avgRating = getTotalAvgRating(ratingJson);

    Map<Integer, Integer> stars = getStars(ratingJson);
    AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();

    advancedRatingReview.setTotalStar1(stars.get(1));
    advancedRatingReview.setTotalStar2(stars.get(2));
    advancedRatingReview.setTotalStar3(stars.get(3));
    advancedRatingReview.setTotalStar4(stars.get(4));
    advancedRatingReview.setTotalStar5(stars.get(5));

    ratingReviews.setAdvancedRatingReview(advancedRatingReview);
    ratingReviews.setInternalId(internalId);
    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);

    return ratingReviews;

  }

  private Map<Integer, Integer> getStars(JSONObject ratingJson) {
    JSONArray jsonArray = ratingJson.optJSONArray("ratingDistribution");
    Map<Integer, Integer> stars = new HashMap<>();
    for (Object object : jsonArray) {
      JSONObject jsonStars = (JSONObject) object;
      int star = jsonStars.optInt("rating");
      int count = jsonStars.optInt("count");
      stars.put(star, count);
    }
    return stars;
  }

  private Double getTotalAvgRating(JSONObject ratingJson) {
    Double avg = 0d;
    if (ratingJson.opt("overallRating") instanceof Double) {
      avg = MathUtils.parseDoubleWithDot(ratingJson.opt("overallRating").toString());
    }
    return avg;
  }

  private Integer getTotalNumOfRatings(JSONObject ratingJson) {
    Integer total = 0;
    if (ratingJson.opt("reviewCount") instanceof Integer) {
      total = ratingJson.optInt("reviewCount");
    }
    return total;
  }
}
