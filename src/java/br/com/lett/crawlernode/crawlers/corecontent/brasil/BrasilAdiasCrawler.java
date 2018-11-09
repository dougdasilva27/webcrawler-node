package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (08/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 * 
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking the URL format.
 * 
 * 5) Even if a product is unavailable, its price is displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is
 * the same for all the variations of a given sku.
 * 
 * 7) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples: ex1 (available):
 * http://www.adias.com.br/produto/ar-condicionado-split-janela-9000-btus-elgin-compact-110v-frio-hcfi09a1na-68152
 * ex2 (unavailable):
 * http://www.adias.com.br/produto/ar-condicionado-split-janela-7000-btus-elgin-compact-220v-frio-sqfic-7000-2-68221
 *
 * Optimizations notes: No optimizations
 *
 ************************************************************************************************************************************************************************************/

public class BrasilAdiasCrawler extends Crawler {

  private final String HOME_PAGE = "https://www.adias.com.br/";

  public BrasilAdiasCrawler(Session session) {
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

    if (isProductPage(this.session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // InternalId
      String internalId = crawlInternalId(doc);

      // Pid
      String internalPid = crawlInternalPid(doc);

      // Name
      String name = crawlName(doc);

      // Price
      Float price = crawlMainPagePrice(doc);

      // Price options
      Prices prices = crawlPrices(doc, internalId);

      // Availability
      boolean available = crawlAvailability(doc);

      // Categories
      ArrayList<String> categories = crawlCategories(doc);
      String category1 = getCategory(categories, 0);
      String category2 = getCategory(categories, 1);
      String category3 = getCategory(categories, 2);

      // Primary image
      String primaryImage = crawlPrimaryImage(doc);

      // Secondary images
      String secondaryImages = crawlSecondaryImages(doc, primaryImage);

      // Description
      String description = crawlDescription(doc);

      // Stock
      Integer stock = null;

      // Marketplace map
      Map<String, Float> marketplaceMap = crawlMarketplace(doc);

      // Marketplace
      Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

      // Creating the product
      Product product = new Product();
      product.setUrl(this.session.getOriginalURL());
      product.setInternalId(internalId);
      product.setInternalPid(internalPid);
      product.setName(name);
      product.setPrice(price);
      product.setPrices(prices);
      product.setAvailable(available);
      product.setCategory1(category1);
      product.setCategory2(category2);
      product.setCategory3(category3);
      product.setPrimaryImage(primaryImage);
      product.setSecondaryImages(secondaryImages);
      product.setDescription(description);
      product.setStock(stock);
      product.setMarketplace(marketplace);

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url) {
    if (url.startsWith(HOME_PAGE + "produto/")) {
      return true;
    }
    return false;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(Document document) {
    String internalId = null;
    Element internalIdElement = document.select("#hdnProdutoId").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.attr("value").toString().trim();
    }

    return internalId;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".fbits-produto-nome.prodTitle.title").first();

    if (nameElement != null) {
      name = nameElement.text().toString().trim();
    }

    return name;
  }

  private Float crawlMainPagePrice(Document document) {
    Float price = null;
    Element mainPagePriceElement = document.select("#fbits-forma-pagamento .precoPor").first();

    if (mainPagePriceElement != null) {
      price = Float.parseFloat(mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    }

    return price;
  }

  private Prices crawlPrices(Document doc, String internalId) {
    Prices prices = new Prices();

    // crawl the bank ticket price
    Float bankTicketPrice = null;
    Element bankTicketPriceElement = doc.select("#divFormaPagamento .precoVista .fbits-boleto-preco").first();
    if (bankTicketPriceElement != null) {
      bankTicketPrice = Float.parseFloat(bankTicketPriceElement.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    }

    // crawl the card payment options
    Map<Integer, Float> installments = new TreeMap<Integer, Float>();
    Elements installmentsElements = doc.select("#produto-pagamentoparcelamento-" + internalId + " .details-content p");
    if (installmentsElements.size() > 0) {
      for (Element installmentElement : installmentsElements) {
        Element installmentNumberElement = installmentElement.select("b").first();
        Element installmentValueElement = installmentElement.select("b").last();
        Integer installmentNumber = null;
        Float installmentValue = null;

        if (installmentNumberElement != null) {
          installmentNumber = Integer.parseInt(installmentNumberElement.text().trim());
        }
        if (installmentValueElement != null) {
          installmentValue =
              Float.parseFloat(installmentValueElement.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
        }

        // insert the installment on the map
        if (installmentNumber != null && installmentValue != null) {
          installments.put(installmentNumber, installmentValue);
        }
      }
    }

    // insert the payment options

    // bank ticket
    prices.setBankTicketPrice(bankTicketPrice);

    // all card payment options are the same for all card brands
    // the available card brands are displayed once we hit the buy button
    prices.insertCardInstallment(Card.VISA.toString(), installments);
    prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
    prices.insertCardInstallment(Card.ELO.toString(), installments);
    prices.insertCardInstallment(Card.DINERS.toString(), installments);
    prices.insertCardInstallment(Card.DISCOVER.toString(), installments);
    prices.insertCardInstallment(Card.BNDES.toString(), installments);
    prices.insertCardInstallment(Card.AMEX.toString(), installments);

    return prices;
  }

  private boolean crawlAvailability(Document document) {
    Element notifyMeElement = document.select(".avisoIndisponivel").first();

    if (notifyMeElement != null) {
      if (notifyMeElement.attr("style").equals("display:none;"))
        return true;
    }

    return false;
  }

  private Map<String, Float> crawlMarketplace(Document document) {
    return new HashMap<String, Float>();
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
    return new Marketplace();
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".fbits-componente-imagem img").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("data-zoom-image").trim();
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".fbits-produto-imagensMinicarrossel-item a");

    for (Element e : imagesElement) {
      String image = e.attr("data-zoom-image").trim();

      if (!image.equalsIgnoreCase(primaryImage) && !image.isEmpty()) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private ArrayList<String> crawlCategories(Document document) {
    ArrayList<String> categories = new ArrayList<String>();
    Elements elementCategories = document.select("#fbits-breadcrumb ol li a span");

    for (int i = 1; i < elementCategories.size(); i++) { // starting from
                                                         // index 1,
                                                         // because the
                                                         // first is the
                                                         // market name
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String getCategory(ArrayList<String> categories, int n) {
    if (n < categories.size()) {
      return categories.get(n);
    }

    return "";
  }

  private String crawlDescription(Document document) {
    String description = "";

    Element shortDescription = document.selectFirst("#spnValorReferente");
    if (shortDescription != null) {
      description = description + shortDescription.html();
    }

    Element descriptionElement = document.select(".informacao-abas #conteudo-0").first();
    Element specElement = document.select(".informacao-abas #conteudo-1").first();

    if (descriptionElement != null)
      description = description + descriptionElement.html();
    if (specElement != null)
      description = description + specElement.html();

    return description;
  }

}
