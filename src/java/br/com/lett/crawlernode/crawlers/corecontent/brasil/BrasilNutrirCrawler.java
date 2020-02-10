package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.prices.Prices;

public class BrasilNutrirCrawler extends Crawler {
   private static final String HOME_PAGE = "https://www.nutrir-sc.com.br/";

   public BrasilNutrirCrawler(Session session) {
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

         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".p_info > .p_tags_list .n1");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".p_img .img_primary img", Arrays.asList("src"), "https",
               "www.nutrir-sc.com.br");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "[id^=fotos] .galeria li:not(:last-child) a",
               Arrays.asList("href"), "https://", "www.nutrir-sc.com.br", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".produto_detalhes .tabs #descricao-do-produto",
               ".produto_detalhes .tabs #informacoes-tecnicas"));

         Elements variations = getVariations(doc, ".prod_select #produto_sel option[value]");

         if (variations.isEmpty()) {
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".p_title h1", true);
            String internalId = scrapInternalId(doc, ".produto.produto_show");
            boolean available = false;

            // Creating the product
            Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setName(name)
                  .setAvailable(available)
                  .setPrices(new Prices())
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .build();

            products.add(product);

         } else {
            for (Element e : variations) {
               String internalId = e.val();
               String name = scrapVariationName(e);
               Integer stock = scrapStock(e);
               Float price = scrapPrice(e);
               Prices prices = crawlPrices(price, e);
               boolean available = stock != null && stock > 0;

               // Creating the product
               Product product = ProductBuilder.create()
                     .setUrl(session.getOriginalURL())
                     .setInternalId(internalId)
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
                     .setStock(stock)
                     .build();

               products.add(product);
            }
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("div.produto_show") != null;
   }

   private Elements getVariations(Document doc, String selector) {
      return doc.select(selector);
   }

   private String scrapInternalId(Document doc, String selector) {
      Element e = doc.selectFirst(selector);
      String internalId = null;

      if (e != null) {
         String aux = e.id();

         if (!aux.isEmpty()) {
            internalId = aux.substring(1);
         }
      }

      return internalId;
   }

   private String scrapVariationName(Element e) {
      String name = e.ownText();

      if (name.contains("(R$")) {
         name = name.split("\\(R\\$")[0].trim();
      } else if (name.contains("(de")) {
         name = name.split("\\(de")[0].trim();
      }

      return name;
   }

   private Integer scrapStock(Element e) {
      Integer stock = 0;
      String attr = e.attr("qtde");

      if (!attr.isEmpty()) {
         stock = Integer.parseInt(attr);
      }

      return stock;
   }

   private Float scrapPrice(Element e) {
      Float price = null;

      // search the value in the reversed text to find the last ocurrence
      Matcher m = Pattern.compile("([0-9]+,[0-9]+)").matcher(new StringBuilder(e.text()).reverse());

      // get the first ocurrence
      if (m.find()) {
         String aux = m.group();

         if (aux != null) {
            // parse the value after reversing the string to original
            price = MathUtils.parseFloatWithComma(new StringBuilder(aux).reverse().toString());
         }
      }

      return price;
   }

   private Prices crawlPrices(Float price, Element e) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         Matcher m = Pattern.compile("([0-9]+,[0-9]+)").matcher(e.text());

         if (m.find()) {
            String aux = m.group();

            if (aux != null) {
               prices.setPriceFrom(MathUtils.parseDoubleWithComma(aux));
            }
         }

         // get the second occurrence of values and, if exists, parse it
         if (m.find()) {
            String aux = m.group();

            if (aux != null) {
               prices.setBankTicketPrice(MathUtils.parseDoubleWithComma(aux));
            }
         } else {
            // if doesnt exist, set the main price
            prices.setBankTicketPrice(MathUtils.normalizeTwoDecimalPlaces(price.doubleValue()));
         }

      }

      return prices;
   }
}
