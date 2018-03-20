package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.List;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.GPACrawler;

public class SaopauloPaodeacucarCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.paodeacucar.com";
  private static final String HOME_PAGE_HTTP = "http://www.paodeacucar.com";

  public SaopauloPaodeacucarCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  // Loja 501 sp
  private static final String STORE_ID = "501";

  @Override
  public void handleCookiesBeforeFetch() {

    // Criando cookie da loja 501 = São Paulo capital
    BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", STORE_ID);
    cookie.setDomain(".paodeacucar.com.br");
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
    return new GPACrawler(logger, session, HOME_PAGE, HOME_PAGE_HTTP, STORE_ID, cookies, "pa").extractInformation();
  }
}
