package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Collections;
import java.util.List;

public class BrasilNacaoverdeCrawler extends VTEXOldScraper {

   public BrasilNacaoverdeCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return "https://www.nacaoverde.com.br/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Collections.singletonList("nacaoverde");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
