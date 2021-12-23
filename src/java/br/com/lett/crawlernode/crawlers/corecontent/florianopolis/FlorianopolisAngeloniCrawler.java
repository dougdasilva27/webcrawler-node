package br.com.lett.crawlernode.crawlers.corecontent.florianopolis;

import java.util.*;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.crawlers.extractionutils.core.AngeloniSuperUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class FlorianopolisAngeloniCrawler extends Crawler {

   private final Set<String> cards = Set.of(Card.AMEX.toString(), Card.DINERS.toString(), Card.ELO.toString(), Card.MASTERCARD.toString(), Card.VISA.toString(),
      Card.HIPERCARD.toString());

   public FlorianopolisAngeloniCrawler(Session session) {
      super(session);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      this.cookies = AngeloniSuperUtils.fetchLocationCookies(session, this.dataFetcher);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String internalPid = internalId;
         String newUrl = internalId != null ? CrawlerUtils.getRedirectedUrl(session.getOriginalURL(), session) : session.getOriginalURL();
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcumb > a:not(:first-child)");
         String name = crawlName(doc);
         String defaultImage = CrawlerUtils.scrapUrl(doc, "meta[property=\"og:image\"]", "content", "https", "img.angeloni.com.br");
         String host = defaultImage != null ? new java.net.URI(defaultImage).getHost() : "img.angeloni.com.br";
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".container__body-detalhe-produto .p-relative .zoom", Arrays.asList("data-zoom-image", "src"), "https", host);
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".swiper-slide.count-slide img", Collections.singletonList("src"), "https", host, primaryImage);
         String description = crawlDescription(doc, internalId);
         boolean available = doc.select("#productUnavailable").isEmpty() && doc.selectFirst(".box-sem-estoque") == null;
         Offers offers = available ? scrapOffers(internalId, internalPid) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(newUrl)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page.");
      }

      return products;
   }

   private Offers scrapOffers(String internalId, String internalPid) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      String url = "https://www.angeloni.com.br/super/ajax/productDetailPriceAjax.jsp?productId=" + internalPid
         + "&skuId=" + internalId + "&ajax=&_=1571244043146";
      Request request = RequestBuilder.create().setUrl(url).setCookies(this.cookies).build();
      Document docPrice = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      Pricing pricing = scrapPricing(docPrice);

      String sellerfullname = session.getMarket().getFullName();

      List<String> sales = Collections.singletonList(docPrice.select(".selo-expandido").text());

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerfullname)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
            .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document docPrice) throws MalformedPricingException {


      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(docPrice, ".box-produto__texto-tachado", null, true, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(docPrice, ".content__desc-prod__box-valores", "content", false, '.', session);

      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }


   private boolean isProductPage(Document doc) {
      return !doc.select(".container__body-detalhe-produto").isEmpty();
   }

   private String crawlInternalId(Document doc) {
      String id = null;

      Element elementInternalId = doc.select("[itemprop=sku]").first();
      if (elementInternalId != null) {
         id = elementInternalId.attr("content").trim();
      } else {
         Element specialId = doc.selectFirst(".content-codigo");

         if (specialId != null) {
            id = CommonMethods.getLast(specialId.ownText().trim().split(" "));
         }
      }


      return id;
   }

   private String crawlName(Document doc) {
      Element elementName = doc.select(".p-relative  h1").first();
      if (elementName != null) {
         return elementName.text().trim();
      }
      return null;
   }

   private String crawlDescription(Document doc, String internalId) {
      StringBuilder description = new StringBuilder();

      Elements descs = doc.select(".div__box-info-produto");
      for (Element e : descs) {
         description.append(e.html());
      }

      description.append(CrawlerUtils.scrapLettHtml(internalId, session, session.getMarket().getNumber()));

      return description.toString();
   }
}
