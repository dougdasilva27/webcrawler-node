package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MercadolivreCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilMercadolivrejvm2808Crawler extends MercadolivreCrawler {

   private static final String SELLER_URL = "https://www.mercadolivre.com.br/perfil/JVM2808";

   private String scrapProductsUrl() {
      Document doc = fetchDocument(SELLER_URL);
      return CrawlerUtils.scrapUrl(doc, "a.publications__subtitle", "href", "https", "lista.mercadolivre.com.br");
   }

   public BrasilMercadolivrejvm2808Crawler(Session session) {
      super(session);
      super.setUrl(scrapProductsUrl());
      super.setProductUrlHost("produto.mercadolivre.com.br");
      super.setNextUrlHost("lista.mercadolivre.com.br");
   }
}
