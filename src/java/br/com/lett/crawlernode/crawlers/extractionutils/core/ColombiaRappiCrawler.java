package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.session.Session;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

public class ColombiaRappiCrawler extends RappiCrawler {

   public ColombiaRappiCrawler(Session session) {
      super(session);
   }


   protected String getHomeDomain() {
      return "grability.rappi.com";
   }

   protected String getImagePrefix() {
      return "images.rappi.com/products";
   }

   @Override
   protected String getUrlPrefix() {
      return "producto/";
   }

   @Override
   protected String getHomeCountry() {
      return "https://www.rappi.com.co/";
   }

   @Override
   protected String getMarketBaseUrl() {
      return "https://www.rappi.com.co/tiendas/";
   }

   @Override
   protected boolean checkOldUrl(String productUrl) throws MalformedURLException {
      URL url = new URL(productUrl);
      final Pattern urlPathPattern = Pattern.compile(".*/([0-9][^a-zA-Z]*)_([0-9][^a-zA-Z]*)");

      boolean checkHost = url.getHost().contains("www.rappi.com.co");
      boolean checkPath = urlPathPattern.matcher(url.getPath()).matches();

      return checkHost && checkPath;
   }
}
