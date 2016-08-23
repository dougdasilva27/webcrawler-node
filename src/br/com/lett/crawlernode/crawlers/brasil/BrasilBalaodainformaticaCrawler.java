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


public class BrasilBalaodainformaticaCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.balaodainformatica.com.br/";

	public BrasilBalaodainformaticaCrawler(CrawlerSession session) {
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
			Element elementInternalID = doc.select(".produto-detalhe p").first();
			if (elementInternalID != null) {
				int begin = elementInternalID.text().indexOf(':') + 1;
				internalId = elementInternalID.text().substring(begin).trim();
			}
			if (internalId == null) {
				Logging.printLogError(logger, session, "Não encontrei id interno para o produto na URL: " + this.session.getUrl());
				return products;
			}

			// Pid
			String internalPid = internalId;

			// Nome
			Element elementProduct = doc.select("#content-center").first();
			Element element_name = elementProduct.select("#nome h1").first();
			String name = element_name.text().replace("'", "").replace("’", "").trim();

			// Disponibilidade
			boolean available = true;
			Element elementBuyButton = elementProduct.select("#btnAdicionarCarrinho").first();
			if (elementBuyButton == null) {
				available = false;
			}

			// Preço
			Float price = null;
			Element elementPrice = elementProduct.select("#preco-comprar .avista").first();
			if (elementPrice != null) {
				price = Float.parseFloat(
						elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Categoria
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Element elementCategories = elementProduct.select("h2").first();
			String[] categories = elementCategories.text().split(">");
			for (String c : categories) {
				if (category1.isEmpty()) {
					category1 = c.trim();
				} else if (category2.isEmpty()) {
					category2 = c.trim();
				} else if (category3.isEmpty()) {
					category3 = c.trim();
				}
			}

			// Imagens secundárias e primária
			Elements elementImages = elementProduct.select("#imagens-minis img");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			if (elementImages.isEmpty()) {

				Element elementImage = elementProduct.select("#imagem-principal img").first();
				if (elementImage != null) {
					primaryImage = elementImage.attr("src");
				}

			} else {
				for (Element e : elementImages) {
					if (primaryImage == null) {
						primaryImage = e.attr("src").replace("imagem2", "imagem1");
					} else {
						secondaryImagesArray.put(e.attr("src").replace("imagem2", "imagem1"));
					}
				}
			}

			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescription = doc.select("#especificacoes").first();
			if (elementDescription != null) {
				description = elementDescription.html().replace("’", "").trim();
			}

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
			Logging.printLogDebug(logger, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (
				url.startsWith("https://www.balaodainformatica.com.br/Produto/")
				|| url.startsWith("http://www.balaodainformatica.com.br/Produto/") 
				|| url.startsWith("https://www.balaodainformatica.com.br/ProdutoAnuncio/")
				|| url.startsWith("http://www.balaodainformatica.com.br/ProdutoAnuncio/")
				);
	}
}
