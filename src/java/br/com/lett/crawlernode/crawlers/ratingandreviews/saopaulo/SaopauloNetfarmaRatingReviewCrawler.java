package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class SaopauloNetfarmaRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloNetfarmaRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();

      JSONObject chaordicMeta = crawlChaordicMeta(document);

      ratingReviews.setDate(session.getDate());
      ratingReviews.setInternalId(crawlInternalId(document));

      String sku = crawlSkuId(chaordicMeta);

      JSONObject reviewPage = requestReviewPage(sku, 1);

      if (reviewPage.has("totalPaginas") && reviewPage.has("totalRegistros")) {
        Integer numRatings = reviewPage.getInt("totalRegistros");
        Integer totalPages = reviewPage.getInt("totalPaginas");
        Double totalRating = 0.0;

        // get the totalRating in the first page
        totalRating = totalRating + getTotalRatingFromReviewPage(reviewPage);

        for (int i = 2; i <= totalPages; i++) {
          reviewPage = requestReviewPage(sku, i);
          totalRating = totalRating + getTotalRatingFromReviewPage(reviewPage);
        }

        Double avgRating;
        if (totalRating.equals(0.0)) {
          avgRating = 0.0;
        } else {
          avgRating = MathUtils.normalizeTwoDecimalPlaces(totalRating / numRatings);
        }

        ratingReviews.setTotalRating(numRatings);
        ratingReviews.setAverageOverallRating(avgRating);
      }

      ratingReviewsCollection.addRatingReviews(ratingReviews);

    }


    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return doc.select(".product-details__code").first() != null;
  }

  private Double getTotalRatingFromReviewPage(JSONObject reviewPage) {
    Double totalRating = 0.0;

    if (reviewPage.has("avaliacoes")) {
      JSONArray ratings = reviewPage.getJSONArray("avaliacoes");

      for (int j = 0; j < ratings.length(); j++) {
        JSONObject rating = ratings.getJSONObject(j);
        if (rating.has("nota")) {
          totalRating = totalRating + new Double(rating.getInt("nota"));
        }
      }
    }

    return totalRating;
  }

  private String crawlSkuId(JSONObject jsonProduct) {
    String internalId = null;
    if (jsonProduct.has("sku")) {
      internalId = jsonProduct.getString("sku").trim();
    }
    return internalId;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    String name = crawlName(doc);

    JSONObject object = new JSONObject();
    String token = "vargoogle_tag_params=";
    Elements scripts = doc.select("script");

    for (Element e : scripts) {
      String script = e.outerHtml();

      // replace name because some names has ' that break json
      script = script.replace(name, "").replace(" ", "");

      if (script.contains(token)) {
        int x = script.indexOf(token) + token.length();
        int y = script.indexOf("};", x) + 1;

        String json = script.substring(x, y).trim();

        if (json.startsWith("{") && json.endsWith("}")) {
          try {
            object = new JSONObject(json);
          } catch (Exception e1) {
            Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e1));
          }
        }

        break;
      }
    }

    if (object.has("ecomm_prodid")) {
      internalId = object.getString("ecomm_prodid");
    }

    return internalId;
  }

  private String crawlName(Document document) {
    String name = null;

    // get base name
    Element elementName = document.select(".product-details__title").first();
    if (elementName != null) {
      name = elementName.text().trim();
    }

    if (name != null) {
      // get 'gramatura' attribute
      Element gramaturaElement = document.select(".product-details__measurement").first();
      if (gramaturaElement != null) {
        name = name + " " + gramaturaElement.text().trim();
      }
    }

    return name;
  }

  private JSONObject requestReviewPage(String skuId, Integer pageNumber) {
    String url = "https://www.netfarma.com.br/api/produto/" + skuId + "/avaliacoes/" + pageNumber;
    return DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, null);
  }

  /**
   * Get the window.chaordic_meta script inside the html.
   * 
   * window.chaordic_meta = { "page": "product", "sku": "C02500LRE00", "price": 29.90, "pid": "36755"
   * };
   * 
   * @return
   */
  private JSONObject crawlChaordicMeta(Document document) {
    Elements scriptTags = document.getElementsByTag("script");
    String scriptVariableName = "window.chaordic_meta = ";
    JSONObject chaordicMeta = null;

    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith(scriptVariableName)) {
          chaordicMeta = new JSONObject(node.getWholeData().split(Pattern.quote(scriptVariableName))[1]
              + node.getWholeData().split(Pattern.quote(scriptVariableName))[1].split(Pattern.quote("};"))[0]);
        }
      }
    }

    if (chaordicMeta == null) {
      chaordicMeta = new JSONObject();
    }

    return chaordicMeta;
  }


}
