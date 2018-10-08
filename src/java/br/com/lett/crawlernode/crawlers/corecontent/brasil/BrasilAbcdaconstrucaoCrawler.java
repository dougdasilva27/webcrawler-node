package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 10/09/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilAbcdaconstrucaoCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.abcdaconstrucao.com.br/";

  public BrasilAbcdaconstrucaoCrawler(Session session) {
    super(session);
  }

  @Override
  protected Object fetch() {
    Document doc = new Document("");
    String originalUrl = session.getOriginalURL();
    String payload = "c=produtos";

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    String urlToFetch = null;

    if (originalUrl.contains("ofertas.php")) {
      urlToFetch = "https://www.abcdaconstrucao.com.br/ajax/index.ajax.php";
    } else if (originalUrl.contains("outlet.php")) {
      urlToFetch = "https://www.abcdaconstrucao.com.br/ajax/outlet.ajax.php";
    }

    if (urlToFetch != null) {
      doc = Jsoup.parse(POSTFetcher.fetchPagePOSTWithHeaders(urlToFetch, session, payload, cookies, 1, headers, null, null));
    }

    return doc;
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      Elements productsElements = doc.select("a.btn2-oferta");

      for (Element e : productsElements) {
        String internalId = crawlInternalId(e);
        String internalPid = crawlInternalPid(e);
        String name = crawlName(e);
        Float price = crawlPrice(e);
        Prices prices = crawlPrices(price, e);
        String primaryImage = crawlPrimaryImage(e);
        String description = crawlDescription(e);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(true).setPrimaryImage(primaryImage).setDescription(description)
            .setMarketplace(new Marketplace()).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select("a.btn2-oferta").isEmpty();
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;

    if (e.hasAttr("data-cod")) {
      internalPid = e.attr("data-cod");
    } else if (e.hasAttr("rel")) {
      internalPid = e.attr("rel");
    }

    return internalPid;
  }

  private String crawlInternalId(Element e) {
    String internalId = null;

    Element internalIdElement = e.selectFirst(".rodape-produto");
    if (internalIdElement != null) {
      String text = internalIdElement.ownText().toLowerCase().trim();

      if (text.contains("cod.")) {
        String value = CommonMethods.getLast(text.split("cod.")).trim();

        if (value.contains(" ")) {
          internalId = value.split(" ")[0];
        } else {
          internalId = value;
        }
      }
    }

    return internalId;
  }

  private String crawlName(Element e) {
    String name = null;
    Element nameElement = e.selectFirst(".produtocontent [itemprop=name]");

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Element e) {
    Float price = null;

    String priceText = null;
    Element salePriceElement = e.selectFirst(".produtocontent .preco [itemprop=price]");

    if (salePriceElement != null) {
      priceText = salePriceElement.ownText();

      // In this site some products appear like this: R$ 54,90
      // and others appear like this: R$ 54.90
      if (priceText.contains(",")) {
        price = MathUtils.parseFloatWithComma(priceText);
      } else {
        price = Float.parseFloat(priceText.replaceAll("[^0-9.]", ""));
      }
    }

    return price;
  }

  private String crawlPrimaryImage(Element e) {
    String primaryImage = null;
    Element elementPrimaryImage = e.selectFirst(".produtoimagem .img-produto img");

    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("src").trim();

      if (!primaryImage.startsWith("http")) {
        primaryImage = HOME_PAGE + primaryImage;
      }
    }

    return primaryImage;
  }

  private String crawlDescription(Element e) {
    StringBuilder description = new StringBuilder();

    Element desc = e.selectFirst(".rodape-produto");

    if (desc != null) {
      description.append(desc.html());
    }


    return description.toString();
  }

  /**
   * In the time when this crawler was made, this market hasn't installments informations
   * 
   * @param element
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Element element) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      Element priceFrom = element.selectFirst(".produtocontent .txt-preco1");
      if (priceFrom != null) {
        String priceText = priceFrom.ownText();

        if (priceText.contains(",")) {
          prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceText));
        } else {
          prices.setPriceFrom(Double.parseDouble(priceText.replaceAll("[^0-9.]", "")));
        }
      }

      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
    }

    return prices;
  }

}
