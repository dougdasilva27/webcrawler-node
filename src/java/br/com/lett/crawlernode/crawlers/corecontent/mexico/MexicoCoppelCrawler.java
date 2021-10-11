package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.*;

public class MexicoCoppelCrawler extends Crawler {

   private static final String SELLER_NAME = "coppel";
   private static String pageId;

   public MexicoCoppelCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   private static final List<String> cards = Arrays.asList("Coppel"); //TODO verificar se precisa adicionar na lista de cartões

   @Override
   protected Document fetch() {
      Map<String, String> headers = new HashMap<>();

      headers.put("Accept","*/*");
      headers.put("Accept-Encoding","gzip, deflate, br");
      headers.put("Connection","keep-alive");

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .setProxyservice(Collections.singletonList(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .setSendUserAgent(false)
         .build();

      Response a = this.dataFetcher.get(session, request);

      String content = a.getBody();

      return Jsoup.parse(content);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst("#main_header_name") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalId = getInternalId(doc); //TODO verificar formatação id PR-7257752
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#main_header_name", false);
         this.pageId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[name=pageId]", "content");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#product_longdescription_" + this.pageId)); //TODO: remover html tags e tornar pageId dinamico
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#productMainImage ", Arrays.asList("src"), "https", "");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc,"[class=mz-lens] img", Arrays.asList("src"),"https", "padovani.vteximg.com.br", primaryImage);
         CategoryCollection categories = getCategories(doc, "[name=keywords]", "content");
         boolean available = doc.selectFirst("[class=available]") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setOffers(offers)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .build();
         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }
      return products;
   }

   public String getInternalId (Document doc) {
      String sku = CrawlerUtils.scrapStringSimpleInfo(doc, "#IntelligentOfferMainPartNumber", false);
      StringBuilder skuNumber = new StringBuilder(sku);

      for (int i = 0; i < 3; i++) {
         skuNumber = skuNumber.deleteCharAt(0);
      }

      return skuNumber.toString();
   }

   public static CategoryCollection getCategories(Document doc, String selector, String attr) {
      CategoryCollection categories = new CategoryCollection();

      Elements elementCategories = doc.select(selector);

      String selectorAttribute = elementCategories.attr(attr);

      ArrayList<String> categoryList = new ArrayList<>(Arrays.asList(selectorAttribute.split(",")));

      for (String category: categoryList) {
         categories.add(category.trim());
      }
      return categories;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder() //TODO atributos de offer???
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("coppel")//TODO verificar se full name é slug
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".unique_price", null, true, ',', session);
      //TODO verificar formato do preço
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old_price", null, true, ',', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice, doc);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double price, Document doc) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      String installmentNumbers = CrawlerUtils.scrapStringSimpleInfo(doc, "#creditCoppelPrice_" + this.pageId, false);
      String installmentPrice = CrawlerUtils.scrapStringSimpleInfo(doc, ".p_credito", false);
      System.out.println("installmentNumbers "+ installmentNumbers);
      System.out.println("installmentPrice "+ installmentPrice);

      installments.add(Installment.InstallmentBuilder.create()//TODO verificar parcelas
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .build());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }
}
