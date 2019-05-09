package br.com.lett.crawlernode.crawlers.corecontent.florianopolis;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
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

public class FlorianopolisBistekCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.bistekonline.com.br/";
  private static final String HOST = "www.bistekonline.com.br";

  public FlorianopolisBistekCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
  }

  @Override
  public void handleCookiesBeforeFetch() {
    this.cookies = CrawlerUtils.fetchCookiesFromAPage("https://www.bistekonline.com.br/store/SetStoreByZipCode?zipCode=88066-000", null, HOST, "/",
        cookies, session, new HashMap<>(), dataFetcher);
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = crawlInternalPid(doc);
      String name = crawlName(doc);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".main-content #lblPrecoPor strong", null, true, ',', session);
      boolean available = price != null;
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = null;
      String description = crawlDescription(doc);
      Prices prices = crawlPrices(doc, price);
      String ean = scrapEan(doc);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).setEans(Arrays.asList(ean)).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page: " + session.getOriginalURL());
    }

    return products;
  }

  private static boolean isProductPage(Document doc) {
    return doc.select("#info-product").first() != null;
  }

  private static String crawlInternalId(Document document) {
    String internalId = null;
    Element internalIdElement = document.selectFirst("#liCodigoInterno #productInternalCode");

    if (internalIdElement != null) {
      internalId = internalIdElement.text().trim();
    }

    return internalId;
  }

  private static String crawlInternalPid(Document document) {
    String internalPid = null;
    Element pid = document.select("#ProdutoCodigo").first();

    if (pid != null) {
      internalPid = pid.val();
    }

    return internalPid;
  }

  private static String crawlName(Document document) {
    String name = null;
    Element nameElement = document.selectFirst(".main-content h1.name.fn");

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private static String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".collum.images #hplAmpliar").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("href").trim();

      if (primaryImage.equals("#")) { // no image for product
        return null;
      }
    }

    return CrawlerUtils.completeUrl(primaryImage, "https", HOST);
  }

  /**
   * @param document
   * @return
   */
  private static CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select("#breadcrumbs span a[href] span");

    for (int i = 1; i < elementCategories.size(); i++) {
      String cat = elementCategories.get(i).ownText().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private static String crawlDescription(Document document) {
    String description = "";
    Element descriptionElement = document.select("#panCaracteristica").first();

    if (descriptionElement != null) {
      description = description + descriptionElement.html();
    }

    return description;
  }

  private String scrapEan(Document doc) {
    String ean = null;

    JSONObject dataLayer = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer.push(", ");", true, true);
    if (dataLayer.has("RKProductEan13")) {
      ean = dataLayer.get("RKProductEan13").toString();
    }

    return ean;
  }

  /**
   * In this market has no bank slip payment method
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".main-content #lblPreco.price-from", null, true, ',', session));

      Element installments = doc.select(".main-content #lblParcelamento").first();

      if (installments != null) {
        Element installmentElement = installments.select("#lblParcelamento1 > strong").first();

        if (installmentElement != null) {
          Integer installment = Integer.parseInt(installmentElement.text().replaceAll("[^0-9]", ""));

          Element valueElement = installments.select("#lblParcelamento2 > strong").first();

          if (valueElement != null) {
            Float value = MathUtils.parseFloatWithComma(valueElement.text());

            installmentPriceMap.put(installment, value);
          }
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    }

    return prices;
  }
}
