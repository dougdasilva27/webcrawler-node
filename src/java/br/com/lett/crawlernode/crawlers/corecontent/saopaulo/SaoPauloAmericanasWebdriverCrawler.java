package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriverException;

import java.text.Normalizer;

public class SaoPauloAmericanasWebdriverCrawler extends SaopauloAmericanasCrawler {

   public SaoPauloAmericanasWebdriverCrawler(Session session) {
      super(session);
   }

   private Document webdriverRequest(String url) {
      Document doc;

      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(url, ProxyCollection.BUY_HAPROXY, session);

         if (webdriver != null) {
            doc = Jsoup.parse(webdriver.getCurrentPageSource());
            webdriver.terminate();
         } else {
            throw new WebDriverException("Failed to instantiate webdriver");
         }
      } catch (Exception e) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
         throw e;
      }

      return doc;
   }

   @Override
   protected Document fetch() {
      return webdriverRequest(session.getOriginalURL());
   }

   @Override
   protected Document acessOffersPage(String offersPageURL) {
      return webdriverRequest(offersPageURL);
   }

   @Override
   protected String crawlDescription(JSONObject apolloJson, Document doc, String internalPid) {
      StringBuilder description = new StringBuilder();

      boolean alreadyCapturedHtmlSlide = false;

      Element datasheet = doc.selectFirst("#info-section");
      if (datasheet != null) {
         Element iframe = datasheet.selectFirst("iframe");

         if (iframe != null) {
            Document docDescriptionFrame = webdriverRequest(iframe.attr("src"));
            if (docDescriptionFrame != null) {
               description.append(docDescriptionFrame.html());
            }
         }

         // https://www.shoptime.com.br/produto/8421276/mini-system-mx-hs6500-zd-bluetooth-e-funcao-karaoke-bivolt-preto-samsung
         // alreadyCapturedHtmlSlide as been moved here because of links like these.

         alreadyCapturedHtmlSlide = true;
         datasheet.select("iframe, h1.sc-hgHYgh").remove();
         description.append(datasheet.html().replace("hidden", ""));
      }

      if (internalPid != null) {
         Element desc2 = doc.select(".info-description-frame-inside").first();

         if (desc2 != null && !alreadyCapturedHtmlSlide) {
            String urlDesc2 = homePage + "product-description/acom/" + internalPid;
            Document docDescriptionFrame = webdriverRequest(urlDesc2);
            if (docDescriptionFrame != null) {
               description.append(docDescriptionFrame.html());
            }
         }

         Element elementProductDetails = doc.select(".info-section").last();
         if (elementProductDetails != null) {
            elementProductDetails.select(".info-section-header.hidden-md.hidden-lg").remove();
            description.append(elementProductDetails.html());
         }
      }
      if (description.length() == 0) {
         Object apolloDescription = apolloJson.optQuery("/ROOT_QUERY/product({\"productId\":\"" + internalPid + "\"})/description/content");
         if (apolloDescription != null) {
            description.append((String) apolloDescription);
         }
      }

      return Normalizer.normalize(description.toString(), Normalizer.Form.NFD).replaceAll("[^\n\t\r\\p{Print}]", "");
   }
}
