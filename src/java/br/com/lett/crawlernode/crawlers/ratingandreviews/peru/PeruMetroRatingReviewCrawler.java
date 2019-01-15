package br.com.lett.crawlernode.crawlers.ratingandreviews.peru;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.Fetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class PeruMetroRatingReviewCrawler extends RatingReviewCrawler {
  private static final String HOME_PAGE = "https://www.metro.pe/";
  private static final String MAIN_SELLER_NAME_LOWER = "metro";

  public PeruMetroRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    Logging.printLogDebug(logger, session,
        "Product page identified: " + this.session.getOriginalURL());

    String internalId = null;
    Integer totalNumOfEvaluations = 0;
    Double avgRating = 0.0;

    Element aux = doc.selectFirst("body");

    if (aux != null) {
      JSONObject outterJson = new JSONObject(aux.text());

      if (outterJson.has("reviews")) {
        JSONObject middleJson = new JSONObject(outterJson.get("reviews").toString());

        for (String s : middleJson.keySet()) {
          JSONObject innerJson = middleJson.getJSONObject(s);

          totalNumOfEvaluations += 1;

          if (innerJson.has("rating")) {
            avgRating = (double) Math
                .round(((avgRating + innerJson.getInt("rating")) / totalNumOfEvaluations));
          }
        }

        RatingsReviews ratingReviews = new RatingsReviews();
        ratingReviews.setDate(session.getDate());

        ratingReviews.setInternalId(internalId);
        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);

        ratingReviewsCollection.addRatingReviews(ratingReviews);
      }
    }

    return ratingReviewsCollection;
  }

  @Override
  protected Document fetch() {
    String html;

    if (this.config.getFetcher() == Fetcher.STATIC) {
      html = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, session.getOriginalURL(),
          null, cookies);
    } else {
      this.webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), session);
      html = this.webdriver.getCurrentPageSource();
    }

    JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(Jsoup.parse(html), session);
    VTEXCrawlersUtils vtexUtil =
        new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies);
    String internalPid = vtexUtil.crawlInternalPid(skuJson);
    String url = "https://www.metro.pe/wongfood/dataentities/RE/documents/" + internalPid
        + "?_fields=reviews";

    if (this.config.getFetcher() == Fetcher.STATIC) {
      html = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);
    } else {
      this.webdriver = DynamicDataFetcher.fetchPageWebdriver(url, session);
      html = this.webdriver.getCurrentPageSource();
    }

    return Jsoup.parse(html);
  }
}
