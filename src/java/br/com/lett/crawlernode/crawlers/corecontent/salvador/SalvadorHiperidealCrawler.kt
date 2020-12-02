package br.com.lett.crawlernode.crawlers.corecontent.salvador

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.nodes.Document
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder

class SalvadorHiperidealCrawler(session: Session) : VTEXOldScraper(session) {

  override fun getHomePage(): String {
    return "https://www.hiperideal.com.br/"
  }

  override fun getMainSellersNames(): MutableList<String> {
    return mutableListOf("Hiper Ideal")
  }

  override fun scrapRating(internalId: String?, internalPid: String?, doc: Document?, jsonSku: JSONObject?): RatingsReviews {
    return RatingsReviews()
  }
	
	override fun crawlProductApi(internalPid: String?, parameters: String?): JSONObject {
		  var productApi = JSONObject();

      val url = homePage + "api/catalog_system/pub/products/search?fq=productId:" + internalPid + "&sc=9";

      val request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      val array = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (!array.isEmpty()) {
         var obj: JSONObject? = array.optJSONObject(0);
		  
		     if(obj != null) {
		        productApi = obj;
				  }
      }

      return productApi;
	}
	
}