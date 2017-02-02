package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilRogeCrawler extends CrawlerRankingKeywords{

	public BrasilRogeCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 20;
	
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "https://www.roge.com.br/Produtos/ListagemProdutos?parametros="+ this.keywordEncoded +"&pg="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Elements products =  this.currentDoc.select("#visaoProdutos div > a[style]");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {			
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();
			for(Element e : products) {
				
				// InternalPid
				String internalPid 	= crawlInternalPid(e);
				
				// InternalId
				String internalId 	= crawlInternalId(e);
				
				// Url do produto
				String urlProduct = crawlProductUrl(e);
				
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
	protected boolean hasNextPage() {
				
		//se  elemeno page obtiver algum resultado
		if(this.arrayProducts.size() < this.totalBusca){
			//tem próxima página
			return true;
		} 
			
		return false;
		
	}
	
	@Override
	protected void setTotalBusca()	{
		Element totalElement = this.currentDoc.select("#lblQtdProdutos").first();
		
		if(totalElement != null) { 	
			try	{				
				this.totalBusca = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
			} catch(Exception e) {
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private String crawlInternalId(Element e){
		String internalId = null;
		Element idElement = e.select("#codigoProduto").first();
		
		if(idElement != null){
			internalId = idElement.text().replaceAll("[^0-9]", "").trim();
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Element e){
		String internalPid = null;
		
		return internalPid;
	}
	
	private String crawlProductUrl(Element e){
		String urlProduct = null;
		
		urlProduct = e.attr("href");
			
		if(!urlProduct.contains("roge")){
			urlProduct = "https://www.roge.com.br/" + e.attr("href");
		}
		
		return urlProduct;
	}
}
