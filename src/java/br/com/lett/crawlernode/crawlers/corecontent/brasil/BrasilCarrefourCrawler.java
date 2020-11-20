package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CarrefourCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;

/**
 * 02/02/2018
 * 
 * @author gabriel
 *
 */
public class BrasilCarrefourCrawler extends CarrefourCrawler {

   private static final String HOME_PAGE = "https://www.carrefour.com.br/";
   private static final String LOCATION = "";

   public BrasilCarrefourCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
