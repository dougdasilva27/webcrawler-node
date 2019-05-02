package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YotpoRatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 30/04/19
 * 
 * Testes:
 * https://www.mobly.com.br/armario-multiuso-com-mesa-goias-2-pt-branco-267959.html
 * https://www.mobly.com.br/armario-alderamin-1-pt-branco-12356.html
 * https://www.mobly.com.br/balcao-de-cozinha-village-ii-com-tampo-2-pt-3-gv-branco-241844.html
 * https://www.mobly.com.br/balcao-de-cozinha-lorena-com-tampo-2-pt-2-gv-branco-484395.html
 * 
 * @author Arthur Floresta
 */
public class BrasilMoblyRatingReviewCrawler extends RatingReviewCrawler {
  
  public BrasilMoblyRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());
      
      // Getting ID that Yotpo API uses to identify the product
      String yotpoId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-reviews .yotpo", "data-product-id");
      
      YotpoRatingReviewCrawler yotpo = new YotpoRatingReviewCrawler(session, cookies, logger);
      Document apiDoc = yotpo.extractRatingsFromYotpo(this.session.getOriginalURL(), yotpoId, fetchAppKey(dataFetcher), dataFetcher);
      
      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(apiDoc, "a.text-m", true, 0);
      Double avgRating = scrapAvgRating(apiDoc, ".yotpo-bottomline .sr-only");
      
      // Cehcking multiple skus in the page
      Elements skus = doc.select(".product-option .custom-select option[data-js-function]");
      
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      
      if (!skus.isEmpty()) {        
        for (Element sku : skus) {
          String internalID = scrapInternalIdForMutipleVariations(sku);
          RatingsReviews ratingsReviewsClone = ratingReviews.clone();
          
          ratingsReviewsClone.setInternalId(internalID);
          ratingReviewsCollection.addRatingReviews(ratingsReviewsClone);
        }
      } else {
        String internalID = scrapInternalIdSingleProduct(doc);
        
        ratingReviews.setInternalId(internalID);
        ratingReviewsCollection.addRatingReviews(ratingReviews);
      }
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst("#product-info") != null;
  }
  
  /**
   * Method to fetch App Key of Yotpo API.
   * 
   * @param dataFetcher {@link DataFetcher} object to fetch page.
   * @return
   */
  private String fetchAppKey(DataFetcher dataFetcher) {
    
    String url = "https://www.mobly.com.br/static/jsConfiguration/?v2=1556533989";
    
    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    String response = dataFetcher.get(session, request).getBody().trim();
    
    return CrawlerUtils.extractSpecificStringFromScript(response, "YOTPO_URL_KEY = 'staticw2.yotpo.com/", "/widget.js';", false);
  }

  private Double scrapAvgRating(Document doc, String selector) {
    Double avgRating = 0.0;
    
    Element e = doc.selectFirst(selector);
    
    if(e != null) {
      String text = e.text();
      text = text.replaceAll("[^0-9.]+", "");
      
      try {
        avgRating = Double.parseDouble(text);
      } catch (NumberFormatException ex) { }
    }
    
    return avgRating;
  }
  
  private String scrapInternalIdForMutipleVariations(Element sku) {
    String internalId = null;

    internalId = sku.val().trim();

    return internalId;
  }
  
  private String scrapInternalIdSingleProduct(Document document) {
    String internalId = null;
    Element internalIdElement = document.select(".add-wishlistsel-product-move-to-wishlist").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.attr("data-simplesku");
    }

    return internalId;
  }
}
