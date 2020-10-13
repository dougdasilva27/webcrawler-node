package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXNewScraper;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXOldScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class SaopauloTrimaisCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.trimais.com.br/";
   private static final List<String> MAIN_SELLERS = Collections.singletonList("TriMais");
   public SaopauloTrimaisCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return MAIN_SELLERS;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {

      Document rating = fetchRatingApi(jsonSku, internalPid);

      RatingsReviews ratingsReviews = new RatingsReviews();

      AdvancedRatingReview advancedRatingReview = scrapAdvancedRating(rating);

      int totalReviews = advancedRatingReview.getTotalStar1() + advancedRatingReview.getTotalStar2() + advancedRatingReview.getTotalStar3() +
         advancedRatingReview.getTotalStar4() + advancedRatingReview.getTotalStar5();

      int avgReviews = CrawlerUtils.scrapIntegerFromHtml(rating, ".avaliacao .media span:last-child", true, 0);

      ratingsReviews.setTotalRating(totalReviews);
      ratingsReviews.setAverageOverallRating((double)avgReviews);
      ratingsReviews.setTotalWrittenReviews(totalReviews);
      ratingsReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingsReviews;
   }

   private AdvancedRatingReview scrapAdvancedRating(Document ratingStars){

      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();

      Elements stars = ratingStars.select("ul.rating li");

      int count = 0;
      for(Element star: stars){
         count++;
         int starNum = CrawlerUtils.scrapIntegerFromHtml(star, "span:nth-of-type(2)", null, null, false, true, 0);
         switch (count){
            case 1:
               advancedRatingReview.setTotalStar1(starNum);
               break;
            case 2:
               advancedRatingReview.setTotalStar2(starNum);
               break;
            case 3:
               advancedRatingReview.setTotalStar3(starNum);
               break;
            case 4:
               advancedRatingReview.setTotalStar4(starNum);
               break;
            case 5:
               advancedRatingReview.setTotalStar5(starNum);
               break;
            default:
         }
      }

      return advancedRatingReview;
   }

   private Document fetchRatingApi(JSONObject jsonSku, String internalPid){
      String apiRating = "https://www.trimais.com.br/userreview";
      String valueId = null;
      JSONArray referenceId = jsonSku.optJSONArray("referenceId");
      String nameComplete = jsonSku.optString("nameComplete").toLowerCase().replace(" ", "-");

      if(!referenceId.isEmpty()){
         JSONObject value = (JSONObject) referenceId.get(0);
         valueId = value.optString("Value");
      }

      String productLinkId = valueId + "-" + nameComplete;

      StringBuilder formData = new StringBuilder();
      formData.append("productId=").append(internalPid).append("&productLinkId=").append(productLinkId);

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "*/*");
      headers.put("accept-encoding", "gzip, deflate, br");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");

      Request request = Request.RequestBuilder.create().setUrl(apiRating).setHeaders(headers).setPayload(formData.toString()).setCookies(cookies).build();

      String response = this.dataFetcher.post(session, request).getBody();

      return Jsoup.parse(response);

   }
}
