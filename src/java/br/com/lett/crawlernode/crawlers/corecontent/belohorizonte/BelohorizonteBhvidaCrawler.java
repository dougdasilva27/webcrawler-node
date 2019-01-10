package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class BelohorizonteBhvidaCrawler extends Crawler {

  public BelohorizonteBhvidaCrawler(Session session) {
    super(session);
    // TODO Auto-generated constructor stub
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session,
          "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = crawlInternalPid(doc);
      boolean available = crawlAvailability(doc);

      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#detalhes-mini > ul > li h1", true);
      Float price =
          available ? CrawlerUtils.scrapSimplePriceFloat(doc, ".preco strong", true) : null;
      CategoryCollection categories =
          CrawlerUtils.crawlCategories(doc, ".produtos a:not(:first-child)");

      Prices prices = crawlPrices(price, doc);
      String description = crawlDescription(doc);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "img", Arrays.asList("src"),
          "https:", "www.bhvida.com/");


      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
          .setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage).setSecondaryImages(null).setDescription(description)
          .setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private String crawlDescription(Document doc) {
    List<String> selectors = new ArrayList<>();
    selectors.add("#detalhes-mini ul li p");
    selectors.add("#detalhes-mini ul li  .bloco p");
    selectors.add("#detalhes-mini ul li  .bloco table");

    return CrawlerUtils.scrapSimpleDescription(doc, selectors);
  }

  private boolean isProductPage(Element doc) {
    return !doc.select(".det_carrinho").isEmpty();
  }

  private String crawlInternalPid(Element doc) {
    String internalPid = null;

    Element searchedId = doc.selectFirst("#detalhes-mini > ul > li:last-child");
    if (searchedId != null) {
      internalPid = searchedId.ownText();
    }

    return internalPid;
  }

  private String crawlInternalId(Element doc) {
    String internalId = null;

    Element searchedId = doc.selectFirst("#frmcarrinho #codigo");

    if (searchedId != null) {
      internalId = searchedId.val().trim();
    } else {
      searchedId = doc.selectFirst(".aviseme button");
      if (searchedId != null) {
        String input = searchedId.attr("onclick");

        if (input.contains("'")) {
          internalId = input.substring(input.indexOf("'") + 1, input.lastIndexOf("'"));

        }

      }
    }

    return internalId;
  }

  private boolean crawlAvailability(Element doc) {
    return !doc.select("#b_estq").isEmpty();
  }

  /**
   * In the time when this crawler was made, this market hasn't installments informations
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Element doc) {
    Prices prices = new Prices();
    Element offer = doc.selectFirst(".preco .oferta");

    if (price != null) {
      Map<Integer, Float> installmentPriceMapShop = new HashMap<>();
      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      installmentPriceMap.put(1, price);

      Elements elements = doc.select(".parcelamento ul > li");

      for (Element element : elements) {
        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, element, true);

        if (!pair.isAnyValueNull()) {
          installmentPriceMapShop.put(pair.getFirst(), pair.getSecond());
        }
      }

      if (offer != null) {
        prices.setPriceFrom(CrawlerUtils.scrapSimplePriceDouble(doc, ".preco .oferta", false));
      }

      if (crawlAvailability(doc)) {
        prices.setBankTicketPrice(price);
        prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMapShop);
      }

    }

    return prices;
  }
}
