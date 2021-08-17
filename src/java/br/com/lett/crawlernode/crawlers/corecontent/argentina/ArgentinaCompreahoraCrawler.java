package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.jsoup.nodes.Document;

import java.util.*;

public class ArgentinaCompreahoraCrawler extends Crawler {
   private static final String SELLER_FULL_NAME = "Compre Ahora";

   public ArgentinaCompreahoraCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("x-requested-with", "XMLHttpRequest");
      headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");


      String payload = "username=federico.serrano%40scmalaga.com.ar" +
         "&password=Cl112007*";


      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.compreahora.com.ar/customerlogin/ajax/login")
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .setPayload(payload)
         .build();

      Response response = this.dataFetcher.post(session, request);

      this.cookies = response.getCookies();
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".pdp-item", "data-product-id");
      String internalPid = internalId;
      String name = CrawlerUtils.scrapStringSimpleInfo(document, ".base", true);
      JSONArray imagesArray = CrawlerUtils.crawlArrayImagesFromScriptMagento(document);
      String primaryImage = CrawlerUtils.scrapPrimaryImageMagento(imagesArray);
      String secondaryImages = CrawlerUtils.scrapSecondaryImagesMagento(imagesArray, primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(document, Arrays.asList(".data.item.content .value", ".data.item.content .additional-composition-wrapper"));
      boolean available = document.selectFirst(".stock.available") != null;
      Offers offers = available ? scrapOffers(document) : new Offers();

      products.add(ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setPrimaryImage(primaryImage)
         .setSecondaryImages(secondaryImages)
         .setDescription(description)
         .setOffers(offers)
         .build());

      return products;
   }

   private Offers scrapOffers(Document document) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(document);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document document) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(document, ".old-price .price", null, true, ',', session);
      Double spotlightPrice;
      if (priceFrom != null)
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(document, ".special-price .price", null, true, ',', session);
      else
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(document, ".product-info-price .price", null, true, ',', session);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .build();
   }
}

