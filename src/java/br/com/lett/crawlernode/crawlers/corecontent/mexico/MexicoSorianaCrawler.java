package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import org.apache.http.HttpHeaders;
import org.jsoup.nodes.Document;

import java.util.*;

public class MexicoSorianaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "soriana";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public MexicoSorianaCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   private Map<String, String> getHeaders() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("Connection", "keep-alive");
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("Accept", "*/*");
      headers.put("cookie", "PIM-SESSION-ID=dznWODemsE3uLOoC; dwac_f1e552945f65c8b8b0264c1a64=PTG1Ii-Pw2dC3o-3fPsnNlP9e1jTX53j9qk%3D|dw-only|||MXN|false|America%2FMexico%5FCity|true; cqcid=ackLoYst7aCewofaKKgPkobfLK; cquid=||; dwanonymous_ad95070cfe256984fa6223b8e5b1c401=ackLoYst7aCewofaKKgPkobfLK; sid=PTG1Ii-Pw2dC3o-3fPsnNlP9e1jTX53j9qk; __cq_dnt=0; dw_dnt=0; dwsid=tJb04uh4Z1BjUFlGo-p4EtpaOpgUqDnU1yd-nwWkycRT5hWg_NnI42XF2gIFU3dqxYYcsS2AFicPMXXruLu_Hg==; _gcl_au=1.1.1389905974.1658947038; cookieCodePostal=true; __cq_uuid=deagk4FCec3rRbruYeyHJqgHLg; __cq_bc=%7B%22bgbd-Soriana%22%3A%5B%7B%22id%22%3A%222023107%22%7D%5D%7D; __cq_seg=; bm_sv=17E4F7F1D642F21C369E93917EBFBC9B~YAAQbs1YaHExmyCCAQAAwZ3xQBApuIwc5D/vZ66bhdxhFM/SMbxGhvva+Vb0ie/JObqXsmm4VNTEI23pKHKZEoZk4fbNWm3TpJr/NBZjU0K7R33orUiIVRESbzeq3tfnQ0OqfPf8gUtVB8he+RaSkFMwVifKLJJrQxEBzjlj7U/eaLabY75/xqt6FrVV1dhyAVS0SSSbmyrfH9g9L7zG9XPCLlqjjvng8GO+FNLbJUixe/mZqoSwRztPnX4YiqVKRA==~1; _ga_LZ2D37D7GQ=GS1.1.1658946171.1.1.1658947038.15; _ga=GA1.2.947908215.1658947038; _gid=GA1.2.95255934.1658947038; ak_bmsc=53D9644728FF16E4B0F82D0CDDD98D66~000000000000000000000000000000~YAAQbs1YaHgxmyCCAQAAq57xQBAUhCGmrfbpvGoYYp5ngvQrVnZ4CXKN3d0pUQKkjkurhPnBpmNvrFpf/3GgAfuwanttexCGXKaT45gmcuMP2vVqXgTv5tTlT3TZod4KfVX2NxvvhrEeXeG2+wR2hDWVfXN0io5gy6l9+bddk8rQ5tQPaPOjVVHLSY2UpmyzBu1z0//jl4XTtxHFthf3RwJthubARr+xmNH05ZDXVQaPUAYI95Y3tClPxooHDIB9q+1jR6/1n0F9i9rKXSbT9ObcsutHWmHopt68/KH+quIfpRvlSSYWWNVhdQ7AVgDD1wLe4hY4H2ttdaHjiMzgUxLrTb3NXX/rznplYd99AO2yn/aJENXqoa5hgAO5rhaDKf2y1e5dVZDvl9uphaWDT7FWuTwStbKwhw+pTkamf8+Gm/b7WLB1c5ipUTlGDRCo9BgyyP1rJCJssATkf3pFuvaInLMNE4XyC+zmIo/i2B9hBA1+IxHk/rB/emjy; _scid=faea27f4-421e-473a-8791-e88c6da14c75; _uetsid=216e46400dd911ed9b14b35e1be3535a; _uetvid=410d2d20f94b11ec8d6127a397078e76; _tt_enable_cookie=1; _ttp=91616f0c-0b77-49ee-b7b7-bdaaa8d713fc; _gat_UA-4339337-6=1; _hjSessionUser_464447=eyJpZCI6IjFjYTQ2YWEzLWNhODctNWQ3My05ZDkzLWQ1Y2EyYTdhMzlmZSIsImNyZWF0ZWQiOjE2NTg5NDcwMzk3NjAsImV4aXN0aW5nIjpmYWxzZX0=; _hjFirstSeen=1; _hjIncludedInSessionSample=0; _hjSession_464447=eyJpZCI6IjE3NTM2YjJmLTYwY2UtNDI1MC1hMTZiLTQ0NjIyNDNhZDFkMSIsImNyZWF0ZWQiOjE2NTg5NDcwMzk5NjUsImluU2FtcGxlIjpmYWxzZX0=; _hjAbsoluteSessionInProgress=0; _pin_unauth=dWlkPVl6RmpaVFZpT0RBdFl6Y3dZUzAwTlRNNUxUaGxZall0TURJd056Smlaamt3WWpJeA; _clck=nhs57s|1|f3i|0; _clsk=13kjz7o|1658947040490|1|1|l.clarity.ms/collect; _gali=formPostalcode; RT=\"z=1&dm=soriana.com&si=3088047b-40c0-4b00-bace-70fe7f8816e1&ss=l63xpq1z&sl=6&tt=swl&bcn=%2F%2F17de4c1f.akstat.io%2F&ld=iqmb&nu=upsadnr&cl=iuxj\"; _abck=7A2A1798C7B33BF3C12E5AA1463770F1~-1~YAAQbs1YaGennSCCAQAAP+cnQQjiyPa1QRQSjff3WsIeRaWfHtihN6CMdM/per1OnALgGacG3XzKGpDSNH/lw1PKodA79vyeesaSjPOXJWKtwcdfFrSU8M5qh86MlEAEBzOI231B/zqkMp91NJpFphLoool6497k4XxK37E+8Zi7mPYDrWGD5ohjm8hprvzM/XmPT/CFLlNBYknWrm7tXq8KQf9g6TjH1/TEQ58aJ74kOcqJnfgyEJLU5LJqI8GyFOXN4pzhav8BFqhm52LXIyaMaiTAGFvYoPkZrm9xdmkT4A96IHPgMvGFs2FF2HAg+A0UdqfK2JZYVgeNcVxIOUEPPhthebI9mD8jnbnhqeLOUkC/sxR5PHp+jYt4k1snz/hy2nF+itjh/fGWU0feCjdl83BibdmJNA==~0~-1~-1; bm_sv=17E4F7F1D642F21C369E93917EBFBC9B~YAAQbs1YaGinnSCCAQAAP+cnQRDYXCT5eD8jzs7JLg6BnKW2/+uM/mA3NRcEZeE0u4NjyqIt0pL4aFqoK+ovmqLCUOmo42szAAFiU7TsXAiRtT+qjr10FQUSaazSpKbEp/Op4FOFGkbs9hTh/Kw44bCeiiesrEumh0nFGOkrveIS+UPJvLeKuAf8jHdbmoFC7ZjG3wlwFA7EK9cgxRNyAHCgof7+Oh3RNQnnCXjF91HJ5ePJKLIF83AvujDNftrX+g==~1; __cq_dnt=0; dw_dnt=0");

      return headers;
   }
   @Override
   public void handleCookiesBeforeFetch() {
      Response response;
      String postalCode = session.getOptions().optString("postalCode");
      if (postalCode.isEmpty()) {
         Request request = Request.RequestBuilder.create().setUrl("https://www.soriana.com/").build();
         response = this.dataFetcher.get(session, request);
      } else {
         Request request = Request.RequestBuilder.create()
            .setUrl("https://www.soriana.com/on/demandware.store/Sites-Soriana-Site/default/Stores-UpdateStoreByPostalCode")
            .setHeaders(getHeaders())
            .setProxyservice(Arrays.asList(
               ProxyCollection.BUY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES,
               ProxyCollection.NETNUT_RESIDENTIAL_BR
            ))
            .setSendUserAgent(true)
            .setPayload("dwfrm_storeUpdate_postalCode=" + postalCode + "&basketValidation=true&selectSubmitPc=true&methodid=homeDelivery")
            .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create().mustUseMovingAverage(false).mustRetrieveStatistics(true).build())
            .build();
         response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session, "post");
      }
      this.cookies.addAll(response.getCookies());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".container.product-detail.product-wrapper", "data-pid");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".carousel-item.active img", Collections.singletonList("src"), "https", "centralar.com.br");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".carousel-indicators li:not(:first-child) img", Collections.singletonList("src"), "https", "www.soriana.com", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".description-and-detail"));
         CategoryCollection categoryCollection = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:last-child) a", true);
         boolean availableToBuy = doc.select(".cart-icon.d-none").isEmpty();
         Offers offers = availableToBuy ? scrapOffer(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categoryCollection)
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
      return !doc.select(".container.product-detail.product-wrapper").isEmpty();
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String sale = CrawlerUtils.calculateSales(pricing);

      if (sale != null) {
         sales.add(sale);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".sales .value", "content", true, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".strike-through.list span", "content", true, '.', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

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
