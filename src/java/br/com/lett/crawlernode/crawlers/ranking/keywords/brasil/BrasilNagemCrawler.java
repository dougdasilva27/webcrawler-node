package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilNagemCrawler extends CrawlerRankingKeywords {

	public BrasilNagemCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 10;

		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.nagem.com.br/modulos/navegacao/busca.php?b="+this.keywordEncoded+"&q=50&p="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);			
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select("form#frmListaProdutos > table");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) {
				setTotalBusca();
			}
						
			for(Element e : products) {
				//InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				//InternalId
				String internalId = internalPid;
				
				//monta a url
				String productUrl = crawlProductUrl(e);
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) {
					break;
				}
				
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		
	}

	@Override
	protected boolean hasNextPage() {
		if(this.arrayProducts.size() < this.totalBusca) {
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void setTotalBusca() {
		Element totalElement = this.currentDoc.select("p.qtd-encontrados span").first();
		
		try {
			if(totalElement != null) {
				this.totalBusca = Integer.parseInt(totalElement.text());
			}
		} catch(Exception e) {
			this.logError(CommonMethods.getStackTrace(e));
		}
		
		this.log("Total da busca: "+this.totalBusca);
	}

	
	private String crawlInternalPid(Element e){
		String internalPid = e.attr("id");;
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		Element urlElement = e.select("td.dtl-produto td > a[href]").first();
		
		if(urlElement != null){
			urlProduct = urlElement.attr("href");
			
			if(!urlProduct.contains("nagem")){
				urlProduct = "http://www.nagem.com.br/" + urlElement.attr("href");
			}
		}
		
		return urlProduct;
	}
}
