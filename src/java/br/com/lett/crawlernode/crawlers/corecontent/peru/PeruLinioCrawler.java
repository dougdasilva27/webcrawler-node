package br.com.lett.crawlernode.crawlers.corecontent.peru;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PeruLinioCrawler extends Crawler {
   public PeruLinioCrawler(Session session) {
      super(session);
   }
   private static final String SELLER_NAME = "Linio";
   protected Document fetchDocument(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES,
            ProxyCollection.SMART_PROXY_PE
         ))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

      return Jsoup.parse(response.getBody());
   }
   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      doc = fetchDocument(session.getOriginalURL());
      List<Product> products = new ArrayList<>();
      Element product = doc.selectFirst(".wrapper.container-fluid");
      if (product != null) {
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".js-product-form input", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(product, ".product-name", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(product,".image-modal",List.of("data-lazy"),"https","");
         List<String> categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li", true);
         String description = CrawlerUtils.scrapStringSimpleInfo(product, ".product-description.user-content", false);
         Offers offers = scrapOffers(doc);
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".product-image.thumb-image .image-wrapper .image", Arrays.asList("data-lazy"), "https", "",primaryImage);;

         Product newProduct = ProductBuilder.create()
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setUrl(session.getOriginalURL())
            .setCategories(categories)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .build();
         products.add(newProduct);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }
   private Offers scrapOffers(Element data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(data);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Element doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-main-md", null, true, '.', session);
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".original-price", null, true, '.', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(price)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AMEX.toString(), Card.DINERS.toString(), Card.AURA.toString(),
         Card.ELO.toString(), Card.HIPER.toString(), Card.HIPERCARD.toString(), Card.DISCOVER.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }
   private List<String> getSecondaryImages(Document doc, String imgUrl) {
      List<String> imgs = CrawlerUtils.scrapSecondaryImages(doc, ".col-2.d-none.d-md-block a img", Arrays.asList("data-srcset"), "https", "", imgUrl);
      List<String> returnImgs = new ArrayList<String>();
      if (imgs != null && !imgs.isEmpty()) {
         for (Integer i = 1; i < imgs.size(); i++) {
            returnImgs.add(getImage(imgs.get(i)));
         }
      }
      return returnImgs;
   }

   private String getImage(String values) {
      String imgs[] = values.split(",");
      Integer ult = imgs.length - 1;
      String pathImg[] = imgs[ult].split(" ");
      if (pathImg[1].contains("https://")) {
         return pathImg[1];
      }
      return "https:" + pathImg[1];
   }
}
