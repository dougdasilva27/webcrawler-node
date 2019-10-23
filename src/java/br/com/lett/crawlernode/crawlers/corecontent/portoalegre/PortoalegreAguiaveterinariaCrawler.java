package br.com.lett.crawlernode.crawlers.corecontent.portoalegre;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

public class PortoalegreAguiaveterinariaCrawler extends Crawler {

  private static final String INTERNALPID_ID = "varproduto_id='";
  private static final String INTERNALID_ID = "vargrade_id='";

  public PortoalegreAguiaveterinariaCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    String idsScript = scrapScripWithIds(doc);

    if (idsScript != null) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = CrawlerUtils.extractSpecificStringFromScript(idsScript, INTERNALPID_ID, false, "';", false);
      CategoryCollection categories = new CategoryCollection();
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".single-product-gallery-item a", Arrays.asList("href"), "https:",
          "www.aguiaveterinaria.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-sku-image .image-highlight a.main-product img",
          Arrays.asList("data-zoom-image", "src"), "https", "www.aguiaveterinaria.com.br", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#description"));

      Elements variationsElements = doc.select("#variacao_ul li");
      if (!variationsElements.isEmpty()) {
        for (Element variationElement : variationsElements) {
          String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(variationElement, "input[value]", "value");
          String name = CrawlerUtils.scrapStringSimpleInfo(variationElement, "> label", true);
          Float price = scrapPriceVariation(variationElement);
          boolean available = price != null;
          Prices prices = scrapPrices(variationElement, price);

          // Creating the product
          Product product = ProductBuilder.create()
              .setUrl(session.getOriginalURL())
              .setInternalId(internalId)
              .setInternalPid(internalPid)
              .setName(name)
              .setPrice(price)
              .setPrices(prices)
              .setAvailable(available)
              .setCategory1(categories.getCategory(0))
              .setCategory2(categories.getCategory(1))
              .setCategory3(categories.getCategory(2))
              .setPrimaryImage(primaryImage)
              .setSecondaryImages(secondaryImages)
              .setDescription(description)
              .setMarketplace(new Marketplace())
              .build();

          products.add(product);
        }
      } else {
        String internalId = CrawlerUtils.extractSpecificStringFromScript(idsScript, INTERNALID_ID, false, "';", false);
        String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1[itemprop=\"name\"]", true);
        boolean available = doc.selectFirst(".add-cart-holder") != null;
        Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".final_price", "content", false, ',', session);
        Prices prices = scrapPrices(doc, price);

        // Creating the product
        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrice(price)
            .setPrices(prices)
            .setAvailable(available)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setMarketplace(new Marketplace())
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private Float scrapPriceVariation(Element variationElement) {
    Float price = CrawlerUtils.scrapFloatPriceFromHtml(variationElement, "input", "data-promo", true, '.', session);

    if (price == null || price <= 0f) {
      price = CrawlerUtils.scrapFloatPriceFromHtml(variationElement, "input", "data-preco", true, '.', session);
    }

    return price;
  }

  private String scrapScripWithIds(Document doc) {
    String script = null;

    Elements scripts = doc.select("script");
    for (Element e : scripts) {
      String html = e.html().replace(" ", "");

      if (html.contains(INTERNALPID_ID) && html.contains(INTERNALID_ID)) {
        script = html;
        break;
      }
    }

    return script;
  }

  private Prices scrapPrices(Element doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "input[data-promo]", "data-promo", true, '.', session);
      if (priceFrom == null || priceFrom <= 0f) {
        priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "input[data-promo]", "data-preco", true, '.', session);
      }

      prices.setPriceFrom(priceFrom != null ? priceFrom : CrawlerUtils.scrapDoublePriceFromHtml(doc, ".previous-price", null, true, ',', session));

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }
}
