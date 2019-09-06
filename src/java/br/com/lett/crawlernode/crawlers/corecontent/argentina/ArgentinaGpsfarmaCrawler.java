package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import org.apache.http.impl.cookie.BasicClientCookie;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.GpsfarmaCrawler;

/**
 * Date: 28/08/2019
 * 
 * @author João Pedro
 *
 */
public class ArgentinaGpsfarmaCrawler extends GpsfarmaCrawler {

  public ArgentinaGpsfarmaCrawler(Session session) {
    super(session);
  }

  @Override
  public void handleCookiesBeforeFetch() {

    // Criando cookie da cidade CABA
    BasicClientCookie cookie = new BasicClientCookie("GPS_CITY_ID", "32");
    cookie.setDomain(".www.gpsfarma.com");
    cookie.setPath("/");
    this.cookies.add(cookie);

    // Criando cookie da regiao sao nicolas
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
