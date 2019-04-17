package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

/**
 * Date: 13/12/16
 * 
 * @author gabriel
 *
 */
public class BrasilBifarmaRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilBifarmaRatingReviewCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.WEBDRIVER);
  }

  @Override
  protected Document fetch() {
    this.webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), session);
    Document doc = Jsoup.parse(this.webdriver.getCurrentPageSource());

    Element script = doc.select("head script").last();
    Element robots = doc.select("meta[name=robots]").first();

    if (script != null && robots != null) {
      String eval = script.html().trim();

      if (!eval.isEmpty()) {
        Logging.printLogDebug(logger, session, "Execution of incapsula js script...");
        this.webdriver.executeJavascript(eval);
      }
    }

    String requestHash = FetchUtilities.generateRequestHash(session);
    this.webdriver.waitLoad(9000);
    doc = Jsoup.parse(this.webdriver.getCurrentPageSource());

    // saving request content result on Amazon
    S3Service.saveResponseContent(session, requestHash, doc.toString());

    return doc;
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    JSONObject productInfo = crawlProductInfo(document);

    if (productInfo.length() > 0) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = crawlInternalId(productInfo);

      if (internalId != null) {
        Integer totalNumOfEvaluations = getTotalNumOfRatings(document);
        Double avgRating = getTotalAvgRating(document, totalNumOfEvaluations);

        ratingReviews.setInternalId(internalId);
        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);

        ratingReviewsCollection.addRatingReviews(ratingReviews);
      }

    }

    return ratingReviewsCollection;

  }

  private String crawlInternalId(JSONObject info) {
    String internalId = null;

    if (info.has("skus")) {
      JSONArray skus = info.getJSONArray("skus");

      if (skus.length() > 0) {
        JSONObject sku = skus.getJSONObject(0);

        if (sku.has("sku")) {
          internalId = sku.getString("sku");
        }
      }
    }

    return internalId;
  }

  /**
   * Return a json like this: "{\"product\": {\n" + "\t \"id\": \"7748\",\n" + "\t \"name\": \"Mucilon
   * arroz/aveia 400gr neste\",\n" + "\t \n" + "\t \"url\":
   * \"/produto/mucilon-arroz-aveia-400gr-neste-7748\",\n" + "\t \"images\": {\n" + "\t \"235x235\":
   * \"/fotos/PRODUTO_SEM_IMAGEM_mini.png\"\n" + "\t },\n" + "\t \"status\": \"available\",\n" + "\t\t
   * \n" + "\t \"price\": 12.50,\n" + "\t \"categories\": [{\"name\":\"Mamãe e
   * Bebê\",\"id\":\"8\"},{\"name\":\"Alimentos\",\"id\":\"89\",\"parents\":[\"8\"]},],\n" + "\t
   * \"installment\": {\n" + "\t \"count\": 0,\n" + "\t \"price\": 0.00\n" + "\t },\n" + "\t \n" + "\t
   * \n" + "\t \n" + "\t \t\"skus\": [ {\n" + "\t \t\t\t\"sku\": \"280263\"\n" + "\t \t}],\n" + "\t
   * \n" + "\t \"details\": {},\n" + "\t \t\t\"published\": \"2017-01-24\"\n" + "\t }}";
   *
   * @param doc
   * @return
   */
  private JSONObject crawlProductInfo(Document doc) {
    JSONObject info = new JSONObject();

    Elements scripts = doc.select("script");

    for (Element e : scripts) {
      String text = e.html();

      String varChaordic = "chaordicProduct =";

      if (text.contains(varChaordic)) {
        int x = text.indexOf(varChaordic) + varChaordic.length();
        int y = text.indexOf(';', x);

        String json = text.substring(x, y).trim();

        if (json.startsWith("{") && json.endsWith("}")) {
          JSONObject product = new JSONObject(json);

          if (product.has("product")) {
            info = product.getJSONObject("product");
          }
        }

        break;
      }
    }

    return info;
  }

  /**
   * Average is calculated Example: img src = ".../star5.png" [percentage bar] 0(number of evaluations
   * of this star)/0,00%(percentage of votes) img src = ".../star4.png" [percentage bar] 1(number of
   * evaluations of this star)/100,00%(percentage of votes)
   * 
   * All rating checked were without evaluations in time crawler was made
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document docRating, Integer totalRating) {
    Double avgRating = 0d;
    Elements rating = docRating.select(".card [data-rating]");

    if (totalRating != null && totalRating > 0) {
      Double total = 0d;
      for (Element e : rating) {
        Double value = MathUtils.parseDoubleWithComma(e.attr("data-rating"));

        if (value != null) {
          total += value;
        }
      }

      avgRating = MathUtils.normalizeTwoDecimalPlaces(total / totalRating);
    }

    return avgRating;
  }

  /**
   * Number of ratings appear in api
   * 
   * @param docRating
   * @return
   */
  private Integer getTotalNumOfRatings(Document docRating) {
    return docRating.select(".card [data-rating]").size();
  }


  private boolean isProductPage(Document document) {
    return !document.select(".product_body").isEmpty();
  }

}
