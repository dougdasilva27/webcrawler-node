package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
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

/************************************************************************************************************************************************************************************
 * Crawling notes (29/08/2016):
 * 
 * 1) For this crawler, we have one url for mutiples skus.
 * 
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace information.
 * 
 * 4) In some cases in products has color variation, a variation can be void, so is treated in this
 * crawler.
 * 
 * 5) The sku page identification is done simply looking the URL format or simply looking the html
 * element.
 * 
 * 6) Even if a product is unavailable, its price is not displayed.
 * 
 * 7) There is no internalPid for skus in this ecommerce.
 * 
 * 8) The first image in secondary images is the primary image.
 * 
 * 9) When the sku has variations, the variation name it is added to the name found in the main
 * page.
 * 
 * 10) In case with color variations is make a url with internalId for crawl iformations for this
 * sku.
 * 
 * 11) In case with voltage variations, we have informations in html selector.
 * 
 * 12) When one sku with color variations is void, this crawler accessed a page diferent, then
 * verify if cod in main page is empty, if is empty, this crawler ignore this variation because that
 * is void.
 * 
 * Examples: ex1 (available):
 * http://www.multiloja.com.br/produto/TV-43-Led-Conversor+TV+Digital+Integrado-PHILIPS/5669 ex2
 * (unavailable):
 * http://www.multiloja.com.br/produto/Antena+Interna+DTV-4500+para+TV+(VHFUHFHDTV)-Aquario/3688 ex3
 * (Color Variation):
 * http://www.multiloja.com.br/produto/guarda+roupa+aires+3+portas+3+gavetas-araplac/7271 ex4
 * (Variation):
 * http://www.multiloja.com.br/produto/Cafeteira+CP15+600W+com+Chapa+Aquecedora+Indicador+do+Nivel+de+agua+15xicaras-BRITANIA/2689
 *
 * Optimizations notes: No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilMultilojaCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.multiloja.com.br/";

  public BrasilMultilojaCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {

      String internalId = null;
      String primaryImage = null;
      Elements variations = doc.select(
          "body > div.conteudo > div > form > div > div:nth-child(3) > div:nth-child(2) > div.ciq > div:nth-child(2) > div");

      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1 span", false);
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "span[itemprop=\"productID\"]", false);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#divPreco .p2a span", null, false, ',', session);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#brd-crumbs li", true);
      Prices prices = scrapPrices(doc);
      boolean available = scrapAvaibility(doc);
      String description = scrapDescription(doc);
      String secondaryImages = null;

      for (Element element : variations) {
        internalId = scrapInternalId(element);
        primaryImage = scrapPrimaryImage(element);
        secondaryImages = scrapSecondaryImages(doc, internalId);

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


  private String scrapSecondaryImages(Element element, String internalId) {
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = element.select("#novafoto" + internalId + " div a");

    for (Element ancor : images) {
      if (ancor.hasAttr("href")) {
        secondaryImagesArray.put(CrawlerUtils.completeUrl(ancor.attr("href"), "https", "www.multiloja.com.br"));
      }
    }

    return secondaryImagesArray.toString();
  }

  private String scrapDescription(Document doc) {
    String description = null;
    Element descriptionElement = doc.selectFirst(".InfoProd");

    if (descriptionElement != null) {
      description = descriptionElement.html();
    }

    return description;
  }

  private boolean scrapAvaibility(Document doc) {
    Element availabiltyElement = doc.selectFirst("#Estoque a img");
    boolean available = false;

    if (availabiltyElement != null) {
      available = !availabiltyElement.text().contains("esgotado");
    }

    return available;
  }

  private Prices scrapPrices(Document doc) {
    Prices prices = new Prices();
    Integer installment = null;
    prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, "#divPreco .p2 b", null, false, ',', session));

    Map<Integer, Float> installmentPriceMap = new TreeMap<>();

    Element installmentsElement = doc.selectFirst("#divPreco > span:nth-child(1) > p:nth-child(5) > b");

    if (installmentsElement != null) {
      String aux = installmentsElement.text();
      installment = aux.contains("x") ? Integer.parseInt(aux.replace("x", "")) : null;
      Float aprazo = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#divPreco > span:nth-child(1) > p:nth-child(6) > b", null, false, ',', session);

      if (aprazo != null) {
        installmentPriceMap.put(installment, aprazo);

      }
    }

    prices.setBankTicketPrice(CrawlerUtils.scrapFloatPriceFromHtml(doc, "#divPreco > p.p22 > b:nth-child(1)", null, false, ',',
        session));

    prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

    return prices;
  }

  private String scrapPrimaryImage(Element element) {
    Element imgElement = element.selectFirst("a");
    String img = null;

    if (imgElement != null && imgElement.hasAttr("href")) {
      img = CrawlerUtils.completeUrl(imgElement.attr("href"), "https", "www.multiloja.com.br");
    }

    return img;
  }

  private String scrapInternalId(Element element) {
    return element.hasAttr("id") ? element.attr("id") : null;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".conteudo").isEmpty();
  }


}
