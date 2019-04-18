package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
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
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (25/08/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 * 
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) Even if a product is unavailable, its price is displayed.
 * 
 * 6) There is internalPid for skus in this ecommerce.
 * 
 * 7) The primary image is the first image in the secondary images selector.
 * 
 * Examples: ex1 (available):
 * http://www.arcondicionado.com.br/produto/ar-condicionado-split-9000-btus-frio-220v-lg-smile-ts-c092tnw6-68635
 * ex2 (unavailable):
 * http://www.arcondicionado.com.br/produto/cortina-de-ar-90-cm-220v-springer-acs09s5-68690
 *
 * Optimizations notes: No optimizations.
 *
 ************************************************************************************************************************************************************************************/
public class BrasilArcondicionadoCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.arcondicionado.com.br/";
  private static final String HOME_PAGE_ADIAS = "https://www.adias.com.br/";

  public BrasilArcondicionadoCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE) || href.startsWith(HOME_PAGE_ADIAS));
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONArray productsLista = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "AddProdutos(eval(", "))", true, false);

      String internalId = crawlInternalId(doc, productsLista);
      String internalPid = crawlInternalPid(doc, productsLista);
      String name = crawlName(doc);
      boolean available = crawlAvailability(doc);
      Float price = crawlMainPagePrice(doc, available);
      Prices prices = crawlPrices(doc, available);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#fbits-breadcrumb span[itemprop=name]", true);

      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#galeria li > a", Arrays.asList("data-zoom-image", "data-image"), "https",
          "arcondicionado.fbitsstatic.net");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#galeria li > a", Arrays.asList("data-zoom-image", "data-image"),
          "https", "arcondicionado.fbitsstatic.net", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".infoProd", ".listacompra-descricao"));

      String url = session.getOriginalURL();
      if (this.session.getRedirectedToURL(url) != null && name != null) {
        url = this.session.getRedirectedToURL(url);
      }

      // Creating the product
      Product product = ProductBuilder.create().setUrl(url).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }



  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document doc) {
    return !doc.select(".detalhe-produto").isEmpty();
  }


  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(Document doc, JSONArray products) {
    String internalId = null;
    Element internalIdElement = doc.select("#hdnProdutoVarianteId").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.val().trim();
    } else {
      StringBuilder ids = new StringBuilder();
      for (Object o : products) {
        JSONObject product = (JSONObject) o;

        if (product.has("variante") && !product.isNull("variante")) {
          if (ids.toString().isEmpty()) {
            ids.append(product.get("variante").toString());
          } else {
            ids.append("-").append(product.get("variante").toString());
          }
        }
      }

      if (!ids.toString().isEmpty()) {
        internalId = ids.toString();
      }
    }

    return internalId;
  }

  private String crawlInternalPid(Document doc, JSONArray products) {
    String internalPid = null;

    Element internalPidElement = doc.select("#hdnProdutoId").first();

    if (internalPidElement != null) {
      internalPid = internalPidElement.val().trim();
    } else {
      StringBuilder ids = new StringBuilder();
      for (Object o : products) {
        JSONObject product = (JSONObject) o;

        if (product.has("id") && !product.isNull("id")) {
          if (ids.toString().isEmpty()) {
            ids.append(product.get("id").toString());
          } else {
            ids.append("-").append(product.get("id").toString());
          }
        }
      }


      if (!ids.toString().isEmpty()) {
        internalPid = ids.toString();
      }
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".prodTitle").first();

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private Float crawlMainPagePrice(Document doc, boolean available) {
    Float price = null;

    if (available) {
      Element specialPrice = doc.select(".produtoInfo .precoPor").first();

      if (specialPrice == null) {
        specialPrice = doc.select("#fbits-forma-pagamento .precoPor").first();
      }

      if (specialPrice != null) {
        price = Float.parseFloat(specialPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
      }

      if (price == null) {
        price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".detalhe-produto .fbits-boleto-preco", null, true, ',', session);
      }
    }

    return price;
  }

  private Prices crawlPrices(Document document, boolean available) {
    Prices prices = new Prices();

    if (available) {
      Float bankTicketPrice = null;
      Map<Integer, Float> installments = new TreeMap<>();

      // bank ticket
      Element bankTicketPriceElement = document.select(".detalhe-produto .fbits-boleto-preco").last();
      if (bankTicketPriceElement != null) {
        bankTicketPrice = MathUtils.parseFloatWithComma(bankTicketPriceElement.text());
      }

      Elements installmentsElements = document.select(".fbits-parcelamento-padrao .details .details-content p");
      if (!installmentsElements.isEmpty()) {
        for (Element installmentElement : installmentsElements) {
          Integer installmentNumber = null;
          Float installmentPrice = null;

          Element installmentNumberElement = installmentElement.select("b").first();
          if (installmentNumberElement != null) {
            installmentNumber = Integer.parseInt(installmentNumberElement.text().trim());
          }

          Element installmentPriceElement = installmentElement.select("b").last();
          if (installmentPriceElement != null) {
            installmentPrice = MathUtils.parseFloatWithComma(installmentPriceElement.text());
          }

          installments.put(installmentNumber, installmentPrice);
        }
      } else {
        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".fbits-parcelamento .precoParcela", document, true, "x", "", true);
        if (!pair.isAnyValueNull()) {
          Integer installmentNumber = pair.getFirst();
          Float installmentValue = pair.getSecond();

          installments.put(1, MathUtils.normalizeTwoDecimalPlaces(installmentNumber * installmentValue));
          installments.put(installmentNumber, installmentValue);
        }
      }

      prices.setBankTicketPrice(bankTicketPrice);

      prices.insertCardInstallment(Card.VISA.toString(), installments);
      prices.insertCardInstallment(Card.AMEX.toString(), installments);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
      prices.insertCardInstallment(Card.DINERS.toString(), installments);
      prices.insertCardInstallment(Card.ELO.toString(), installments);
      prices.insertCardInstallment(Card.DISCOVER.toString(), installments);
      prices.insertCardInstallment(Card.BNDES.toString(), installments);
    }

    return prices;
  }

  private boolean crawlAvailability(Document document) {
    return document.selectFirst(".avisoIndisponivel:not([style])") == null;
  }
}
