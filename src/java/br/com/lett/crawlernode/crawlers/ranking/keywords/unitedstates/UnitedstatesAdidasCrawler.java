package br.com.lett.crawlernode.crawlers.ranking.keywords.unitedstates;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.AdidasCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class UnitedstatesAdidasCrawler extends AdidasCrawler {
  private static String HOST = "www.adidas.com";

  public UnitedstatesAdidasCrawler(Session session) {
    super(session, HOST);
  }

  @Override
  protected String scrapUrl(JSONObject item) {
    return item.has("link") ? CrawlerUtils.completeUrl(item.getString("link"), "https", HOST.concat("/us")) : null;
  }

  @Override
  protected JSONObject accessRedirect(JSONObject rankingJson) {
    String slug = buildSlug(rankingJson);
    String url =
        "https://".concat(HOST).concat("/api/search/taxonomy?query=").concat(slug).concat("&start=").concat(Integer.toString(arrayProducts.size()));

    this.log("Link onde são feitos os crawlers: " + url);
    return super.fecthJson(url);
  }

  @Override
  protected String buildSlug(JSONObject rankingJson) {
    String slug = rankingJson.getString("redirect-url");
    // ".[^0-9]./" we can try use this regex to remove "/us/"

    Matcher regSlug = Pattern.compile(".[^0-9]./").matcher(slug);

    if (regSlug.find()) {

      if (slug.contains("?")) {
        slug = slug.substring(regSlug.end());
        slug = slug.replace("?", "&");
      }
    } else {
      slug = slug.replace("/", "");
    }

    return slug;
  }
}
