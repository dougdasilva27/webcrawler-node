package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MercadolivreCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilMercadolivreterrierpetstoreCrawler extends MercadolivreCrawler {

   private static final String SELLER_URL = "https://www.mercadolivre.com.br/perfil/TERRIER%20PETSTORE";

   public String scrapProductsUrl(String url) {
      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).build();
      Response response = new FetcherDataFetcher().get(session, request);

      Document doc = Jsoup.parse(response.getBody());

      return CrawlerUtils.scrapUrl(doc, "a.publications__subtitle", "href", "https", "lista.mercadolivre.com.br");
   }

   @Override
   protected String getNextPageUrl() {
      String url = null;

      if (this.currentPage > 1) {
         url = this.nextUrl;
      } else {
         url = scrapProductsUrl(this.url);
      }

      return url;
   }

   public BrasilMercadolivreterrierpetstoreCrawler(Session session) {
      super(session);
      super.setUrl(SELLER_URL);
      super.setProductUrlHost("produto.mercadolivre.com.br");
      super.setNextUrlHost("lista.mercadolivre.com.br");
   }
}
