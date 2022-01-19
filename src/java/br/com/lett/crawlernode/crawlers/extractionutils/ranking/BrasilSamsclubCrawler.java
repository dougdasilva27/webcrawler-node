package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class BrasilSamsclubCrawler extends VTEXRankingKeywords {

   private static final String HOME_PAGE = "https://www.samsclub.com.br/";

   public BrasilSamsclubCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLocation() {
      return "";
   }

   @Override
   protected String getVtexSegment(){
      return session.getOptions().getJSONObject("cookies").getString("vtex_segment");
   }

   @Override
   protected Document fetchDocument() {

      StringBuilder searchPage = new StringBuilder();

      searchPage.append(getHomePage())
         .append(this.keywordEncoded)
         .append("?_q=")
         .append(this.keywordEncoded)
         .append("&map=ft")
         .append("&page=")
         .append(this.currentPage);


      String apiUrl = searchPage.toString().replace("+", "%20");

      return Jsoup.parse(fetchPage(apiUrl));
   }
}
