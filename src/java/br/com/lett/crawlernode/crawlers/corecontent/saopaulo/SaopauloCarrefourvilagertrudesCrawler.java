package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CarrefourCrawler;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloCarrefourvilagertrudesCrawler extends CarrefourCrawler {


   public SaopauloCarrefourvilagertrudesCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   public static final String LOCATION = "04794-000";
   public static final String LOCATION_TOKEN = "eyJhbGciOiJFUzI1NiIsImtpZCI6IkY0OTFDQ0U2QTNCQjU3MTgyNEE0QjRFREY1OUYxRDlDOTg0RTY5QjYiLCJ0eXAiOiJqd3QifQ.eyJhY2NvdW50LmlkIjoiNzQzMWNlNzItMTJlMy00OTczLWE5MjUtZTA2YTBmODhiZjU3IiwiaWQiOiIwYTg1NmNhZC1kODhlLTQ5MjItYWNkNS02NTg2N2E4ZWI2NTYiLCJ2ZXJzaW9uIjoyLCJzdWIiOiJzZXNzaW9uIiwiYWNjb3VudCI6InNlc3Npb24iLCJleHAiOjE2MzIzNDMzNTAsImlhdCI6MTYzMTY1MjE1MCwiaXNzIjoidG9rZW4tZW1pdHRlciIsImp0aSI6ImM5NjcxNmYzLTQ3MDUtNDNmNi04YWUwLWEzZDYxYjhlNmI4OSJ9.XatnnA3XvHW2lOunOTKBy__vIxWSTXLq5Ee3vNpkDN-SG0qwKuzQlpI-uItSuLHZ4DvQJkPTtXBA-XNpAlgerw";

   @Override
   protected String getLocationToken() {
      return LOCATION_TOKEN;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
