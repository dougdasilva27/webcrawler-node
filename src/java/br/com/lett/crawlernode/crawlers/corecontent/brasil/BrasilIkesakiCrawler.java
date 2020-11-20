package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class BrasilIkesakiCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "http://www.ikesaki.com.br/";
   private static final List<String> MAIN_SELLER_NAME_LOWER = Collections.singletonList("ikesaki");

   public BrasilIkesakiCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return MAIN_SELLER_NAME_LOWER;
   }

   //When this crawler was made, no product with rating was found
   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {

      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());

      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(internalId);

      ratingReviews.setAverageOverallRating(scrapAverageRating(doc));
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setTotalRating(CrawlerUtils.extractReviwsNumberOfAdvancedRatingReview(advancedRatingReview));

      return ratingReviews;
   }

   private Double scrapAverageRating(Document doc) {

      Integer averageOverallRatingInt = CrawlerUtils.scrapIntegerFromHtml(doc, "#spnRatingProdutoTop", false, 0);

      return (double)averageOverallRatingInt/10;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(String productId) {

      Document doc = fetchReviewDoc(productId);

      AdvancedRatingReview advancedRatingReview = CrawlerUtils.advancedRatingEmpty();

      for (int i = 1; i < 6; ++i) {
         Element element = doc.selectFirst(".rating li > em > .avaliacao"+i+"0");

         if (element != null) {
            Integer value = CrawlerUtils.scrapIntegerFromHtml(element.parent().parent(), null, false, 0);
            CrawlerUtils.incrementAdvancedRating(advancedRatingReview, i, value);
         }
      }

      return advancedRatingReview;
   }

   Document fetchReviewDoc(String productId) {

      String productUrlPath = "";
      String[] split = session.getOriginalURL().replace("://", "").split("/");
      if (split.length > 1) {
         productUrlPath = split[1];
      }

      String url = "https://www.ikesaki.com.br/userreview";
      String payload = "productId=" + productId +
         "&productLinkId=" + productUrlPath;

      Map<String, String> headers = new HashMap<>();

      headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");

      Request request = RequestBuilder
         .create()
         .setUrl(url)
         .setPayload(payload)
         .setHeaders(headers)
         .build();

      Response response = dataFetcher.post(session, request);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) {
      return CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".conteudo", ".detalhes"));
   }
}
