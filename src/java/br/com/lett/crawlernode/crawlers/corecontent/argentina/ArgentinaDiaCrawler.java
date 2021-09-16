package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class ArgentinaDiaCrawler extends VTEXScraper {
   public ArgentinaDiaCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   protected String getHomePage() {
      return "https://diaonline.supermercadosdia.com.ar/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList("Supermercados DIA");
   }

   @Override
   protected String scrapInternalpid(Document doc) {
         return CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#___rc-p-id", "value");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
