package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class AdidasCrawler extends CrawlerRankingKeywords {
  private String HOME_PAGE = "";

  public AdidasCrawler(Session session, String HOME_PAGE) {
    super(session);
    this.HOME_PAGE = HOME_PAGE;
  }

  protected JSONObject fecthJson(String url) {
    JSONObject jsonSku = new JSONObject();
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "text/html,application/xhtmlxml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("accept-encoding", "gzip, deflate, br");
    headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
    headers.put("cache-control", "max-age=0");
    headers.put("upgrade-insecure-requests", "1");
    headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
    try {
      jsonSku = new JSONObject(Jsoup.connect(url).headers(headers).ignoreContentType(true).execute().body());
    } catch (JSONException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return jsonSku;
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 48;

    String url = HOME_PAGE + "/api/search/query?query=" + this.location + "&start=" + arrayProducts.size();
    JSONObject rankingJson = fecthJson(url);
    this.log("Link onde são feitos os crawlers: " + url);

    rankingJson = redirectUrl(rankingJson);

    JSONObject itemList = rankingJson.has("itemList") ? rankingJson.getJSONObject("itemList") : new JSONObject();

    if (this.totalProducts == 0) {
      setTotalProducts(itemList);
    }

    JSONArray items = getJSONArrayItems(itemList);
    if (items.length() > 0) {

      for (Object object : items) {
        JSONObject item = (JSONObject) object;

        String internalId = scrapInternalId(item);
        String internalPid = scrapInternalPid(item);
        String productUrl = scrapUrl(item);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

        if (this.arrayProducts.size() == productsLimit) {
          break;
        }

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }


  }

  protected JSONObject redirectUrl(JSONObject rankingJson) {
    if (rankingJson.has("redirect-url")) {
      String slug = buildSlug(rankingJson);

      String url = HOME_PAGE + "/api/search/taxonomy?query=" + slug + "&start=" + arrayProducts.size();
      rankingJson = fecthJson(url);
      this.log("Link onde são feitos os crawlers: " + url);
    }

    return rankingJson;
  }

  protected String buildSlug(JSONObject rankingJson) {
    String slug = rankingJson.getString("redirect-url");
    // ".[^0-9]./" we can try use this regex to remove "/us/"

    Matcher regSlug = Pattern.compile(".[^0-9]./").matcher(slug);
    if (regSlug.find()) {

      if (slug.contains("?")) {
        slug = slug.substring(regSlug.end(), slug.indexOf('?'));
      }
    } else {
      slug = slug.replace("/", "");
    }

    return slug;
  }

  private void setTotalProducts(JSONObject rankingJson) {
    if (rankingJson.has("count")) {
      this.totalProducts = rankingJson.getInt("count");
      this.log("Total da busca: " + this.totalProducts);
    }
  }

  protected String extractHostFromHomePage(String HOME_PAGE) {
    return HOME_PAGE.replace("https://", "");
  }

  protected String scrapUrl(JSONObject item) {
    String host = extractHostFromHomePage(HOME_PAGE);
    return item.has("link") ? CrawlerUtils.completeUrl(item.getString("link"), "https", host) : null;
  }

  private String scrapInternalPid(JSONObject item) {
    return item.has("modelId") ? item.get("modelId").toString() : null;
  }

  private String scrapInternalId(JSONObject item) {
    return item.has("productId") ? item.get("productId").toString() : null;
  }

  private JSONArray getJSONArrayItems(JSONObject itemList) {
    return itemList.has("items") ? itemList.getJSONArray("items") : new JSONArray();
  }


}
