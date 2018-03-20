package br.com.lett.crawlernode.crawlers.corecontent.brasilia;

import java.util.List;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.GPACrawler;

public class BrasiliaPaodeacucarCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.paodeacucar.com";
  private static final String HOME_PAGE_HTTP = "http://www.paodeacucar.com";
  private static final String STORE_ID = "6";

  public BrasiliaPaodeacucarCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
  }

  @Override
  public void handleCookiesBeforeFetch() {
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
