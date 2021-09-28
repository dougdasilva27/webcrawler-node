package br.com.lett.crawlernode.crawlers.ranking.keywords.portoalegre;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.CarrefourCrawler;
import br.com.lett.crawlernode.crawlers.ranking.keywords.brasil.BrasilCarrefourCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 18/04/2020
 * 
 * @author Fabrício
 *
 */
public class PortoalegreCarrefourCrawler extends CarrefourCrawler {

   public PortoalegreCarrefourCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = br.com.lett.crawlernode.crawlers.corecontent.portoalegre.PortoalegreCarrefourCrawler.HOME_PAGE;
   public static final String CEP = br.com.lett.crawlernode.crawlers.corecontent.portoalegre.PortoalegreCarrefourCrawler.CEP;
   public static final String LOCATION_TOKEN = br.com.lett.crawlernode.crawlers.corecontent.portoalegre.PortoalegreCarrefourCrawler.LOCATION_TOKEN;
   public static final String REGION_ID = "U1cjY2FycmVmb3VyYnI5NzY=";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLocation() {
      return LOCATION_TOKEN;
   }

   @Override
   protected String getCep() {
      return CEP;
   }
}
