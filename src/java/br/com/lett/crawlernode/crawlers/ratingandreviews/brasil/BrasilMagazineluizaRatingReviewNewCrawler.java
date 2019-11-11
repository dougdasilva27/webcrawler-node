package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 14/12/16
 * @author gabriel
 *
 */
public class BrasilMagazineluizaRatingReviewNewCrawler extends RatingReviewCrawler {

	public BrasilMagazineluizaRatingReviewNewCrawler(Session session) {
	  super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
	  RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

	  if (isProductPage(session.getOriginalURL())) {
	    Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
	    ratingReviewsCollection.addRatingReviews(crawlRatingNew(doc));
	  } else {
	    Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
	  }

	  return ratingReviewsCollection;
	} 

	/**
	 * 
	 * @param doc
	 * @return
	 */
	public RatingsReviews crawlRatingNew(Document doc) {

	  // InternalId
	  String internalId = crawlInternalId(doc, "digitalData = ");
		
	  RatingsReviews ratingReviews = crawlRatingReviews(doc);
	  ratingReviews.setInternalId(internalId);
		
	  return ratingReviews;
	}
	
	/**
	 * Crawl Internal ID 
	 * @param doc
	 * @return
	 */
	private String crawlInternalId(Document doc, String token) {
	  String internalId = null;
        
	  Elements scriptTags = doc.getElementsByTag("script");

      for (Element tag : scriptTags){                
        String html = tag.outerHtml();
  
        if(html.contains(token)) {
          int x = html.indexOf(token) + token.length();
          int y = html.indexOf("};", x) + 1;
  
          String json = html.substring(x, y)
              .replace("window.location.host", "''")
              .replace("window.location.protocol", "''")
              .replace("window.location.pathname", "''")
              .replace("document.referrer", "''")
              .replace("encodeURIComponent(", "")
              .replace("'),", "',");
          
          for(String line : json.split("\n")) {
            String jsonToken = "'idSku'";
            
            if(line.contains(jsonToken)) {
              Pattern p = Pattern.compile("\\'(.*?)\\'"); 
              Matcher m = p.matcher(line); 
              
              while (m.find()) {
                String group = m.group();
                
                if(!group.equals(jsonToken)) {
                  internalId = group.substring(1, group.length()-1);
                  break;
                }
              }
              
              break;
            }
          }
        }
      }
      
      return internalId;
	}

	/**
	 * Crawl rating and reviews stats using the bazaar voice endpoint.
	 * To get only the stats summary we need at first, we only have to do
	 * one request. If we want to get detailed information about each review, we must
	 * perform pagination.
	 * 
	 * The RatingReviews crawled in this method, is the same across all skus variations
	 * in a page.
	 *
	 * @param document
	 * @return
	 */
	private RatingsReviews crawlRatingReviews(Document doc) {
	  RatingsReviews ratingReviews = new RatingsReviews();

	  ratingReviews.setDate(session.getDate());

	  ratingReviews.setTotalRating(getTotalReviewCount(doc));
	  ratingReviews.setAverageOverallRating(getAverageOverallRating(doc));

	  return ratingReviews;
	}

	private Integer getTotalReviewCount(Document doc) {
	  Integer totalReviewCount = null;
	  Element total = doc.select(".interaction-client__rating-info > span").last();
		
	  if (total != null) {
	    totalReviewCount = Integer.parseInt(total.ownText().replaceAll("[^0-9]", ""));
	  }
	  
	  return totalReviewCount;
	}

	private Double getAverageOverallRating(Document doc) {
	  Double avgOverallRating = null;
	  Element avg = doc.select(".interaction-client__rating-info > span").first();
		
	  if (avg != null) {
	    avgOverallRating = Double.parseDouble(avg.ownText().replaceAll("[^0-9,]", "").replace(",", "."));
	  }
	  
	  return avgOverallRating;
	}

	private boolean isProductPage(String url) {
	  return url.contains("/p/") || url.contains("/p1/");
	}
}