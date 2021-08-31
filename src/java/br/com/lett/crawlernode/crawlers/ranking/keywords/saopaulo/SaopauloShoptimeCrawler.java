package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.B2WCrawlerRanking;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.B2WScriptPageCrawlerRanking;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Map;

public class SaopauloShoptimeCrawler extends B2WScriptPageCrawlerRanking {

   private static final String HOME_PAGE = "https://www.shoptime.com.br/";

  public SaopauloShoptimeCrawler(Session session) {
    super(session);
  }
   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected Document fetchPage() {
      String keyword = this.keywordWithoutAccents.replace(" ", "-");

      String url = homePage + "busca/" + keyword + "?limit=24&offset=" + (this.currentPage - 1) * pageSize;
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String,String> headers = new HashMap<>();

      headers.put("authority", "www.americanas.com.br");
      headers.put("sec-ch-ua", " \" Not A;Brand\";v=\"99\", \"Chromium\";v=\"90\", \"Google Chrome\";v=\"90\"");
      headers.put("sec-ch-ua-mobile", "?0");
      headers.put("upgrade-insecure-requests", "1");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,/;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("sec-fetch-site", "none");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-user", "?1");
      headers.put("sec-fetch-dest", "document");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");

      return Jsoup.parse(br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloAmericanasCrawler.fetchPage(url,this.dataFetcher,this.cookies,headers,session));
   }
}
