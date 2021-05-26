package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.*;

import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.crawlers.extractionutils.core.LifeappsCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import com.google.common.collect.Sets;
import com.sun.syndication.feed.rss.Guid;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 31/07/2019
 *
 * @author Joao Pedro
 */
public class BrasilJcdistribuicaoCrawler extends LifeappsCrawler {

   private static final String HOME_PAGE = "https://jcdistribuicao.superon.app/";
   private static final String COMPANY_ID = "6f0ae38d-50cd-4873-89a5-6861467b5f52";    //never changes
   private static final String API_HASH = "cc64548c0cbad6cf58d4d3bbd433142b694d281e-b509-42c4-8042-78cfeb0c52ff"; //can change, but is working since 2019
   private static final String FORMA_PAGAMENTO = "3dedac19-6643-4401-9c08-aac81d6edb7c";  //never changes
   private static final String SELLER_NAME_LOWER = "jc distribuicao brasil";

   public BrasilJcdistribuicaoCrawler(Session session) {
      super(session);
   }

   @Override
   public String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   public String getCompanyId() {
      return COMPANY_ID;
   }

   @Override
   public String getApiHash() {
      return API_HASH;
   }

   @Override
   public String getFormaDePagamento() {
      return FORMA_PAGAMENTO;
   }

   @Override
   public String getSellerName() {
      return SELLER_NAME_LOWER;
   }
}
