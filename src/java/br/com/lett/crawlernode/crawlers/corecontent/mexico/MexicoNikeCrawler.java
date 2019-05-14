package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.NikeCrawler;

public class MexicoNikeCrawler extends NikeCrawler {

  protected static final String HOME_PAGE = "https://www.nike.com/mx";
  protected static final String COUNTRY_URL = "/es_la/";

  public MexicoNikeCrawler(Session session) {
    super(session);
    super.HOME_PAGE = HOME_PAGE;
    super.COUNTRY_URL = COUNTRY_URL;
  }

  @Override
  protected String getSkuName(JSONObject json, String name) {
    if (json.has("localizedSizePrefix") && json.has("localizedSize")) {
      String prefix = json.getString("localizedSizePrefix");
      String size = json.getString("localizedSize");

      if (!prefix.isEmpty() && !size.isEmpty()) {
        name += " - " + prefix + " " + size;
      }
    }

    return name;
  }
}
