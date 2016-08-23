package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

public class BrasilRicardoeletroCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.ricardoeletro.com.br/";

	public BrasilRicardoeletroCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// ID interno
			String internalId = null;
			Element elementInternalID = doc.select("#ProdutoDetalhesCodigoProduto").first();
			if (elementInternalID != null) {
				internalId = elementInternalID.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").trim();
			}

			// Pid
			// está igual o internalId porque o internalId mudará depois
			String internalPid = internalId;

			// Nome
			String name = null;
			Element elementName = doc.select("#ProdutoDetalhesNomeProduto h1").first();
			if (elementName != null) {
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
			Elements elementPrimaryImages = doc.select("#ProdutoDetalhesFotosFotosPequenas").select("a.zoom-gallery img");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element e : elementPrimaryImages) {

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
			Element elementDescription = doc.select("#ProdutoDescricao").first();
			if(elementDescription != null) description = elementDescription.html().replace("’","").trim();

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setSeedId(this.session.getSeedId());
			product.setUrl(this.session.getUrl());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.startsWith("http://www.ricardoeletro.com.br/Produto/");
	}
}
