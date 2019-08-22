package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 14/12/16
 * 
 * @author gabriel
 *
 */
public class SaopauloPontofrioRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloPontofrioRatingReviewCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  private static final String HOME_PAGE = "https://www.pontofrio.com.br/";

  @Override
  protected Document fetch() {
    String page = fetchPage(session.getOriginalURL());

    if (page != null) {
      return Jsoup.parse(page);
    }

    return new Document("");
  }

  private String fetchPage(String url) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
    headers.put("Cache-Control", "no-cache");
    headers.put("Connection", "keep-alive");
    headers.put("Host", "www.pontofrio.com.br");
    headers.put("Referer", HOME_PAGE);
    headers.put("Upgrade-Insecure-Requests", "1");
    headers.put("User-Agent", FetchUtilities.randUserAgent());


    Request request = RequestBuilder.create()
        .setUrl(url)
        .setCookies(cookies)
        .setHeaders(headers)
        .setProxyservice(
            Arrays.asList(
                ProxyCollection.INFATICA_RESIDENTIAL_BR,
                ProxyCollection.STORM_RESIDENTIAL_EU,
                ProxyCollection.BUY,
                ProxyCollection.STORM_RESIDENTIAL_US
            )
        ).build();
    return this.dataFetcher.get(session, request).getBody();
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalRating(document);
      Double avgRating = getTotalAvgRating(document);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      List<String> idList = crawlInternalIds(document);
      for (String internalId : idList) {
        RatingsReviews clonedRatingReviews = (RatingsReviews) ratingReviews.clone();
        clonedRatingReviews.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
      }
    }

    return ratingReviewsCollection;

  }


  /**
   * 
   * @param doc
   * @return
   */
  private Integer getTotalRating(Document doc) {
    Integer total = 0;

    Element finalElement = null;
    Element rating = doc.select(".pr-snapshot-average-based-on-text .count").first();
    Element ratingOneEvaluation = doc.select(".pr-snapshot-average-based-on-text").first();
    Element specialEvaluation = doc.select(".rating-count[itemprop=\"reviewCount\"]").first();

    if (rating != null) {
      finalElement = rating;
    } else if (ratingOneEvaluation != null) {
      finalElement = ratingOneEvaluation;
    } else if (specialEvaluation != null) {
      finalElement = specialEvaluation;
    }

    if (finalElement != null) {
      total = Integer.parseInt(finalElement.ownText().replaceAll("[^0-9]", ""));
    }

    return total;
  }

  /**
   * @param Double
   * @return
   */
  private Double getTotalAvgRating(Document doc) {
    Double avgRating = 0d;

    Element avg = doc.select(".pr-snapshot-rating.rating .pr-rounded.average").first();

    if (avg == null) {
      avg = doc.select(".rating .rating-value").first();
    }

    if (avg != null) {
      avgRating = Double.parseDouble(avg.ownText().replace(",", "."));
    }

    return avgRating;
  }

  private List<String> crawlInternalIds(Document doc) {
    List<String> ids = new ArrayList<>();
    String internalPid = crawlInternalPid(doc);

    if (hasProductVariations(doc)) {
      Elements skuOptions = doc.select(".produtoSku option[value]:not([value=\"\"])");

      for (Element e : skuOptions) {
        ids.add(internalPid + "-" + e.attr("value"));
      }

    } else {
      Element elementDataSku = doc.select("#ctl00_Conteudo_hdnIdSkuSelecionado").first();

      if (elementDataSku != null) {
        ids.add(internalPid + "-" + elementDataSku.attr("value"));
      }
    }

    return ids;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Elements elementInternalId = document.select("script[type=text/javascript]");

    String idenfyId = "idProduct";

    for (Element e : elementInternalId) {
      String script = e.outerHtml();

      if (script.contains(idenfyId)) {
        script = script.replaceAll("\"", "");

        int x = script.indexOf(idenfyId);
        int y = script.indexOf(',', x + idenfyId.length());

        internalPid = script.substring(x + idenfyId.length(), y).replaceAll("[^0-9]", "").trim();
      }
    }


    return internalPid;
  }

  private boolean hasProductVariations(Document document) {
    Elements skuChooser = document.select(".produtoSku option[value]:not([value=\"\"])");

    if (skuChooser.size() > 1) {
      if (skuChooser.size() == 2) {
        String prodOne = skuChooser.get(0).text();
        if (prodOne.contains("|")) {
          prodOne = prodOne.split("\\|")[0].trim();
        }

        String prodTwo = skuChooser.get(1).text();
        if (prodTwo.contains("|")) {
          prodTwo = prodTwo.split("\\|")[0].trim();
        }


        if (prodOne.equals(prodTwo)) {
          return false;
        }
      }
      return true;
    }

    return false;

  }

  private boolean isProductPage(Document doc) {
    Element productElement = doc.select(".produtoNome h1").first();

    if (productElement != null) {
      return true;
    }

    return false;
  }

}
