package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
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



public class ColombiaExitoCrawler extends Crawler {
  private static final String HOME_PAGE = "https://www.exito.com/";

  public ColombiaExitoCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.WEBDRIVER);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected Object fetch() {
    Document doc = new Document("");
    this.webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), session);

    if (this.webdriver != null) {
      doc = Jsoup.parse(this.webdriver.getCurrentPageSource());

      Element script = doc.select("script").first();
      Element robots = doc.select("meta[name=robots]").first();

      if (script != null && robots != null) {
        String eval = script.html().trim();
        if (!eval.isEmpty()) {
          Logging.printLogDebug(logger, session, "Escution of incapsula js script...");
          this.webdriver.executeJavascript(eval);
        }

        String requestHash = FetchUtilities.generateRequestHash(session);
        this.webdriver.waitLoad(9000);

        doc = Jsoup.parse(this.webdriver.getCurrentPageSource());
        Logging.printLogDebug(logger, session, "Terminating PhantomJS instance ...");
        this.webdriver.terminate();

        // saving request content result on Amazon
        S3Service.uploadCrawlerSessionContentToAmazon(session, requestHash, doc.toString());
      }
    }

    return doc;
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = scrapInternalId(doc, "#pdp .row.product");
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".col-addtocart button.btn", "data-sku");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".row.product .name", true);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".col-image .image #slide-image-pdp .item img", Arrays.asList("data-src"),
          "https:", "www.exito.com");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".col-image .image #slide-image-pdp .item img", Arrays.asList("data-src"),
          "https:", "www.exito.com", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc,
          Arrays.asList(".tabs-pdp [data-target=\"#tabDescription\"]", ".tabs-pdp #tabDescription .tab-content-body",
              ".tabs-pdp [data-target=\"#tabFeature\"]", ".tabs-pdp #tabFeature .tab-content-body", ".tabs-pdp [data-target=\"#tabSpecifications\"]",
              ".tabs-pdp #tabSpecifications .tab-content-body"));
      Integer stock = scrapStock(doc, ".input-group input[data-stock]");
      boolean available = scrapAvaliability(doc, ".col-addtocart button[disabled]");
      Float price = scrapPrice(doc, ".col-price .money");
      Prices prices = scrapPrices(price, doc, ".col-price p");

      String seller = CrawlerUtils.scrapStringSimpleInfo(doc, ".seller-name strong", true);
      // Creating the product

      ProductBuilder product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid)
          .setName(name).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setPrice(price)
          .setDescription(description).setStock(stock);

      if (StringUtils.stripAccents(seller).toLowerCase().trim().equals("exito")) {
        product.setPrices(prices);
      } else {
        product.setPrices(new Prices());
        product.setMarketplace(createMarketplace(seller, prices));
      }

      products.add(product.build());

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst("#pdp") != null;
  }

  private String scrapInternalId(Document doc, String selector) {
    String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, selector, "id");

    if (internalId != null && internalId.startsWith("prd")) {
      internalId = internalId.substring(3);
    }

    return internalId;
  }

  private Integer scrapStock(Document doc, String selector) {
    Element e = doc.selectFirst(selector);
    Integer stock = 0;

    if (e != null) {
      String aux = e.attr("data-stock");

      if (!aux.isEmpty()) {
        stock = Integer.parseInt(aux);
      }
    }

    return stock;
  }

  private Float scrapPrice(Document doc, String selector) {
    Float price = null;

    Elements prices = doc.select(selector);

    if (prices.size() > 2) {
      price = MathUtils.parseFloatWithDots(prices.get(1).ownText().trim());
    } else if (!prices.isEmpty()) {
      price = MathUtils.parseFloatWithDots(prices.get(0).ownText().trim());
    }

    return price;
  }

  private Prices scrapPrices(Float price, Document doc, String selector) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      Elements scrapedPrices = doc.select(selector);
      boolean hasShopCard = false;

      for (Element e : scrapedPrices) {
        if (e.selectFirst("img.payment-card") != null) {
          Map<Integer, Float> shopcard = new TreeMap<>();
          shopcard.put(1, MathUtils.parseFloatWithDots(e.selectFirst("span").text().trim()));

          prices.insertCardInstallment(Card.SHOP_CARD.toString(), shopcard);
          hasShopCard = true;
        }

        if (e.hasClass("before")) {
          prices.setPriceFrom(MathUtils.normalizeTwoDecimalPlaces(MathUtils.parseFloatWithDots(e.text().trim()).doubleValue()));
        }
      }

      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

      if (!hasShopCard)
        prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
    }

    return prices;
  }

  private boolean scrapAvaliability(Document doc, String selector) {
    return doc.selectFirst(selector) == null;
  }

  private Marketplace createMarketplace(String name, Prices prices) {
    Map<String, Prices> mktp = new HashMap<String, Prices>();
    mktp.put(name, prices);
    List<String> nm = Arrays.asList(name.toLowerCase());

    return CrawlerUtils.assembleMarketplaceFromMap(mktp, nm, Card.AMEX, session);
  }
}
