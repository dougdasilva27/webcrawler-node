package br.com.lett.crawlernode.crawlers.ranking.keywords.fortaleza;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VTEXRankingKeywords;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class FortalezaPaguemenosCrawler extends VTEXRankingKeywords {

   private static final String HOME_PAGE = "https://www.paguemenos.com.br/";

   public FortalezaPaguemenosCrawler(Session session) {
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
   protected Document fetchDocument() {

      StringBuilder searchPage = new StringBuilder();

      searchPage.append(HOME_PAGE)
         .append(this.keywordEncoded)
         .append("?_q")
         .append(this.keywordEncoded)
         .append("&map=ft&page")
         .append(this.currentPage);

      String apiUrl = searchPage.toString().replace("+", "%20");
      //https://www.paguemenos.com.br/vitamina?_q=vitamina&map=ft&page=2
      //Request request = Request.RequestBuilder.create().setUrl(apiUrl).setCookies(this.cookies).build();

      return Jsoup.parse(fetchPage(apiUrl));
   }

}
