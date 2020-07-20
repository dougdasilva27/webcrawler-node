package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;


import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class SaopauloChoppbrahmaexpressCrawler extends Crawler {

  public SaopauloChoppbrahmaexpressCrawler(Session session) {
    super(session);
  }

  private final String HOME_PAGE = "https://choppbrahmaexpress.com.br/produtos/?franquia=brooklin";

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }


  @Override protected Object fetch() {

    String url = "https://choppbrahmaexpress.com.br/belohorizonte-funcionarios/produtos/chopp-brahma-claro-50l-3";

    Map<String, String> headers = new HashMap<>();
    headers.put("Cookie",
          " _td_global=fdf9cb89-9be5-4e47-80d5-e5da7da4c664; PHPSESSID=496d8b3ddff949291b3a75763dbfb69f; _cid=rbAWQzIn3Fi14j35; visid_incap_2241603=fCKnnvngRYawMQ3KBMh7Wk3MFV8AAAAAQUIPAAAAAACpKkWrPJBMsdVEJ/k6OSVo; incap_ses_989_2241603=9nRpf011yXAHs63iWaK5DU3MFV8AAAAAIcO0dVQmViyMkLU38C5bTg==; _vwo_uuid_v2=D85B252C6F128F4E7A1067F823D74A234|d5530b4263a3793983cf486dedafbe0c; _vis_opt_s=1%7C; _vis_opt_test_cookie=1; _vwo_uuid=D85B252C6F128F4E7A1067F823D74A234; _vwo_ds=3%3Aa_0%2Ct_0%3A0%241595264215%3A46.92622103%3A%3A%3A237_0%2C236_0%3A1; _gcl_au=1.1.99500028.1595264216; __kdtv=t%3D1594920543779%3Bi%3D41f83249968552c15abe3b1bbd2f04e2f35c5029; _kdt=%7B%22t%22%3A1594920543779%2C%22i%22%3A%2241f83249968552c15abe3b1bbd2f04e2f35c5029%22%7D; _ga=GA1.3.1298239815.1595264217; _gid=GA1.3.614302521.1595264217; __td_blockEvents=false; optimizelyEndUserId=oeu1595264217045r0.31849370868921123; _st_ses=31895622204768026; _st_no_user=1; _st_no_script=1; _sptid=6325; _spcid=5400; _fbp=fb.2.1595264218925.426271760; _cm_ads_activation_retry=false; sback_browser=0-34171300-1592917032f74c696c359d956607aa469623d15c2dd1d1fe0a20405602565ef1fc28536e61-38206177-186250129183,5418223384-1595264218; _hjid=13787402-084d-4d07-8b13-65e13ff34362; _hjAbsoluteSessionInProgress=1; sback_client=5d8524ce70662eb4fc46d5c1; sback_customer=$2gMxcWVp1mQaFEexlFRy4UW5l3TFhzRY9UQ1cVRXdEcPB3ajZ1UHlTTIRjQjlDV0kFOxQUSVdFNNpkM6lFb6ZTW2$12; sback_access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhcGkuc2JhY2sudGVjaCIsImlhdCI6MTU5NTI2NDIxOSwiZXhwIjoxNTk1MzUwNjE5LCJhcGkiOiJ2MiIsImRhdGEiOnsiY2xpZW50X2lkIjoiNWQ4NTI0Y2U3MDY2MmViNGZjNDZkNWMxIiwiY2xpZW50X2RvbWFpbiI6ImNob3BwYnJhaG1hZXhwcmVzcy5jb20uYnIiLCJjdXN0b21lcl9pZCI6IjVlZjFmYzJhODlhOGRlMDg3YTViMTY2YyIsImN1c3RvbWVyX2Fub255bW91cyI6dHJ1ZSwiY29ubmVjdGlvbl9pZCI6IjVmMTA4ZTZmODFmYzE1ZWZjZDIzNDAzNCIsImFjY2Vzc19sZXZlbCI6ImN1c3RvbWVyIn19.9FAui4QmliAB6TX8m_x-vUqLcHiRcWj5dek452Z6qmc.WrWrDrHeDrgPuyzRgPWrHe; sback_partner=false; __zlcmid=zHjIYELJMIO0nj; sback_current_session=1; sback_total_sessions=1; sb_days=1595264221993; sback_customer_w=true; tdcn8Q_1pc=d51a944e-ee99-401f-9847-cdd708eb1298; blueID=3caf1e0c-8f33-4f61-91f8-9a3ac1485630; agegate=1; OptanonAlertBoxClosed=2020-07-20T16:57:29.232Z; _td_global=fdf9cb89-9be5-4e47-80d5-e5da7da4c664; postcode_data=a%3A9%3A%7Bs%3A7%3A%22success%22%3Bb%3A1%3Bs%3A10%3A%22error_code%22%3BN%3Bs%3A8%3A%22postcode%22%3Bs%3A9%3A%2230160-010%22%3Bs%3A6%3A%22street%22%3Bs%3A10%3A%22R+da+Bahia%22%3Bs%3A17%3A%22neighborhood_name%22%3Bs%3A6%3A%22Centro%22%3Bs%3A9%3A%22city_name%22%3Bs%3A14%3A%22Belo+Horizonte%22%3Bs%3A2%3A%22uf%22%3Bs%3A2%3A%22MG%22%3Bs%3A3%3A%22url%22%3Bs%3A26%3A%22belohorizonte-funcionarios%22%3Bs%3A12%3A%22affiliate_id%22%3Bs%3A6%3A%22106774%22%3B%7D; AffiliateAutoSelect=1; Affiliate=belohorizonte-funcionarios; notification_customer=1; frontend=d2da82018d49cdf837f8930bf27ff22c; frontend_cid=UmrshFVedj27eXMB; enext=adba8f8d-a365-4640-b460-f183d53bf9d3; enext-s=16f9bdce-6c77-4c6b-aac1-34a375ac00a6; sback_refresh_wp=no; LAST_CATEGORY=38; _vwo_sn=0%3A3; _uetsid=b75da4fc61f718d5d66bb9f64bb91295; _uetvid=e10dc62802c1e8cad581c473e6f34472; __atuvc=1%7C30; __atuvs=5f15cdf6099eb81c000; OptanonConsent=isIABGlobal=false&datestamp=Mon+Jul+20+2020+14%3A01%3A43+GMT-0300+(Hor%C3%A1rio+Padr%C3%A3o+de+Bras%C3%ADlia)&version=6.3.0&landingPath=NotLandingPage&groups=1%3A1%2C2%3A1%2C3%3A1%2C4%3A1%2C0_277070%3A1%2C0_236004%3A1%2C0_236006%3A1%2C0_236008%3A1%2C0_236010%3A1%2C0_236012%3A1%2C0_236014%3A1%2C0_236016%3A1%2C0_236019%3A1%2C0_278687%3A1%2C0_236021%3A1%2C0_278689%3A1%2C0_236023%3A1%2C0_236025%3A1%2C0_278685%3A1%2C0_236027%3A1%2C0_277071%3A1%2C0_236005%3A1%2C0_236007%3A1%2C0_236009%3A1%2C0_236011%3A1%2C0_236013%3A1%2C0_278690%3A1%2C0_236015%3A1%2C0_236017%3A1%2C0_236018%3A1%2C0_278686%3A1%2C0_236020%3A1%2C0_278688%3A1%2C0_236022%3A1%2C0_236024%3A1%2C0_236026%3A1&AwaitingReconsent=false; _td=b3b551f6-4736-4dca-81a6-91f8de248f4c; _spl_pv=3");

    Request request = Request.RequestBuilder.create().setCookies(cookies).setHeaders(headers).setUrl(url).build();
    String response = this.dataFetcher.get(session, request).getBody();
    Document document = Jsoup.parse(response);
    System.out.println(document);

    return document;
  }

  @Override
  public List<Product> extractInformation(Document document) throws Exception {

    super.extractInformation(document);
    List<Product> products = new ArrayList<>();

    if (document.selectFirst(".product-view.has-cep .product-essential") != null) {

      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".product-essential div input[name=\"product\"]", "value");
      String internalPid = internalId;
      String name = CrawlerUtils.scrapStringSimpleInfo(document, ".product-essential .full_product_cont .product-name", false);
      boolean available = document.selectFirst(".add-to-cart-buttons button") != null;
      CategoryCollection categories = CrawlerUtils.crawlCategories(document, ".breadcrumbs ul li");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(document, ".gallery-image", Arrays.asList("data-zoom-image"), null, null);
      System.err.println(primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(document, Arrays.asList(".row.description_cont .short-description"));

      // Creating the product
      Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setDescription(description)

            .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }
}
