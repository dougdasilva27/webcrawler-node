package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.Arrays;
import java.util.List;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CNOVANewCrawler;

public class SaopauloExtramarketplaceCrawler extends CNOVANewCrawler {

   private static final String STORE = "extra";
   private static final String INITIALS = "EX";
   private static final List<String> SELLER_NAMES = Arrays.asList("Extra", "extra.com.br");

   public SaopauloExtramarketplaceCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   protected Response fetchPage(String url, boolean tryAgain) {
      Request request = RequestBuilder.create()
            .setUrl(url)
            .build();

      return new ApacheDataFetcher().get(session, request);
   }

   @Override
   protected String getStore() {
      return STORE;
   }

   @Override
   protected List<String> getSellerName() {
      return SELLER_NAMES;
   }

   @Override
   protected String getInitials() {
      return INITIALS;
   }
}
