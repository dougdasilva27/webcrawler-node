package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
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

public class BrasilNagemCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.nagem.com.br/";

  public BrasilNagemCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = internalId;
      String name = crawlName(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, internalId, doc);
      boolean available = crawlAvailability(price);
      CategoryCollection categories = crawlCategories(doc);
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
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    if (doc.select(".bg_conteudo-produto").first() != null) {
      return true;
    }
    return false;
  }

  private String crawlInternalId(Document document) {
    String internalId = null;

    Element internalIdElement = document.select("#codigoproduto").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    }

    return internalId;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("h1.produto-descricao").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;
    Element salePriceElement = document.select(".precoPOR-detalhe").first();

    if (salePriceElement != null) {
      String priceText = salePriceElement.ownText();

      if (!priceText.isEmpty()) {
        price = MathUtils.parseFloatWithComma(priceText);
      }
    }

    return price;
  }

  private boolean crawlAvailability(Float price) {
    boolean available = false;

    if (price != null) {
      available = true;
    }

    return available;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".active.carousel-item img").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("src").trim();

      if (!primaryImage.startsWith(HOME_PAGE)) {
        primaryImage = (HOME_PAGE + primaryImage).replace("br//", "br/");
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".carousel-item:not(.active) img");

    for (Element e : imagesElement) { // first index is the primary image
      String image = e.attr("src").trim();

      if (!image.startsWith(HOME_PAGE)) {
        image = (HOME_PAGE + image).replace("br//", "br/");
      }

      secondaryImagesArray.put(image);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();

    Elements elementCategories = document.select(".breadcrumb > a");
    for (Element e : elementCategories) {
      categories.add(e.ownText().trim());
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();
    Element descriptionElement = document.select("#detalhe0").first();
    Element especificationElement = document.select("#especificacoes0").first();

    if (descriptionElement != null) {
      Element detalheNagem = document.select("#detalhe-nagem").first();
      Element eanE = document.select("script[data-flix-inpage=flix-inpage][data-flix-ean]").first();

      if (eanE != null && detalheNagem != null && !detalheNagem.html().trim().isEmpty()) {
        String ean = eanE.attr("data-flix-ean");
        String flixMediaDesc = CrawlerUtils.crawlDescriptionFromFlixMedia("4154", ean, session);

        description.append(flixMediaDesc);
      } else if (detalheNagem != null) {
        description.append(detalheNagem.html());
      }



      if (!descriptionElement.text().trim().isEmpty()) {
        description.append(descriptionElement.html());
      }
    }

    if (especificationElement != null) {
      description.append(especificationElement.html());
    }

    return description.toString();
  }

  /**
   * There is no bankSlip price.
   * 
   * 1x de 739,00
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, String internalId, Document docPrincipal) {
    Prices prices = new Prices();

    if (price != null) {
      Element boleto = docPrincipal.select(".valor_boleto strong").first();

      if (boleto != null) {
        prices.setBankTicketPrice(MathUtils.parseFloatWithComma(boleto.ownText()));
      }

      String urlParameters = "codigoProduto=" + internalId;

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

      Document doc = Jsoup.parse(
          POSTFetcher.fetchPagePOSTWithHeaders("http://www.nagem.com.br/produto/pagamentoproduto", session, urlParameters, cookies, 1, headers));

      Elements cards = doc.select(".modal-body > ul.nav-tabs > li > a");

      for (Element e : cards) {
        Map<Integer, Float> installmentPriceMap = new TreeMap<>();

        String card = e.ownText().toLowerCase().trim();
        String element = e.attr("href");

        switch (card) {
          case "visa":
            card = Card.VISA.toString();
            break;

          case "mastercard":
            card = Card.MASTERCARD.toString();
            break;

          case "diners":
            card = Card.DINERS.toString();
            break;

          case "american express":
            card = Card.AMEX.toString();
            break;

          case "hipercard":
            card = Card.HIPERCARD.toString();
            break;

          case "elo":
            card = Card.ELO.toString();
            break;

          default:
            break;
        }

        Elements installments = doc.select(element + " tr");

        for (Element installment : installments) {
          Element firstValue = installment.select("td").first();

          if (firstValue != null) {
            String text = firstValue.ownText().toLowerCase();

            if (text.contains("x")) {
              Integer installmentNumber = Integer.parseInt(text.split("x")[0].replaceAll("[^0-9]", ""));
              Float installmentValue = MathUtils.parseFloatWithComma(text.split("x")[1]);

              installmentPriceMap.put(installmentNumber, installmentValue);
            }
          }
        }

        prices.insertCardInstallment(card, installmentPriceMap);
      }
    }

    return prices;
  }
}
