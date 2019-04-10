package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.json.JSONObject;
import org.jsoup.Jsoup;
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
import models.Marketplace;
import models.prices.Prices;

/**
 * date: 02/04/2019
 * 
 * @author gabriel
 *
 */
public class BrasilGazinCrawler extends Crawler {

  private static final String PROTOCOL = "https";
  private static final String HOST = "www.gazin.com.br";
  private static final String HOME_PAGE = PROTOCOL + "://" + HOST;

  public BrasilGazinCrawler(Session session) {
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

      JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", "", null, false, false);

      String internalPid = jsonInfo.has("sku") ? jsonInfo.get("sku").toString() : null;
      String name = jsonInfo.has("name") ? jsonInfo.get("name").toString() : null;
      Float price = crawlPrice(jsonInfo);
      Prices prices = crawlPrices(price, doc);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#brd-crumbs li:not(:first-child) > a");
      String description = crawlDescription(doc);
      String primaryImageMain = CrawlerUtils.scrapSimplePrimaryImage(doc, ".conteudopreco .FotoMenor a[href]", Arrays.asList("href"), PROTOCOL, HOST);
      String secondaryImagesMain =
          CrawlerUtils.scrapSimpleSecondaryImages(doc, ".conteudopreco .FotoMenor a", Arrays.asList("href"), PROTOCOL, HOST, primaryImageMain);
      List<String> eans = scrapEans(jsonInfo);

      Map<String, String> skus = scrapVariations(doc);
      if (skus.isEmpty()) {
        String internalId = internalPid;
        String primaryImage = primaryImageMain;
        String secondaryImages = secondaryImagesMain;
        boolean available = crawlAvailability(jsonInfo);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setEans(eans).setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages).setDescription(description).setMarketplace(new Marketplace()).build();

        products.add(product);
      } else {
        for (Entry<String, String> entry : skus.entrySet()) {
          String variationId = entry.getKey();
          String variationName = entry.getValue() != null ? (name + " " + entry.getValue()).trim() : name;
          String internalId = internalPid + "-" + variationId;
          String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".conteudopreco div[id~=" + entry.getKey() + "] .FotoMenor a",
              Arrays.asList("href"), PROTOCOL, HOST);

          if (primaryImage == null) {
            primaryImage = primaryImageMain;
          }

          String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".conteudopreco div[id~=" + entry.getKey() + "] .FotoMenor a",
              Arrays.asList("href"), PROTOCOL, HOST, primaryImage);

          if (secondaryImages == null) {
            secondaryImages = secondaryImagesMain;
          }

          boolean available = crawlAvailabilityVariation(doc, variationId);

          // Creating the product
          Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid)
              .setName(variationName).setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setEans(eans)
              .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
              .setSecondaryImages(secondaryImages).setDescription(description).setMarketplace(new Marketplace()).build();

          products.add(product);
        }
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".conteudopreco").isEmpty();
  }

  private List<String> scrapEans(JSONObject json) {
    List<String> eans = new ArrayList<>();

    if (json.has("mpn")) {
      eans = Arrays.asList(json.getString("mpn").split(","));
    }

    return eans;
  }

  private Float crawlPrice(JSONObject json) {
    Float price = null;

    if (json.has("offers")) {
      JSONObject value = json.getJSONObject("offers");
      if (value.has("@type") && value.getString("@type").equalsIgnoreCase("offer")) {
        price = value.getFloat("price");
      }
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject json) {
    boolean availability = false;

    if (json.has("offers")) {
      JSONObject value = json.getJSONObject("offers");
      if (value.has("@type") && value.getString("@type").equalsIgnoreCase("offer") && value.has("availability")) {
        availability = value.get("availability").toString().equalsIgnoreCase("in stock");
      }
    }

    return availability;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();
    Element shortDescription = document.selectFirst(".conteudopreco > .InfoProd");

    if (shortDescription != null) {
      shortDescription.select("> .InfoProd").remove();
      shortDescription.select("> .InfoProd2").remove();
      description.append(shortDescription.html());
    }

    return description.toString();
  }

  /**
   * There is no bankSlip price.
   * 
   * Some cases has this: 6 x $259.83
   * 
   * Only card that was found in this market was the market's own
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(CrawlerUtils.scrapFloatPriceFromHtml(doc, "#navpa .Menupa .bcaa b", null, true, ',', session));

      Elements parcels = doc.select("#navpa .Menupa .med > p.p1b");
      Elements parcelsValues = doc.select("#navpa .Menupa .med > p.p2b");

      if (parcels.size() == parcelsValues.size()) {
        for (int i = 0; i < parcels.size(); i++) {
          Integer parcel = CrawlerUtils.scrapIntegerFromHtml(parcels.get(i), null, true, null);
          Float value = CrawlerUtils.scrapFloatPriceFromHtml(parcelsValues.get(i), null, null, true, ',', session);

          if (parcel != null && value != null) {
            installmentPriceMap.put(parcel, value);
          }
        }
      }

      if (prices.getBankTicketPrice() == null) {
        prices.setBankTicketPrice(price);
      }

      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }

    return prices;
  }

  /**
   * Scrap variatons from html and returns a map with key internalId and value variationName
   * 
   * @param doc
   * @return
   */
  private Map<String, String> scrapVariations(Document doc) {
    Map<String, String> skus = new HashMap<>();

    Elements variations = doc.select(".conteudopreco .ciq > div > div[id]");
    for (Element e : variations) {
      skus.put(e.id(), CrawlerUtils.scrapStringSimpleInfo(e, "a > span", true));
    }

    Elements variationsSpecial = doc.select(".conteudopreco .ciq select > option[class]");
    for (Element e : variationsSpecial) {
      skus.put(e.val(), e.ownText());
    }

    return skus;
  }

  private boolean crawlAvailabilityVariation(Document doc, String id) {
    boolean availability = false;
    String token = "if(a.value == \"" + id + "\")";
    String token2 = "document.getelementbyid('estoque').innerhtml = '";

    Elements scripts = doc.select(".conteudo > div > script");
    for (Element e : scripts) {
      String html = e.outerHtml().toLowerCase();

      if (html.contains(token) && html.contains(token2)) {
        int x = html.indexOf(token) + token.length();
        int y = html.indexOf("';", x);

        String function = html.substring(x, y);

        Element script = Jsoup.parse(function.substring(function.indexOf("= '") + 3).trim());
        availability = !script.select("input").isEmpty();

        break;
      }
    }

    return availability;
  }
}
