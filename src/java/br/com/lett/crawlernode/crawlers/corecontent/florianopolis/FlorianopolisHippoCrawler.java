package br.com.lett.crawlernode.crawlers.corecontent.florianopolis;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlorianopolisHippoCrawler extends Crawler {

   private final String HOME_PAGE = "http://www.hippo.com.br/";

   public FlorianopolisHippoCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(this.session.getOriginalURL(), doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalID = Integer.toString(Integer.parseInt(this.session.getOriginalURL().split("/")[4]));

         String internalPid = null;
         Element elementInternalPid = doc.selectFirst(".infos .nome .cod");
         if (elementInternalPid != null) {
            internalPid = elementInternalPid.text().split("\\.")[1].trim();
         }

         String name = null;
         Element elementName = doc.selectFirst("p.nome span");
         if (elementName != null) {
            name = elementName.text().replace("'", "").trim();
         }

         Float price = null;
         Element elementPrice = doc.selectFirst("div.preco_comprar div.valores .valor");
         if (elementPrice != null) {
            price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replace(",", ".").trim());
         }

         Elements element_cat1 = doc.select(".breadcrumb a");
         String category1 = (element_cat1.size() >= 3) ? element_cat1.get(2).text().trim() : "";

         Elements element_cat2 = doc.select(".breadcrumb a");
         String category2 = (element_cat2.size() >= 4) ? element_cat2.get(3).text().trim() : "";


         String primaryImage = null;
         Element elemImg = doc.selectFirst("#zoom_detalhes");
         if (elemImg != null && elemImg.attr("data-zoom-image") != null) {
            primaryImage = HOME_PAGE + elemImg.attr("data-zoom-image").substring(3);
         }

         String description = "";
         Element elementDescription = doc.select("div.accordion--body").first();
         if (elementDescription != null) {
            description = description + elementDescription.html();
         }

         Prices prices = crawlPrices(price);

         Product product = new Product();

         product.setUrl(session.getOriginalURL());
         product.setInternalId(internalID);
         product.setInternalPid(internalPid);
         product.setName(name);
         product.setPrice(price);
         product.setPrices(prices);
         product.setCategory1(category1);
         product.setCategory2(category2);
         product.setPrimaryImage(primaryImage);
         product.setDescription(description);
         product.setAvailable(true);

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private boolean isProductPage(String url, Document doc) {
      return url.contains("/produto/") && doc.select("p.nome").first() != null;
   }

   private Prices crawlPrices(Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new HashMap<>();

         installmentPriceMap.put(1, price);

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      }

      return prices;
   }
}
