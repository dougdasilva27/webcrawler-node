package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class SaopauloChoppbrahmaexpressCrawler extends CrawlerRankingKeywords {

  public SaopauloChoppbrahmaexpressCrawler(Session session) {
    super(session);
  }

  private final String HOME_PAGE = "https://choppbrahmaexpress.com.br/produtos/?franquia=brooklin";

  protected Object fetch() {
    String url = "https://choppbrahmaexpress.com.br/brooklin/catalogsearch/result/?q=" + this.keywordWithoutAccents;

    Map<String, String> headers = new HashMap<>();
    headers.put("Cookie",
          "_td_global=fdf9cb89-9be5-4e47-80d5-e5da7da4c664; _td_global=fdf9cb89-9be5-4e47-80d5-e5da7da4c664; _ga=GA1.3.1298239815.1595264217; _gid=GA1.3.658469956.1595274373; PHPSESSID=09198dd4ed9dc04adb4116aab6427e45; _cid=g1lxnt770d86JrGm; visid_incap_2241603=AMnAYLOQSs6vdV349WOSjof0FV8AAAAAQUIPAAAAAAAZzWq5rKmcaRAnpufjL6Ab; incap_ses_989_2241603=TgvBXcwAW3VUtNLiWaK5DYj0FV8AAAAAbQau9UhAbidus57SOBlKYQ==; _vwo_uuid_v2=D1FE99864754D687707305B4373897242|8f25bb2d2f21bee57f7a3ca905e5c9c8; _vis_opt_s=1%7C; _vis_opt_test_cookie=1; _vwo_uuid=D1FE99864754D687707305B4373897242; _vwo_ds=3%3Aa_0%2Ct_0%3A0%241595274378%3A48.8959332%3A%3A%3A237_0%2C236_0%3A0; _gcl_au=1.1.1983314580.1595274379; _fbp=fb.2.1595274380186.801276848; optimizelyEndUserId=oeu1595274380527r0.06523891635237966; tdcn8Q_1pc=d51a944e-ee99-401f-9847-cdd708eb1298; _hjid=13787402-084d-4d07-8b13-65e13ff34362; __kdtv=t%3D1594920543779%3Bi%3D41f83249968552c15abe3b1bbd2f04e2f35c5029; _kdt=%7B%22t%22%3A1594920543779%2C%22i%22%3A%2241f83249968552c15abe3b1bbd2f04e2f35c5029%22%7D; __td_blockEvents=false; _st_ses=25372541853188646; _sptid=6325; _spcid=5400; _cm_ads_activation_retry=false; sback_browser=0-34171300-1592917032f74c696c359d956607aa469623d15c2dd1d1fe0a20405602565ef1fc28536e61-38206177-186250129183,5418223384-1595274384; agegate=1; __zlcmid=zHjIZL8VyMgztI; sback_client=5d8524ce70662eb4fc46d5c1; sback_customer=$2QRxAVVD1mWaJGe1lVVyMXWylXZFB3Rp9Uc1QTRXdEOPJ1a1YVeHtWTIRzYjBFVjllQx4US4c1NNNlMVlVN6FXW2$12; sback_access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhcGkuc2JhY2sudGVjaCIsImlhdCI6MTU5NTI3NDM4NSwiZXhwIjoxNTk1MzYwNzg1LCJhcGkiOiJ2MiIsImRhdGEiOnsiY2xpZW50X2lkIjoiNWQ4NTI0Y2U3MDY2MmViNGZjNDZkNWMxIiwiY2xpZW50X2RvbWFpbiI6ImNob3BwYnJhaG1hZXhwcmVzcy5jb20uYnIiLCJjdXN0b21lcl9pZCI6IjVlZjFmYzJhODlhOGRlMDg3YTViMTY2YyIsImN1c3RvbWVyX2Fub255bW91cyI6dHJ1ZSwiY29ubmVjdGlvbl9pZCI6IjVmMTA4ZTZmODFmYzE1ZWZjZDIzNDAzNCIsImFjY2Vzc19sZXZlbCI6ImN1c3RvbWVyIn19.FNQP86qO6KevvP9oxDZizINf4Mj20N2nQXGoAwXlzn8.WrWrDrHeDrgPEizRKqiYDr; sback_partner=false; blueID=c55d1372-79f4-4054-94b9-b0644f3ec381; sback_current_session=1; sback_total_sessions=1; sb_days=1595274385990; sback_customer_w=true; OptanonAlertBoxClosed=2020-07-20T19:46:27.554Z; _td_global=fdf9cb89-9be5-4e47-80d5-e5da7da4c664; AffiliateAutoSelect=1; notification_customer=1; frontend=58955c3cd2053cc0d93753b2b4434081; frontend_cid=LZQyB4H98dJX4Twq; _hjAbsoluteSessionInProgress=1; enext=60428756-6170-498f-ad44-37a0568a44d6; enext-s=8deda699-1a87-498d-830e-b7315d7cda0f; sback_refresh_wp=no; postcode_data=a%3A9%3A%7Bs%3A7%3A%22success%22%3Bb%3A1%3Bs%3A10%3A%22error_code%22%3BN%3Bs%3A8%3A%22postcode%22%3Bs%3A9%3A%2204563-000%22%3Bs%3A6%3A%22street%22%3Bs%3A27%3A%22Av+Pe+Ant%C3%B4nio+J+dos+Santos%22%3Bs%3A17%3A%22neighborhood_name%22%3Bs%3A16%3A%22Cidade+Mon%C3%A7%C3%B5es%22%3Bs%3A9%3A%22city_name%22%3Bs%3A10%3A%22S%C3%A3o+Paulo%22%3Bs%3A2%3A%22uf%22%3Bs%3A2%3A%22SP%22%3Bs%3A3%3A%22url%22%3Bs%3A8%3A%22brooklin%22%3Bs%3A12%3A%22affiliate_id%22%3Bs%3A6%3A%22189448%22%3B%7D; Affiliate=brooklin; LAST_CATEGORY=38; incap_ses_1229_2241603=dhvYRDVGj3z/HjcRC0kOEVj3FV8AAAAAYjdEDByM8/F73ys2GHfCoQ==; _hjIncludedInSample=1; __atuvc=15%7C30; __atuvs=5f15f5a793349a5300e; _st_no_script=1; _st_no_user=1; _uetsid=b75da4fc61f718d5d66bb9f64bb91295; _uetvid=e10dc62802c1e8cad581c473e6f34472; OptanonConsent=isIABGlobal=false&datestamp=Mon+Jul+20+2020+17%3A22%3A04+GMT-0300+(Hor%C3%A1rio+Padr%C3%A3o+de+Bras%C3%ADlia)&version=6.3.0&landingPath=NotLandingPage&groups=1%3A1%2C2%3A1%2C3%3A1%2C4%3A1%2C0_277070%3A1%2C0_236004%3A1%2C0_236006%3A1%2C0_236008%3A1%2C0_236010%3A1%2C0_236012%3A1%2C0_236014%3A1%2C0_236016%3A1%2C0_236019%3A1%2C0_278687%3A1%2C0_236021%3A1%2C0_278689%3A1%2C0_236023%3A1%2C0_236025%3A1%2C0_278685%3A1%2C0_236027%3A1%2C0_277071%3A1%2C0_236005%3A1%2C0_236007%3A1%2C0_236009%3A1%2C0_236011%3A1%2C0_236013%3A1%2C0_278690%3A1%2C0_236015%3A1%2C0_236017%3A1%2C0_236018%3A1%2C0_278686%3A1%2C0_236020%3A1%2C0_278688%3A1%2C0_236022%3A1%2C0_236024%3A1%2C0_236026%3A1&AwaitingReconsent=false; _vwo_sn=0%3A33; _spl_pv=29; _td=cf005ede-8721-4d37-bd52-9c1e8f8f289c");
    // When this crawler was created I didn't find another one to get this cookie

    Request request = Request.RequestBuilder.create().setCookies(cookies).setHeaders(headers).setUrl(url).build();
    String response = this.dataFetcher.get(session, request).getBody();

    return this.currentDoc = Jsoup.parse(response);

  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 20;

    this.log("Página " + this.currentPage);

    Document doc = (Document) fetch();

    String url = "https://choppbrahmaexpress.com.br/brooklin/catalogsearch/result/?q=" + this.keywordWithoutAccents;
    this.log("Link onde são feitos os crawlers: " + url);

    Elements products = doc.select(".products-grid .item");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {

        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".qty #qty", "product");
        String internalPid = internalId;
        String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-bg .product-image", "href");


        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }


}
