package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MercadolivreCrawler;

public class ChileMercadolibrehuggiesCrawler extends MercadolivreCrawler {

   private final String URL = "https://listado.mercadolibre.cl/" + this.keywordWithoutAccents.replace(" ", "-") + "_Tienda_huggies#D[A:"
         + this.keywordWithoutAccents.replace(" ", "+") + ",O:huggies]";

   public ChileMercadolibrehuggiesCrawler(Session session) {
      super(session);
      super.setProductUrlHost("articulo.mercadolibre.cl");
      super.setNextUrlHost("listado.mercadolibre.cl");
      super.setUrl(URL);
   }

}
