package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import java.util.List;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.GPACrawler;

public class RiodejaneiroExtraCrawler extends Crawler {

  public RiodejaneiroExtraCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.clubeextra.com.br";
  private static final String HOME_PAGE_HTTP = "http://www.clubeextra.com.br";


  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public void handleCookiesBeforeFetch() {

    // Criando cookie da loja 501 = São Paulo capital
    BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", GPACrawler.RIO_DE_JANEIRO_STORE_ID_EXTRA);
    cookie.setDomain(".clubeextra.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);

  }

  @Override
  protected Object fetch() {
    return new Document("");
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    return new GPACrawler(logger, session, HOME_PAGE, HOME_PAGE_HTTP, GPACrawler.RIO_DE_JANEIRO_STORE_ID_EXTRA, cookies, "ex", dataFetcher)
        .extractInformation();
  }

}
