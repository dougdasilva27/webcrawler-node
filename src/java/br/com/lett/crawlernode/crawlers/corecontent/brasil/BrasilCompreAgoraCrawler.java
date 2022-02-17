package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class BrasilCompreAgoraCrawler extends Crawler {
   protected Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(), Card.VISA.toString(), Card.AMEX.toString(), Card.DINERS.toString());
   private static final String SELLER_NAME = "Compre Agora";

   public BrasilCompreAgoraCrawler(Session session) {
      super(session);
      config.setParser(Parser.HTML);
   }


   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      // data de expiração Tue Feb 07 2023 18:15:20 GMT-0300 (Horário Padrão de Brasília) está no código para diminuir a quantidade de requests
      headers.put("Cookie", "CPL=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJpbmZyYWNvbW1lcmNlLmNvbS5iciIsInN1YiI6IkluZnJhY29tbWVyY2UiLCJhdWQiOiJjb21wcmEtYWdvcmEuY29tIiwiaWF0IjoxNjQ0MjY2MjUxLCJkYXRhIjp7InVpZCI6IlRvYndGUmdjS3pOckNzZXhuUnozWnk3a3JwNEJ3T2VUTGxpRmtBNzR1czQ9In19.Bg1fSs0ugvcJlgSrUfVfNxb0oLyX8BkgWP4aT3cFWl6TR4jLz_Bm4nZ9zDOat7VHfM3zQ5-rngxmwCshHueTbTNjCVrrD7BeXZC9zGv2tu5lig_NR0odF4yBhKzXqxqvyxSdHnJUUJiy6w7ySn24U8vOOBWBaYhBdzqv11hCB5s1vsfGd0P8UVZidS11oilOEm0P2SwUOiCx5SL88MNuKl2QsRTWyjnB_sd7VBbu3DR5OLXamhV5RWh0YR_-MuClpIeFu6bntThPsqyK9pR8SJnJTQk9_5MAipWu2CmqGZvphJzLhfauGsxaz7Bo0-OX8_59_pe8tki80TU7GX5-VA; PHPSESSID=elc3tvs7psd3751ik8ddasuiu3; ccw=2 3 61 94 147; usrfgpt=367359160001371644268077");
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .build();

      return this.dataFetcher.get(session, request);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      super.extractInformation(document);
      List<Product> products = new ArrayList<>();
      if (!isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Not a product page" + session.getOriginalURL());
         return products;
      }
      Elements variations = (Elements) document.select(".v-centered.owl-carousel.carousel-catalogo.carousel-nav .item");

      if (!variations.isEmpty()) {

         for (Element el : variations) {
            products.add(extractProductFromHtml(document, el));
         }
      } else {
         Element el = null;
         products.add(extractProductFromHtml(document, el));
      }
      return products;
   }


   private Product extractProductFromHtml(Document document, Element el) throws OfferException, MalformedPricingException, MalformedProductException {

      // Get all product information
      String productName = CrawlerUtils.scrapStringSimpleInfo(document, ".product-title", false);
      String productInternalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".product-ean", "data-ean");
      String productInternalId = scrapId(el, productInternalPid);
      String productDescription = CrawlerUtils.scrapStringSimpleInfo(document, ".tab-pane.fade.show.active.only-text", false);
      String productPrimaryImage = CrawlerUtils.scrapSimplePrimaryImage(document, ".carousel-imagens-mobile.owl-carousel img", Arrays.asList("src"), "", "");
      List<String> productSecondaryImages = CrawlerUtils.scrapSecondaryImages(document, ".carousel-imagens-mobile.owl-carousel img", Collections.singletonList("src"), "", "", productPrimaryImage);
      boolean available = scrapStock(el);
      String variationName = scrapName(el);
      productName = variationName != null ? productName + " " + variationName + " un" : productName;
      Offers offers = available ? scrapOffers(document, productInternalId, el) : new Offers();

      return ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(productInternalId)
         .setInternalPid(productInternalPid)
         .setName(productName)
         .setPrimaryImage(productPrimaryImage)
         .setSecondaryImages(productSecondaryImages)
         .setDescription(productDescription)
         .setOffers(offers)
         .build();

   }
   private String scrapId(Element el, String productInternalPid) {
      try {
         return CrawlerUtils.scrapStringSimpleInfoByAttribute(el, ".item", "data-sku-id");
      }catch (NullPointerException ex){
         return productInternalPid;
      }
   }
   private String scrapName(Element el){
     try {
       return CrawlerUtils.scrapStringSimpleInfo(el, ".caixa-com .val",false);
     }catch (NullPointerException ex){
        return null;
     }
   }
   private boolean scrapStock(Element el){
      try {
         return el.select(".sem-estoque").isEmpty();
      }catch (NullPointerException ex){
         return false;
      }
   }

   private Offers scrapOffers(Document document, String productInternalId, Element el) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(document, productInternalId, el);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));
      offers.add(new Offer.OfferBuilder()
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .setSales(sales)
         .build()
      );
      return offers;
   }

   private Pricing scrapPricing(Document document, String id, Element el) throws MalformedPricingException {
      Double price;
      Double priceFrom;
      if (el == null){
         price = CrawlerUtils.scrapDoublePriceFromHtml(document, ".val.bestPrice", null, false, ',', session);
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(document, ".old-price .val", null, false, ',', session);
      }else{
         price = CrawlerUtils.scrapDoublePriceFromHtml(el, ".item", "data-preco-por", true, ',', session);
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(el, ".item", "data-preco-de", true, ',', session);
      }

      CreditCards creditCards = CrawlerUtils.scrapCreditCards(price, cards);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }


   private boolean isProductPage(Document document) {
      return document.selectFirst(".message__404") == null;
   }

}