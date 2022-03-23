package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class BrasilMaludocesCrawler extends VTEXRankingKeywords {

   public BrasilMaludocesCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected String getLocation() {
      return "";
   }

   @Override
   protected String getVtexSegment(){
      return session.getOptions().getJSONObject("cookies").optString("vtex_segment");
   }

   @Override
   protected Document fetchDocument() {

      StringBuilder searchPage = new StringBuilder();

      searchPage.append(getHomePage())
         .append(this.keywordEncoded)
         .append("?page=")
         .append(this.currentPage);


      String apiUrl = searchPage.toString().replace("+", "%20");

      return Jsoup.parse(fetchPage(apiUrl));
   }
}
