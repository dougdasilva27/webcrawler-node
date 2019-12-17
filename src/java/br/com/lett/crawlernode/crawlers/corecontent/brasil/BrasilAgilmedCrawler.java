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
import models.prices.Prices;

public class BrasilAgilmedCrawler extends Crawler {

  public BrasilAgilmedCrawler(Session session) {
    super(session);
  }

  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div[data-produto-id]", "data-produto-id");
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "span[itemprop=\"sku\"]", false);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.nome-produto", true);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#descricao"));
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, 
          ".principal .preco-promocional, b.text-parcelas.pull-right.cor-principal", null, false, ',', session);
      boolean available = doc.selectFirst(".comprar .qtde-adicionar-carrinho") != null;
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs ul li", true);
      Prices prices = crawlPrices(doc, price);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#imagemProduto", Arrays.asList("src"), "https:", "www.cdn.awsli.com.br");
      String secondaryImage = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".produto-thumbs.thumbs-horizontal ul > li:not(:first-child) a", Arrays.asList("data-imagem-grande"), "https:", "www.cdn.awsli.com.br", primaryImage);
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
          .setSecondaryImages(secondaryImage)
          .setDescription(description)
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".produto") != null;
  }

  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();
    Map<Integer, Float> installmentPriceMap = new HashMap<>();

    if (price != null) {
      installmentPriceMap.put(1, price);

      Element priceFromElement = doc.selectFirst(".com-promocao .preco-venda");
      if (priceFromElement != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFromElement.text().trim()));
      }

      Elements pricesElements = doc.select(".parcelas-produto .accordion-inner ul li");
      for (Element element : pricesElements) {
        String priceText = element.text();

        Float priceI = MathUtils.parseFloatWithComma(priceText.substring(priceText.indexOf("R"), priceText.length()));
        Integer quantityInstallments = MathUtils.parseInt(priceText.substring(0, priceText.indexOf("x")));

        installmentPriceMap.put(quantityInstallments, priceI);
      }

      if (!installmentPriceMap.isEmpty()) {
        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      }
    }

    return prices;
  }
}
