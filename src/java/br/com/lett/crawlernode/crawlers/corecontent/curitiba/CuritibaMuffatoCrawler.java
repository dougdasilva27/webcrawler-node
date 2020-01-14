package br.com.lett.crawlernode.crawlers.corecontent.curitiba;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (11/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 * 
 * 2) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 3) The sku page identification is done simply looking the URL format combined with some html
 * element.
 * 
 * 4) Availability is crawled from the sku json extracted from a script in the html.
 * 
 * 5) InternalPid is equals internalId for this market.
 * 
 * 6) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples: ex1 (available):
 * http://delivery.supermuffato.com.br/leite-em-po-nestle-nan-soy-400g-97756/p?sc=10
 *
 * Optimizations notes: No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class CuritibaMuffatoCrawler extends Crawler {

  private final String HOME_PAGE = "http://delivery.supermuffato.com.br/";

  public CuritibaMuffatoCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);

  }

  @Override
  public String handleURLBeforeFetch(String curURL) {

    if (curURL.split("\\?")[0].endsWith("/p")) {

      try {
        String url = curURL;
        List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");
        List<NameValuePair> paramsNew = new ArrayList<>();

        for (NameValuePair param : paramsOriginal) {
          if (!param.getName().equals("sc")) {
            paramsNew.add(param);
          }
        }

        paramsNew.add(new BasicNameValuePair("sc", "13"));
        URIBuilder builder = new URIBuilder(curURL.split("\\?")[0]);

        builder.clearParameters();
        builder.setParameters(paramsNew);

        curURL = builder.build().toString();

        return curURL;

      } catch (URISyntaxException e) {
        return curURL;
      }
    }

    return curURL;

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
      Logging.printLogDebug(logger, "Product page identified: " + this.session.getOriginalURL());

      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, "supermuffato", HOME_PAGE, cookies, dataFetcher);

      // InternalId
      String internalId = crawlInternalId(doc);

      // InternalPid
      String internalPid = crawlInternalPid(doc);

      // Name
      String name = crawlName(doc);

      // Price
      Float price = crawlMainPagePrice(doc);

      // Categorias
      ArrayList<String> categories = crawlCategories(doc);
      String category1 = getCategory(categories, 0);
      String category2 = getCategory(categories, 1);
      String category3 = getCategory(categories, 2);

      // Sku json from script
      JSONObject skuJson = crawlSkuJson(doc);

      boolean available = crawlAvailability(skuJson);
      JSONObject apiJSON = vtexUtil.crawlApi(internalId);
      String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
      String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
      String description = crawlDescription(doc, internalId);
      Integer stock = null;
      Marketplace marketplace = new Marketplace();
      Prices prices = crawlPrices(doc, price);
      RatingsReviews ratingReviews = scrapRatingAndReviews(doc, internalId);

      // ean data in html
      JSONArray arrayEan = CrawlerUtils.scrapEanFromVTEX(doc);
      String ean = 0 < arrayEan.length() ? arrayEan.getString(0) : null;

      List<String> eans = new ArrayList<>();
      eans.add(ean);

      // create the product
      Product product = new Product();
      product.setUrl(session.getOriginalURL());
      product.setInternalId(internalId);
      product.setInternalPid(internalPid);
      product.setName(name);
      product.setPrice(price);
      product.setPrices(prices);
      product.setCategory1(category1);
      product.setCategory2(category2);
      product.setCategory3(category3);
      product.setRatingReviews(ratingReviews);
      product.setPrimaryImage(primaryImage);
      product.setSecondaryImages(secondaryImages);
      product.setDescription(description);
      product.setStock(stock);
      product.setMarketplace(marketplace);
      product.setAvailable(available);
      product.setEans(eans);

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page.");
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
    return document.select(".container.prd-info-container").first() != null;
  }


  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(Document document) {
    String internalId = null;
    Element elementInternalID = document.select(".prd-references .prd-code .skuReference").first();
    if (elementInternalID != null) {
      internalId = elementInternalID.text();
    }

    return internalId;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Element elementInternalID = document.select(".prd-references .prd-code .skuReference").first();
    if (elementInternalID != null) {
      internalPid = elementInternalID.text();
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".fn.productName").first();

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private Float crawlMainPagePrice(Document document) {
    Float price = null;
    Element elementPrice = document.select(".plugin-preco .preco-a-vista .skuPrice").first();
    if (elementPrice != null) {
      price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject skuJson) {
    if (skuJson != null && skuJson.has("available")) {
      return skuJson.getBoolean("available");
    }
    return false;
  }

  private ArrayList<String> crawlCategories(Document document) {
    ArrayList<String> categories = new ArrayList<>();
    Elements elementCategories = document.select(".breadcrumb-holder .container .row .bread-crumb ul li a");

    for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first
                                                         // is the market name
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

  private String crawlDescription(Document document, String internalId) {
    StringBuilder description = new StringBuilder();
    Element elementDescription = document.select("#prd-description #prd-accordion-c-one").first();
    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    Element specificDescription = document.selectFirst("#caracteristicas");

    if (specificDescription != null) {
      description.append(specificDescription.html());
    }

    description.append(CrawlerUtils.scrapLettHtml(internalId, session, session.getMarket().getNumber()));

    return description.toString();
  }

  /**
   * No bank slip payment method in this ecommerce.
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      Element priceFrom = doc.select(".skuListPrice").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
      }

      Element installmentElement = doc.select(".skuBestInstallmentNumber").first();

      if (installmentElement != null) {
        Integer installment = Integer.parseInt(installmentElement.text());

        Element valueElement = doc.select(".skuBestInstallmentValue").first();

        if (valueElement != null) {
          Float value = MathUtils.parseFloatWithComma(valueElement.text());

          installmentPriceMap.put(installment, value);
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
    }

    return prices;
  }

  /**
   * Get the script having a json variable with the image in it
   * 
   * @return
   */
  private JSONObject crawlSkuJson(Document document) {
    Elements scriptTags = document.getElementsByTag("script");
    JSONObject skuJson = null;

    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith("var skuJson_0 = ")) {

          skuJson = new JSONObject(node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1]
              + node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]);

        }
      }
    }

    return skuJson;
  }

  private JSONObject getRating(Document doc, String internalId) {
    JSONObject ratingJson = new JSONObject();
    String idWebsite = getIdWebsite(doc);
    JSONObject response = CrawlerUtils.stringToJson(sendRequestToAPI(internalId, "rating", idWebsite));

    if (response.optJSONArray(internalId) instanceof JSONArray) {
      JSONArray rate = response.getJSONArray(internalId);

      if (rate.length() > 0) {
        ratingJson = rate.getJSONObject(0);
      }
    }

    return ratingJson;
  }

  /**
   * 
   * @param doc
   * @param internalId
   * @return json representing an AdvancedRatingReview object as provided in its documentation.
   */
  private JSONObject getReview(Document doc, String internalId) {
    JSONObject ratingJson = new JSONObject();
    String idWebsite = getIdWebsite(doc);
    JSONArray response = CrawlerUtils.stringToJsonArray(sendRequestToAPI(internalId, "reviews", idWebsite));
    if (response.optJSONObject(0) instanceof JSONObject) {
      JSONObject jsonReviews = response.optJSONObject(0);
      if (jsonReviews.optJSONArray("stats") instanceof JSONArray) {
        JSONArray starts = jsonReviews.optJSONArray("stats");
        ratingJson.put(AdvancedRatingReview.RATING_STAR_1_FIELD, starts.get(0));
        ratingJson.put(AdvancedRatingReview.RATING_STAR_2_FIELD, starts.get(1));
        ratingJson.put(AdvancedRatingReview.RATING_STAR_3_FIELD, starts.get(2));
        ratingJson.put(AdvancedRatingReview.RATING_STAR_4_FIELD, starts.get(3));
        ratingJson.put(AdvancedRatingReview.RATING_STAR_5_FIELD, starts.get(4));
      }
    }

    return ratingJson;
  }

  /**
   * 
   * @param internalId
   * @param type can be only "rating" or "reviews"
   * @param idWebsite
   * @return
   */
  private String sendRequestToAPI(String internalId, String type, String idWebsite) {
    String apiUrl = "https://awsapis3.netreviews.eu/product";
    String payload =
        "{\"query\":\"" + type + "\",\"products\":\"" + internalId + "\",\"idWebsite\":\"" + idWebsite + "\",\"plateforme\":\"br\"}";
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json; charset=UTF-8");
    Request request =
        RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).setHeaders(headers).setPayload(payload).mustSendContentEncoding(false).build();
    return new FetcherDataFetcher().post(session, request).getBody();
  }

  private String getIdWebsite(Document doc) {
    // Filtra os elementos script pela url correta e atributo.

    Optional<Element> optionalUrlToken = doc.select("body > script").stream()
        .filter(x -> (x.hasAttr("src") &&
            (x.attr("src").startsWith("https://cl.avis-verifies.com"))))
        .findFirst();

    String attr = optionalUrlToken.get().attr("src");

    String[] strings = attr.substring(attr.indexOf("br/")).split("/");

    return strings[strings.length - 4];
  }

  protected RatingsReviews scrapRatingAndReviews(Document doc, String internalId) {
    RatingsReviews ratingReviews = new RatingsReviews();
    JSONObject rating = getRating(doc, internalId);
    Integer totalReviews = CrawlerUtils.getIntegerValueFromJSON(rating, "count", 0);
    Double avgRating = CrawlerUtils.getDoubleValueFromJSON(rating, "rate", true, false);
    AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview(getReview(doc, internalId));
    ratingReviews.setAdvancedRatingReview(advancedRatingReview);
    ratingReviews.setDate(session.getDate());
    ratingReviews.setTotalRating(totalReviews);
    ratingReviews.setTotalWrittenReviews(totalReviews);
    ratingReviews.setAverageOverallRating(avgRating == null ? 0d : avgRating);
    ratingReviews.setInternalId(internalId);

    return ratingReviews;

  }
}
