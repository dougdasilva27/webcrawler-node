package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
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
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 08/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class ArgentinaRibeiroCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.ribeiro.com.ar/";

  public ArgentinaRibeiroCrawler(Session session) {
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
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_name", true);
      Prices prices = crawlPrices(doc);
      Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);

      boolean available = !doc.select("#tableComprarButtom .atg_store_productAvailability").isEmpty();
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#atg_store_breadcrumbs_mod li:not(:first-child) > a", false);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".atg_store_productImage #imgAux > a", Arrays.asList("data-zoom-image", "src"),
          "https:", "minicuotas.ribeiro.com.ar");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".atg_store_productImage #imgAux > a",
          Arrays.asList("data-zoom-image", "data-image"), "https:", "minicuotas.ribeiro.com.ar", primaryImage);
      String description = crawlDescription(doc);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select("#atg_store_main").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Elements scripts = doc.select("script[type=\"text/javascript\"]");
    for (Element e : scripts) {
      String script = e.html().replace(" ", "").toLowerCase();

      if (script.contains("varproductid=")) {
        internalId = CrawlerUtils.extractSpecificStringFromScript(script, "varproductid=", ";", false).replace("\"", "").trim();
        break;
      }
    }
    return internalId;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();
    description.append(CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".atg_store_productDescription", "#ContenedorDescripciones")));

    Element ean = doc.selectFirst("#ArtEan");
    if (ean != null) {
      description.append(crawlDescriptionFromFlixMedia(ean.val().trim(), session));
    }

    return description.toString();
  }

  public static String crawlDescriptionFromFlixMedia(String ean, Session session) {
    StringBuilder description = new StringBuilder();

    if (!ean.isEmpty()) {

      String url = "https://media.flixcar.com/delivery/js/inpage/4782/f4/40/ean/" + ean + "?&=4782&=f4&ean=" + ean + "&ssl=1&ext=.js";

      String script = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, null);
      final String token = "$(\"#flixinpage_\"+i).inPage";

      JSONObject productInfo = new JSONObject();

      if (script.contains(token)) {
        int x = script.indexOf(token + " (") + token.length() + 2;
        int y = script.indexOf(");", x);

        String json = script.substring(x, y);

        try {
          productInfo = new JSONObject(json);
        } catch (JSONException e) {
          Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
        }
      }

      if (productInfo.has("product")) {
        String id = productInfo.getString("product");

        String urlDesc =
            "https://media.flixcar.com/delivery/inpage/show/4782/f4/" + id + "/json?c=jsonpcar4782f4" + id + "&complimentary=0&type=.html";
        String scriptDesc = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, urlDesc, null, null);

        if (scriptDesc.contains("({")) {
          int x = scriptDesc.indexOf("({") + 1;
          int y = scriptDesc.lastIndexOf("})") + 1;

          String json = scriptDesc.substring(x, y);

          try {
            JSONObject jsonInfo = new JSONObject(json);

            if (jsonInfo.has("html")) {
              if (jsonInfo.has("css")) {
                description.append("<link href=\"" + jsonInfo.getString("css") + "\" media=\"screen\" rel=\"stylesheet\" type=\"text/css\">");
              }

              description.append(jsonInfo.get("html").toString().replace("//media", "https://media"));
            }
          } catch (JSONException e) {
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
          }
        }
      }
    }

    return description.toString();
  }

  /**
   * In the time when this crawler was made, this market hasn't installments informations
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc) {
    Prices prices = new Prices();

    Map<Integer, Float> installmentPriceMap = new TreeMap<>();

    Element priceFrom = doc.selectFirst("div[itemprop=offers] .precio_big_indivGris");
    if (priceFrom != null) {
      prices.setPriceFrom(MathUtils.parseDoubleWithDot(priceFrom.ownText()));
    }

    Elements parcels = doc.select("#planLista li");
    for (Element e : parcels) {
      Element installment = e.selectFirst("input[id^=cantCuotas]");
      Element installmentValue = e.selectFirst("input[id^=pxcuota]");

      if (installment != null && installmentValue != null) {
        String textInstallment = installment.val().replaceAll("[^0-9]", "").trim();
        String textInstallmentValue = installmentValue.val().replaceAll("[^0-9.]", "").trim();

        if (!textInstallment.isEmpty() && !textInstallmentValue.isEmpty()) {
          installmentPriceMap.put(Integer.parseInt(textInstallment), Float.parseFloat(textInstallmentValue));
        }
      }
    }

    prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

    return prices;
  }

}
