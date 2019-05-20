package br.com.lett.crawlernode.crawlers.corecontent.florianopolis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.xml.utils.URI;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class FlorianopolisAngeloniCrawler extends Crawler {

  public FlorianopolisAngeloniCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();

    boolean shouldVisit = false;

    shouldVisit = !FILTERS.matcher(href).matches() && (href.startsWith("http://www.angeloni.com.br/super/"));

    return shouldVisit;
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = internalId;
      String newUrl = internalId != null ? CrawlerUtils.crawlFinalUrl(session.getOriginalURL(), session) : session.getOriginalURL();
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcumb > a:not(:first-child)");
      String name = crawlName(doc);
      Float price = crawlPrice(doc);
      boolean available = price != null;
      String defaultImage = CrawlerUtils.scrapUrl(doc, "meta[property=\"og:image\"]", "content", "https", "img.angeloni.com.br");
      String host = defaultImage != null ? new URI(defaultImage).getHost() : "img.angeloni.com.br";
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".box-galeria img", Arrays.asList("data-zoom-image", "src"), "https", host);
      String secondaryImages = crawlSecondaryImages(doc, host, primaryImage);
      Integer stock = null;
      Marketplace marketplace = new Marketplace();
      String description = crawlDescription(doc, internalId);
      Prices prices = crawlPrices(doc, price);

      Product product = ProductBuilder.create().setUrl(newUrl).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page.");
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document doc) {
    return !doc.select(".container__body-detalhe-produto").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String id = null;

    Element elementInternalId = doc.select("[itemprop=sku]").first();
    if (elementInternalId != null) {
      id = elementInternalId.attr("content").trim();
    } else {
      Element specialId = doc.selectFirst(".content-codigo");

      if (specialId != null) {
        id = CommonMethods.getLast(specialId.ownText().trim().split(" "));
      }
    }


    return id;
  }

  private String crawlName(Document doc) {
    Element elementName = doc.select(".p-relative  h1").first();
    if (elementName != null) {
      return elementName.text().trim();
    }
    return null;
  }

  private Float crawlPrice(Document doc) {
    Float price = null;

    Element elementPrice = doc.selectFirst(".content__desc-prod__box-valores");
    if (elementPrice != null) {
      price = Float.parseFloat(elementPrice.attr("content"));
    }

    return price;
  }

  private String crawlSecondaryImages(Document document, String host, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".swiper-slide.count-slide img");

    for (Element e : imagesElement) {
      String image = e.attr("onclick").trim();

      int x = image.indexOf("('") + 2;
      int y = image.indexOf("',", x);

      image = CrawlerUtils.completeUrl(image.substring(x, y), "https:", host);

      if (!image.equalsIgnoreCase(primaryImage)) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String crawlDescription(Document doc, String internalId) {
    StringBuilder description = new StringBuilder();

    Elements descs = doc.select(".div__box-info-produto");
    for (Element e : descs) {
      description.append(e.html());
    }

    description.append(CrawlerUtils.scrapLettHtml(internalId, session, session.getMarket().getNumber()));

    return description.toString();
  }

  /**
   * Each card has your owns installments Showcase price is price sight
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Element priceFrom = doc.select(".d-block.box-produto__texto-tachado").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.ownText()));
      }

      Map<Integer, Float> installmentsPriceMap = new HashMap<>();
      installmentsPriceMap.put(1, price);

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".box-produto__parcelamento", doc, false);
      if (!pair.isAnyValueNull()) {
        installmentsPriceMap.put(pair.getFirst(), pair.getSecond());
      }

      prices.insertCardInstallment(Card.AMEX.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentsPriceMap);
    }

    return prices;
  }
}
