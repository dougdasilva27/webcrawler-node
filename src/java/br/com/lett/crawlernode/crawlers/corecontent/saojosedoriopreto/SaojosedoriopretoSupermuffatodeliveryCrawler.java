package br.com.lett.crawlernode.crawlers.corecontent.saojosedoriopreto;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermuffatodeliveryCrawler;

public class SaojosedoriopretoSupermuffatodeliveryCrawler extends SupermuffatodeliveryCrawler {

   public SaojosedoriopretoSupermuffatodeliveryCrawler(Session session) {
      super(session);
   }

   private static final String CITY_CODE = "16";

   @Override
   public String handleURLBeforeFetch(String url) {
      return super.handleURLBeforeFetch(url.split("\\?")[0] + "?sc=" + CITY_CODE);
   }
}