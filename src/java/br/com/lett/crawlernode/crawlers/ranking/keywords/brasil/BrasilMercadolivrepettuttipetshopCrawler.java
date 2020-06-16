package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MercadolivreCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilMercadolivrepettuttipetshopCrawler extends MercadolivreCrawler {

   private static final String SELLER_URL = "https://www.mercadolivre.com.br/perfil/PETTUTTIPETSHOP";

   private String scrapProductsUrl() {
      Document doc = fetchDocument(SELLER_URL);
      return CrawlerUtils.scrapUrl(doc, "a.publications__subtitle", "href", "https", "lista.mercadolivre.com.br");
   }

   public BrasilMercadolivrepettuttipetshopCrawler(Session session) {
      super(session);
      super.setUrl(scrapProductsUrl());
      super.setProductUrlHost("produto.mercadolivre.com.br");
      super.setNextUrlHost("lista.mercadolivre.com.br");
   }
}
