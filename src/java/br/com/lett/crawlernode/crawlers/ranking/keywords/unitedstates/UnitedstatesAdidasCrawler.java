package br.com.lett.crawlernode.crawlers.ranking.keywords.unitedstates;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.AdidasCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class UnitedstatesAdidasCrawler extends AdidasCrawler {
  private static String HOME_PAGE = "https://www.adidas.com";

  public UnitedstatesAdidasCrawler(Session session) {
    super(session, HOME_PAGE);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected String scrapUrl(JSONObject item) {
    String host = super.extractHostFromHomePage(HOME_PAGE);
    return item.has("link") ? CrawlerUtils.completeUrl(item.getString("link"), "https", host.concat("/us")) : null;
  }

  @Override
  protected JSONObject redirectUrl(JSONObject rankingJson) {
    if (rankingJson.has("redirect-url")) {
      String slug = buildSlug(rankingJson);

      String url = HOME_PAGE + "/api/search/taxonomy?query=" + slug + "&start=" + arrayProducts.size();
      rankingJson = super.fecthJson(url);
      this.log("Link onde são feitos os crawlers: " + url);
    }

    return rankingJson;
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
