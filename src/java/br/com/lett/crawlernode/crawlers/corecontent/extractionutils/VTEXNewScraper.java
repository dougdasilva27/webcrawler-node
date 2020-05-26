package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public abstract class VTEXNewScraper extends VTEXScraper {

  public VTEXNewScraper(Session session) {
    super(session);
  }

  @Override
  protected String scrapInternalpid(Document doc) {
    Element elem = doc.selectFirst(".vtex-product-context-provider script");
    JSONObject json = JSONUtils.stringToJson(elem.data());
    return json.optString("mpn", null);
  }
}
