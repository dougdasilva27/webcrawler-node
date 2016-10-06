package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;


public class BrasilInsinuanteCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.insinuante.com.br";

	public BrasilInsinuanteCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			String internalID = null;
			Element elementInternalID = doc.select("#ProdutoDetalhesCodigoProduto").first();
			if(elementInternalID != null) {
				internalID = elementInternalID.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").trim();
			}				

			// Nome
			String name = null;
			Element elementName = doc.select("#ProdutoDetalhesNomeProduto h1").first();
			if(elementName != null) {
				name = elementName.text().replace("'","").replace("’","").trim();
			}

			// Preço
			Float price = null;
			Element elementPrice = doc.select("#ProdutoDetalhesPrecoComprarAgoraPrecoDePreco").first();
			if(elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Disponibilidade
			boolean available = true;
			Element elementBuyButton = doc.select("#btnComprar").first();
			if(elementBuyButton == null) {
				available = false;
			}

			// Categorias
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementCategories = doc.select("#Breadcrumbs .breadcrumbs-itens").select("a");

			for(Element e : elementCategories) {
				if(category1.isEmpty()) {
					category1 = e.text();
				}
				else if(category2.isEmpty()) {
					category2 = e.text();
				}
				else if(category3.isEmpty()) {
					category3 = e.text();
				}
			}

			// Imagens
			Elements elementImages = doc.select("#ProdutoDetalhesFotosFotosPequenas").select("a img");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element e : elementImages) {

				// Tirando o 87x87 para pegar imagem original
				if(primaryImage == null) {
					primaryImage = e.attr("src").replace("/87x87", "");
				} 
				else {
					secondaryImagesArray.put(e.attr("src").replace("/87x87", ""));
				}

			}

			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";   
			Element elementDescricao = doc.select(".produto-descricao-texto-descricao").first();
			if(elementDescricao != null) {
				description = elementDescricao.html().replace("’", "").trim();
			}	

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalID);
			product.setName(name);
			product.setPrice(price);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);
			product.setAvailable(available);
			
			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.startsWith("http://www.insinuante.com.br/Produto/");
	}
}
