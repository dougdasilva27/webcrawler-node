package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MercadolivreCrawler;

public class ColombiaMercadolibrekalleyCrawler extends MercadolivreCrawler {

   private final String URL = "https://listado.mercadolibre.com.co/" + this.keywordWithoutAccents.replace(" ", "-") + "_Tienda_kalley#D[A:"
      + this.keywordWithoutAccents.replace(" ", "+") + ",on]";

   public ColombiaMercadolibrekalleyCrawler(Session session) {
      super(session);
      super.setProductUrlHost("articulo.mercadolibre.com.co");
      super.setNextUrlHost("listado.mercadolibre.com.co");
      super.setUrl(URL);
   }
}
