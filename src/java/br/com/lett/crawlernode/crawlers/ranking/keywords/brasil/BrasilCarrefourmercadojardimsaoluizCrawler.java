package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;

public class BrasilCarrefourmercadojardimsaoluizCrawler extends BrasilCarrefourCrawler {

   public BrasilCarrefourmercadojardimsaoluizCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";

   @Override
   protected String getLocation() {
      return br.com.lett.crawlernode.crawlers.corecontent.brasil.BrasilCarrefourmercadojardimsaoluizCrawler.LOCATION_TOKEN;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getCep() {
      return br.com.lett.crawlernode.crawlers.corecontent.brasil.BrasilCarrefourmercadojardimsaoluizCrawler.CEP;
   }
}
