package br.com.lett.crawlernode.crawlers.ranking.keywords.campinas;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.CarrefourCrawler;

public class CampinasCarrefouravenidaengenheiroantoniofranciscoCrawler extends CarrefourCrawler {
   private static final String HOME_PAGE = br.com.lett.crawlernode.crawlers.corecontent.campinas.CampinasCarrefouravenidaengenheiroantoniofranciscoCrawler.HOME_PAGE;

   public CampinasCarrefouravenidaengenheiroantoniofranciscoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLocation() {
      return br.com.lett.crawlernode.crawlers.corecontent.campinas.CampinasCarrefouravenidaengenheiroantoniofranciscoCrawler.LOCATION_TOKEN;
   }

   @Override
   protected String getCep() {
      return br.com.lett.crawlernode.crawlers.corecontent.campinas.CampinasCarrefouravenidaengenheiroantoniofranciscoCrawler.CEP;
   }
}
