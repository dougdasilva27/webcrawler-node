package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.GpsfarmaCrawler;

public class ArgentinaGpsfarmarecoletaCrawler extends GpsfarmaCrawler {

  private List<Cookie> cookies = new ArrayList<>();

  public ArgentinaGpsfarmarecoletaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void processBeforeFetch() {
    // Criando cookie da regiao recoleta
    BasicClientCookie cookie = new BasicClientCookie("GPS_CITY_ID", "28");
    cookie.setDomain(".www.gpsfarma.com");
    cookie.setPath("/");
    this.cookies.add(cookie);

    // Criando cookie da cidade CABA
    BasicClientCookie cookie2 = new BasicClientCookie("GPS_REGION_ID", "509");
    cookie2.setDomain(".www.gpsfarma.com");
    cookie2.setPath("/");
    this.cookies.add(cookie2);

    // Criando cookie da loja 10
    BasicClientCookie cookie3 = new BasicClientCookie("GPS_WAREHOUSE_ID", "10");
    cookie3.setDomain(".www.gpsfarma.com");
    cookie3.setPath("/");
    this.cookies.add(cookie3);
  }


}
