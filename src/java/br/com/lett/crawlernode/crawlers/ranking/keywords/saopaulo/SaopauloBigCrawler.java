package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VTEXRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;

public class SaopauloBigCrawler extends VTEXRankingKeywords {
   
   public SaopauloBigCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().getString("homePage");
   }

   protected Document fetchDocument() {

      StringBuilder searchPage = new StringBuilder();

      searchPage.append(getHomePage())
         .append(this.keywordEncoded)
         .append("?_q")
         .append(this.keywordEncoded)
         .append("&map=ft&page")
         .append(this.currentPage);

      String apiUrl = searchPage.toString().replace("+", "%20");

      return Jsoup.parse(fetchPage(apiUrl));
   }

   @Override
   protected String getLocation() {
      return "";
   }

   @Override
   protected String getVtexSegment() {
      return session.getOptions().getString("vtex_segment");
   }

}
