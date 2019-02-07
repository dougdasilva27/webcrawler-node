package br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils;

import java.util.List;
import org.apache.http.cookie.Cookie;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.session.Session;

public class AngelonieletroUtils {

  public static Document fetchSkuHtml(Document doc, Element skuElement, String mainId, Session session, List<Cookie> cookies) {
    Document skuHtml = doc;

    String internalId = scrapInternalIdFromSkulement(skuElement);

    if (internalId != null && !internalId.equalsIgnoreCase(mainId)) {
      StringBuilder url = new StringBuilder();
      url.append("https://www.angeloni.com.br/eletro/cartridges/DetalhesProduto/DetalhesProduto.jsp").append("?");
      url.append("skuId=").append(internalId);
      url.append("&productId=").append(scrapInternalPidFromSkulement(skuElement));
      url.append("&toCart=&changeSku=true&utm_source=&hideColor=undefined");

      skuHtml = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url.toString(), null, cookies);
    }

    return skuHtml;
  }


  public static String crawlInternalId(Document document) {
    String internalId = null;

    Element elementInternalId = document.select(".codigo span[itemprop=sku]").first();
    if (elementInternalId != null) {
      internalId = elementInternalId.text().trim();
    }

    return internalId;
  }

  public static String scrapInternalIdFromSkulement(Element skuElement) {
    return scrapInfoFromSKuElement(skuElement, 0);
  }

  public static String scrapInternalPidFromSkulement(Element skuElement) {
    return scrapInfoFromSKuElement(skuElement, 1);
  }

  public static String scrapInfoFromSKuElement(Element skuElement, int position) {
    String info = null;
    String onclick = skuElement.attr("onclick");

    if (onclick.contains("('") && onclick.contains(")")) {
      int x = onclick.indexOf("('") + 1;
      int y = onclick.indexOf(')', x);

      String[] infos = onclick.substring(x, y).replace("'", "").split(",");
      if (infos.length > position) {
        info = infos[position].trim();
      }
    }

    return info;
  }

  public static Document fetchVoltageApi(String internalPid, String mainId, Session session, List<Cookie> cookies) {
    StringBuilder url = new StringBuilder();
    url.append("https://www.angeloni.com.br/eletro/components/Product/SelectColorVoltagem.jsp").append("?");
    url.append("skuId=").append(mainId);
    url.append("&productId=").append(internalPid);
    url.append("&inStock=true&limitForPurchase=10&divElement=product-details&url=%2Fcartridges%2FDetalhesProduto%2FDetalhesProduto.jsp")
        .append("&showShipping=true&changeSku=true&quickView=&utm_source=");

    return DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url.toString(), null, cookies);
  }
}
