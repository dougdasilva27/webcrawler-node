package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class ArgentinaMusimundoCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.musimundo.com";

  public ArgentinaMusimundoCrawler(Session session) {
    super(session);
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

      String internalId = crawlInternalId(doc);
      String internalPid = null;
      String name = crawlName(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, doc);
      boolean available = crawlAvailability(doc);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > li", true);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace();

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return doc.select(".main-content > .product").first() != null;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("input[type='hidden']").first();
    if (internalIdElement != null) {
      internalId = (MathUtils.parseInt(internalIdElement.attr("id"))).toString();
    }
    return internalId;
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element primaryImageElement = doc.selectFirst("img#bigImage");

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("src").trim();

      if (!primaryImage.startsWith("http://")) {
        primaryImage = "https://" + primaryImage;
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = doc.select("ul.related-pics > li > table > tbody > tr  > td > a > img");

    for (int i = 1; i < imagesElement.size(); i++) {
      String image = imagesElement.get(i).attr("src").trim();
      secondaryImagesArray.put(image);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element shortDesc = doc.selectFirst("#divDescription");

    if (shortDesc != null) {
      description.append(shortDesc);
    }

    Element techDesc = doc.selectFirst(".technic > table");

    if (techDesc != null) {
      description.append(techDesc);
    }

    // I don't use this anymore because of this link:
    // https://www.musimundo.com/1570~audiotvvideo/1589~televisores/1992~smart-tv/producto~128331~C173466-E134843-televisor--50---smart-tv--50mu6100-4k
    // This link scrap flix info, but doesn't have in html
    // Element mpn = doc.selectFirst("script[data-flix-mpn]");
    // if (mpn != null) {
    // String mpnText = CommonMethods.removeAccents(mpn.attr("data-flix-mpn")).replace(" ", "%20");
    // String url = "https://media.flixcar.com/delivery/js/inpage/4780/f4/mpn/" + mpnText +
    // "?&=4782&=f4&mpn=" + mpnText + "&ssl=1&ext=.js";
    //
    // String script = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, null);
    // final String token = "$(\"#flixinpage_\"+i).inPage";
    //
    // JSONObject productInfo = new JSONObject();
    //
    // if (script.contains(token)) {
    // int x = script.indexOf(token + " (") + token.length() + 2;
    // int y = script.indexOf(");", x);
    //
    // String json = script.substring(x, y);
    //
    // try {
    // productInfo = new JSONObject(json);
    // } catch (JSONException e) {
    // Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    // }
    // }
    //
    // if (productInfo.has("product")) {
    // String id = productInfo.getString("product");
    //
    // String urlDesc =
    // "https://media.flixcar.com/delivery/inpage/show/4780/f4/" + id + "/json?c=jsonpcar4782f4" + id +
    // "&complimentary=0&type=.html";
    // String scriptDesc = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, urlDesc, null,
    // null);
    //
    // if (scriptDesc.contains("({")) {
    // int x = scriptDesc.indexOf("({") + 1;
    // int y = scriptDesc.lastIndexOf("})") + 1;
    //
    // String json = scriptDesc.substring(x, y);
    //
    // try {
    // JSONObject jsonInfo = new JSONObject(json);
    //
    // if (jsonInfo.has("html")) {
    // if (jsonInfo.has("css")) {
    // description.append("<link href=\"" + jsonInfo.getString("css") + "\" media=\"screen\"
    // rel=\"stylesheet\" type=\"text/css\">");
    // }
    //
    // description.append(jsonInfo.get("html").toString().replace("//media", "https://media"));
    // }
    // } catch (JSONException e) {
    // Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    // }
    // }
    // }
    // }

    return description.toString();
  }

  private String crawlName(Document doc) {
    String name = null;
    Element nameElement = doc.selectFirst(".product > hgroup > h1.name");

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private Float crawlPrice(Document doc) {
    Float price = null;

    Element salePriceElement = doc.selectFirst(".prices.online > span.value");

    if (salePriceElement != null) {
      price = MathUtils.parseFloatWithComma(salePriceElement.text());
    }

    return price;
  }

  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setPriceFrom(crawlPriceFrom(doc));

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.NARANJA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }

    return prices;
  }

  private Double crawlPriceFrom(Document doc) {
    Double price = null;

    Element from = doc.select(".prices.cash > span.value").first();
    if (from != null) {
      price = MathUtils.parseDoubleWithComma(from.text());
    }

    return price;
  }

  private boolean crawlAvailability(Document doc) {
    return !doc.select(".button.buy.clickAddCart").isEmpty();
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }

}
