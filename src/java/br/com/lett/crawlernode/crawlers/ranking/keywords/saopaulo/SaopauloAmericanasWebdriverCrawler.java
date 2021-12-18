package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import org.jsoup.nodes.Document;

public class SaopauloAmericanasWebdriverCrawler extends SaopauloAmericanasCrawler {

   private static final String HOME_PAGE = "https://www.americanas.com.br/";

   public SaopauloAmericanasWebdriverCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchPage() {
      String keyword = this.keywordWithoutAccents.replace(" ", "-");

      String url = HOME_PAGE + "busca/" + keyword + "?limit=24&offset=" + (this.currentPage - 1) * pageSize;
      this.log("Link onde são feitos os crawlers: " + url);

      return fetchDocumentWithWebDriver(url);
   }
}
