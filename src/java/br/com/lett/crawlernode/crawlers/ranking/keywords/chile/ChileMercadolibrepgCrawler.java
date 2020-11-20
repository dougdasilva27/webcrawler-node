package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MercadolivreCrawler;

public class ChileMercadolibrepgCrawler extends MercadolivreCrawler {

   private final String URL = "https://listado.mercadolibre.cl/" + this.keywordWithoutAccents.replace(" ", "-") + "_Tienda_pg#D[A:"
         + this.keywordWithoutAccents.replace(" ", "+") + ",pg]";

   public ChileMercadolibrepgCrawler(Session session) {
      super(session);
      super.setProductUrlHost("articulo.mercadolibre.cl");
      super.setNextUrlHost("listado.mercadolibre.cl");
      super.setUrl(URL);
   }

}
