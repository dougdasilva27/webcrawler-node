package br.com.lett.crawlernode.crawlers.ranking.keywords.romania;


import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RomaniaBringoCrawler;

public class RomaniaBringocarrefourorhideeaCrawler extends RomaniaBringoCrawler {

   private static final String SELLE_FULL_NAME = "Carrefour Orhideea";

   public RomaniaBringocarrefourorhideeaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getMainSeller() {
      return SELLE_FULL_NAME;
   }
}
