package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class BrasilIkesakiRatingReviewCrawler extends RatingReviewCrawler{

  public BrasilIkesakiRatingReviewCrawler(Session session) {
    super(session);
    // TODO Auto-generated constructor stub
  }
  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
    JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

    if(isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());
      String internalPid = Integer.toString(skuJson.getInt("productId"));
      
      Document docRating = crawlApiRatings(session.getOriginalURL(), internalPid);
      Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);            
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(getTotalAvgRating(docRating, totalNumOfEvaluations));    
      
      List<String> idList = VTEXCrawlersUtils.crawlIdList(skuJson);
      for (String internalId : idList) {
        RatingsReviews clonedRatingReviews = ratingReviews.clone();
        clonedRatingReviews.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
      }
      
    }
    return ratingReviewsCollection;
  } 
  
  private Document crawlApiRatings(String url, String internalPid){
    Document doc = new Document(url);
    
    String[] tokens = url.split("/");
    String productLinkId = tokens[tokens.length-2];
    String payload = "productId=" + internalPid + "&productLinkId=" + productLinkId;
    
    Map<String,String> headers = new HashMap<>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Accept-Language", "pt-BR,pt;q=0.8,en-US;q=0.6,en;q=0.4");
    
    String response = POSTFetcher.fetchPagePOSTWithHeaders("https://www.ikesaki.com.br/userreview", session, payload, cookies, 1, headers);
    
    if(response != null){
        doc = Jsoup.parse(response);
    }
    
    return doc;
}
  
  private Integer getTotalNumOfRatings(Document doc) {
    Integer numOfRatings = 0;
    Element nRate = doc.selectFirst(".media em > span");
    if(nRate != null) {      
      String stringNumOfRatings = nRate.text().replaceAll("[^0-9]", "").trim();
      numOfRatings = !stringNumOfRatings.isEmpty() ? Integer.parseInt(stringNumOfRatings) : 0;
    }
    return numOfRatings;
   }
  
  private Double getTotalAvgRating(Document doc, Integer totalNumOfEvaluations) {
    Double avgRating = 0.0;
    Elements rating = doc.select("div.rating-wrapper");
    if (totalNumOfEvaluations != null && totalNumOfEvaluations != 0) {
      Double total = 0.0;
      
      for (Element e : rating) {
        Element star = e.selectFirst("div.rating-wrapper div");       
        if (star != null) {
          if (star.hasClass("a50")) {
            total += 5;
          } else if (star.hasClass("a40")) {
            total += 4;
          } else if (star.hasClass("a30")) {
            total += 3;
          } else if (star.hasClass("a20")) {
            total += 2;
          } else if (star.hasClass("a10")) {
            total += 1;
          }
        }
      }
      avgRating = MathUtils.normalizeTwoDecimalPlaces(total / totalNumOfEvaluations);
    }
    return avgRating;
  }
  
  private boolean isProductPage(Document document) {
    return document.selectFirst(".product-info__name")!= null;
  }
  
}
