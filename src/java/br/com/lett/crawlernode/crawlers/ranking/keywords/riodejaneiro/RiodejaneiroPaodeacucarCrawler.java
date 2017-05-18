package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class RiodejaneiroPaodeacucarCrawler extends CrawlerRankingKeywords{

	public RiodejaneiroPaodeacucarCrawler(Session session) {
		super(session);
	}

	private List<Cookie> cookies = new ArrayList<Cookie>();

	@Override
	protected void processBeforeFetch() {
		if(this.cookies.size() < 1){
			// Criando cookie da loja 3 = Rio de Janeiro capital
			BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", "7");
			cookie.setDomain(".deliveryextra.com.br");
			cookie.setPath("/");
			cookie.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));
			
			this.cookies.add(cookie);
		}
	}
	
	@Override
	public void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 12;

		this.log("Página "+ this.currentPage);

		//monta a url com a keyword e a página
		String url = "http://busca.paodeacucar.com.br/search?p=Q&lbc=paodeacucar&w="+this.keywordEncoded+"&srt="+this.arrayProducts.size()+"&cnt=36";
		this.log("Link onde são feitos os crawlers: "+url);		

		//chama função de pegar a url
		this.currentDoc = fetchDocument(url, cookies);

		Elements id 	= this.currentDoc.select(".boxProduct .showcase-item__name a");
		Elements result = this.currentDoc.select("div#sli_noresult");

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:	
		if(id.size() >= 1 && result.size() < 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();

			for(Element e: id){
				// Url do produto
				String urlProduct = crawlProductUrl(e);

				// InternalPid
				String internalPid = crawlInternalPid(e);

				// InternalId
				String internalId = crawlInternalId(urlProduct);

				saveDataProduct(internalId, internalPid, urlProduct);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");

	}

	@Override
	protected boolean hasNextPage(){
		Element page = this.currentDoc.select("span.disabled.button.button-rounded.button-rounded--light-gray.icon.icon--angle-right").first();

		//se  elemeno page obtiver algum resultado
		if(page != null) return false;

		return true;
	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("span.sli_bct_total_records").first();

		try {
			if(totalElement != null) this.totalProducts = Integer.parseInt(totalElement.text());
		} catch(Exception e) {
			this.logError(e.getMessage());
		}

		this.log("Total da busca: "+this.totalProducts);
	}

	private String crawlInternalId(String url){
		String internalId = null;

		String[] tokens = url.split("/");
		internalId = tokens[tokens.length-1];

		return internalId;
	}

	private String crawlInternalPid(Element e){
		String internalPid = null;		

		return internalPid;
	}

	private String crawlProductUrl(Element e){
		String urlProduct = e.attr("title");

		return urlProduct;
	}
}
