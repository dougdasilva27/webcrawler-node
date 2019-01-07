package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class BrasilAgilmedCrawler extends Crawler {

  public BrasilAgilmedCrawler(Session session) {
    super(session);
  }

  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session,
          "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = crawInternalPid(doc);

      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.nome-produto", true);

      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#descricao"));

      Float price = crawlPrice(doc);

      boolean available = crawlAvailability(doc);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs ul li", true);
      Prices prices = crawlPrices(doc);

      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#imagemProduto",
          Arrays.asList("src"), "https:", "www.cdn.awsli.com.br");

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

  private String crawInternalPid(Document doc) {
    Element internalPidElement = doc.selectFirst("span[itemprop=\"sku\"]");
    String internalPid = null;

    if (internalPidElement != null) {
      internalPid = internalPidElement.text().trim();
    }

    return internalPid;
  }

  private Float crawlPrice(Document doc) {
    Element priceElement = doc.selectFirst("meta[itemprop=\"price\"]");
    Float price = null;

    if (priceElement != null) {
      price = MathUtils.parseFloatWithDots(priceElement.attr("content").trim());
    }

    return price;
  }

  private Prices crawlPrices(Document doc) {
    Prices prices = new Prices();
    Map<Integer, Float> installmentPriceMap = new HashMap<>();

    Elements pricesElements = doc.select(".parcelas-produto .accordion-inner ul li");
    Element priceElement = doc.selectFirst(".parcelas-produto .text-parcelas");

    if (pricesElements != null && !pricesElements.isEmpty()) {
      for (Element element : pricesElements) {
        String priceText = element.text();

        Float price = MathUtils
            .parseFloatWithComma(priceText.substring(priceText.indexOf("R"), priceText.length()));
        Integer quantityInstallments =
            MathUtils.parseInt(priceText.substring(0, priceText.indexOf("x")));

        installmentPriceMap.put(quantityInstallments, price);
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

    } else if (priceElement != null) {
      Float price = MathUtils.parseFloatWithComma(priceElement.text().trim());

      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    }

    return prices;
  }

  private boolean crawlAvailability(Document doc) {
    Element avaibleElement = doc.selectFirst(".disponibilidade-produto b");
    boolean availability = false;

    if (avaibleElement != null) {
      availability = avaibleElement.text().trim().equalsIgnoreCase("Disponível");
    }

    return availability;
  }


  private String crawlInternalId(Document doc) {
    Element internalIdElement = doc.selectFirst("div[data-produto-id]");
    String internalId = null;

    if (internalIdElement != null) {
      internalId = internalIdElement.attr("data-produto-id").trim();
    }

    return internalId;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".produto") != null;
  }

}
