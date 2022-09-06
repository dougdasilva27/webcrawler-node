package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import models.AdvancedRatingReview;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class SaopauloAmericanasCrawler extends B2WCrawler {

   private static final String HOME_PAGE = "https://www.americanas.com.br/";
   private static final String URL_PAGE_OFFERS = "https://www.americanas.com.br/parceiros/";
   private static final String MAIN_SELLER_NAME_LOWER = "americanas.com";
   private static final String MAIN_SELLER_NAME_LOWER_FROM_HTML = "Americanas";

   public SaopauloAmericanasCrawler(Session session) {
      super(session);
      super.subSellers = Arrays.asList("b2w", "lojas americanas", "lojas americanas mg", "lojas americanas rj", "lojas americanas sp", "lojas americanas rs", "Lojas Americanas", "americanas");
      super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
      super.sellerNameLowerFromHTML = MAIN_SELLER_NAME_LOWER_FROM_HTML;
      super.homePage = HOME_PAGE;
      super.urlPageOffers = URL_PAGE_OFFERS;
      super.config.setFetcher(FetchMode.JSOUP);
   }
   
   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject reviews) {
      int star1 = 0;
      int star2 = 0;
      int star3 = 0;
      int star4 = 0;
      int star5 = 0;

      JSONArray ratingDistribution = reviews.optJSONArray("ratingDistribution");

      if (ratingDistribution != null) {
         for (Object o : ratingDistribution) {

            JSONObject ratingDistributionObject = (JSONObject) o;

            int ratingValue = ratingDistributionObject.optInt("ratingValue");
            int ratingCount = ratingDistributionObject.optInt("count");

            switch (ratingValue) {
               case 5:
                  star5 = ratingCount;
                  break;
               case 4:
                  star4 = ratingCount;
                  break;
               case 3:
                  star3 = ratingCount;
                  break;
               case 2:
                  star2 = ratingCount;
                  break;
               case 1:
                  star1 = ratingCount;
                  break;
               default:
                  break;
            }
         }
      }

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }

   @Override
   protected String crawlDescription(JSONObject apolloJson, Document doc, String internalPid) {
      Elements el = doc.select(".src__Container-sc-162vcai-0.kHvlHH");
      StringBuilder description = new StringBuilder();

      for (Element e : el) {
         String subtitle = CrawlerUtils.scrapStringSimpleInfo(e, ".title__TitleUI-sc-1eypgxa-1.cZeqMA", true);
         if (checkIsDescription(subtitle)) {
            description.append(e);
         }
      }
      if (description.length() == 0) {
         String descriptionAux = JSONUtils.getValueRecursive(apolloJson, "ROOT_QUERY.description", String.class);
         if (descriptionAux != null) {
            description.append(descriptionAux);
            description.append(" ");
         }
         JSONArray attributes = JSONUtils.getValueRecursive(apolloJson, "ROOT_QUERY.product:{\"productId\":\"" + internalPid + "\"}.attributes", JSONArray.class);
         for (Object obj : attributes) {
            JSONObject attribute = (JSONObject) obj;
            String attributeName = attribute.optString("name");
            String attributeValue = attribute.optString("value");
            if (attributeName != null && attributeValue != null) {
               description.append(attributeName);
               description.append(" ");
               description.append(attributeValue);
               description.append(" ");
            }
         }
      }

      return description.toString();
   }


   private boolean checkIsDescription(String subtitle) {
      String subtitleLowerWithoutAccents = "";
      if (subtitle != null) {
         subtitleLowerWithoutAccents = StringUtils.stripAccents(subtitle);
      }

      return subtitleLowerWithoutAccents.toLowerCase(Locale.ROOT).contains("ficha") || subtitleLowerWithoutAccents.toLowerCase(Locale.ROOT).contains("informacoes");

   }
}
