package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.Pricing;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MexicoMultiherramientasCrawler extends Crawler {
   private static String SELLER_NAME = "Multi Herramientas";
   private static String HOST = "https://multiherramientas.mx/";

   public MexicoMultiherramientasCrawler(Session session) {
      super(session);
   }

   @Override
   protected Response fetchResponse() {
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setProxyservice(List.of(ProxyCollection.BUY, ProxyCollection.LUMINATI_SERVER_BR, ProxyCollection.NETNUT_RESIDENTIAL_MX,ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new ApacheDataFetcher(), new FetcherDataFetcher(), new JsoupDataFetcher()), session, "get");

      return response;
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String productName = CrawlerUtils.scrapStringSimpleInfo(document, ".h2.mb-4", false);
         String internalId = CommonMethods.getLast(session.getOriginalURL().split("="));
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(document, ".exzoom_img_ul > li > img", Arrays.asList("src"), "https", "multiherramientas.mx");
         List<String> secondaryImages = getSecondaryImages(document);
         String description = CrawlerUtils.scrapSimpleDescription(document,Arrays.asList(".col-md-6.m-4"));
         boolean available = document.selectFirst(".text-success") != null;
         Offers offers = available ? scrapOffers(document) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(productName)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .build();
         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".col-md-6.m-4") != null;
   }

   private List<String> getSecondaryImages(Document doc) {
      List<String> secondaryImages = new ArrayList<>();

      Elements images = doc.select(".exzoom_img_ul > li > img");
      for (Element imageLi : images) {
         secondaryImages.add(HOST + imageLi.attr("src"));
      }
      if (secondaryImages.size() > 0) {
         secondaryImages.remove(0);
      }
      return secondaryImages;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".h3.fw-bold.my-4", null, false, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".col-md-6.m-4 >.text-muted >bdi", null, false, '.', session);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .build();
   }
}
