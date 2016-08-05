package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.models.Product;
import br.com.lett.crawlernode.util.Logging;

public class BrasilCitylarCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.citylar.com.br/";

	public BrasilCitylarCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl(), doc) ) {

			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// ID interno
			Element elementInternalID = doc.select("#ProdutoDetalhesCodigoProduto").first();
			String internalID = elementInternalID.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").trim();

			// Nome
			Element elementName = doc.select("#ProdutoDetalhesNomeProduto h1").first();
			String name = elementName.text().replace("'", "").replace("’", "").trim();

			// Preço
			Float price = null;
			Element elementPrice = doc.select("#ProdutoDetalhesPrecoComprarAgoraPrecoDePreco").first();
			if (elementPrice == null) {
				price = null;
			} else {
				price = Float.parseFloat(
						elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Disponibilidade
			boolean available = true;
			Element elementBuyButton = doc.select("#btnComprar").first();
			if (elementBuyButton == null) {
				available = false;
			}

			// Categorias
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements element_categories = doc.select("#Breadcrumbs .breadcrumbs-itens").select("a");
			for (Element e : element_categories) {
				if (category1.isEmpty()) {
					category1 = e.text();
				} else if (category2.isEmpty()) {
					category2 = e.text();
				} else if (category3.isEmpty()) {
					category3 = e.text();
				}
			}

			// Imagens
			Elements elementImages = doc.select("#ProdutoDetalhesFotosFotosPequenas").select("a img");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secundarias_array = new JSONArray();

			for (Element e : elementImages) {

				// Tirando o 87x87 para pegar imagem original
				if (primaryImage == null) {
					primaryImage = e.attr("src").replace("/87x87", "");
				} else {
					secundarias_array.put(e.attr("src").replace("/87x87", ""));
				}

			}

			if (secundarias_array.length() > 0) {
				secondaryImages = secundarias_array.toString();
			}

			// Descrição
			String description = "";
			Element element_descricao = doc.select("#ProdutoDescricao").first();
			if (element_descricao != null) {
				description = element_descricao.html().replace("’", "").trim();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Elements elementsProductVariation = doc.select("#ProdutoDetalhesDoProduto .atributo .input-container option");

			// tem mais de um produto
			if (elementsProductVariation.size() > 1) {

				// inserir cada produto
				for (Element variation : elementsProductVariation) {

					String variationUrl = variation.attr("link");
					String variationName = name + " - "
							+ variation.text().substring(0, variation.text().indexOf('|')).trim();
					String variationInternalID = variation.attr("value");
					boolean variationAvailable = available;
					Float variationPrice = price;

					if (!variationUrl.equals(this.session.getUrl())) { 

						// se não for a url que
						// já tenho preciso
						// dar um fetch na
						// nova url e colher
						// os dados que
						// faltam

						Document variationDocument = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, variationUrl, null, null);

						// Disponibilidade
						Element elementVariationBuyButton = variationDocument.select("#btnComprar").first();
						variationAvailable = true;
						if (elementVariationBuyButton == null) {
							variationAvailable = false;
						}

						// Preço
						variationPrice = null;
						Element elementVariationPrice = variationDocument
								.select("#ProdutoDetalhesPrecoComprarAgoraPrecoDePreco").first();
						if (elementVariationPrice == null) {
							variationPrice = null;
						} else {
							variationPrice = Float.parseFloat(elementVariationPrice.text()
									.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
						}
					}


					Product product = new Product();
					product.setSeedId(this.session.getUrl());
					product.setUrl(variationUrl);
					product.setInternalId(variationInternalID);
					product.setName(variationName);
					product.setPrice(variationPrice);
					product.setCategory1(category1);
					product.setCategory2(category2);
					product.setCategory3(category3);
					product.setPrimaryImage(primaryImage);
					product.setSecondaryImages(secondaryImages);
					product.setDescription(description);
					product.setStock(stock);
					product.setMarketplace(marketplace);
					product.setAvailable(variationAvailable);

					products.add(product);

				}

			}

			// tem um produto apenas
			else {

				Product product = new Product();
				product.setSeedId(this.session.getSeedId());
				product.setUrl(this.session.getUrl());
				product.setInternalId(internalID);
				product.setName(name);
				product.setPrice(price);
				product.setCategory1(category1);
				product.setCategory1(category2);
				product.setCategory1(category3);
				product.setPrimaryImage(primaryImage);
				product.setSecondaryImages(secondaryImages);
				product.setDescription(description);
				product.setStock(stock);
				product.setMarketplace(marketplace);
				product.setAvailable(available);

				products.add(product);

			}



		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}


	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document document) {
		Element elementProduct = document.select("#ProdutoDetalhes").first();
		return url.startsWith("http://www.citylar.com.br/Produto/") && (elementProduct != null);
	}
}
