package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.models.Product;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloExtramarketplaceCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.extra.com.br/";

	private static final String PARTNERS_URL_REPLACEMENT_PATTERN = "\\d+\\.html.*";

	public SaopauloExtramarketplaceCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		boolean x = !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
		return x;
	}


	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		Element elementInternalID = doc.select(".produtoNome h1 span").first();

		if(elementInternalID != null) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// Pegando url padrão no doc da página, para lidar com casos onde tem url em formato diferente no banco
			String url = "http://www.extra.com.br" + doc.select("form[name]").attr("action").trim();

			// ID interno
			String internalID = Integer.toString(Integer.parseInt(elementInternalID.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "")));				

			// Pid
			String internalPid = null;
			Element elementInternalPid = doc.select("input#ctl00_Conteudo_hdnIdSkuSelecionado").first();
			if (elementInternalPid != null) {
				internalPid = elementInternalPid.attr("value").trim();
			}

			// Nome
			Elements elementName = doc.select(".produtoNome h1 b");
			String name = elementName.text().replace("'","").replace("’","").trim();

			// Categorias
			Elements element_categories = doc.select(".breadcrumb a"); 
			String category1;
			String category2; 
			String category3;

			String[] cat = new String[4];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			cat[3] = "";

			int j=0;
			for(int i=0; i < element_categories.size(); i++) {
				Element e = element_categories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "");
				j++;
			}
			category1 = cat[1];
			category2 = cat[2];
			category3 = cat[3];

			// Imagem primaria
			Elements elementPrimaryImage = doc.select(".photo");
			String primaryImage = elementPrimaryImage.attr("src");

			// Imagens secundarias
			Elements elementSecondaryImages = doc.select(".carouselBox ul li a");
			String secondaryImages = null;
			JSONArray secundarias_array = new JSONArray();

			if(elementSecondaryImages.size() > 1) {
				for(int i = 1; i < elementSecondaryImages.size(); i++) { // primeira imagem é a primaria
					Element e = elementSecondaryImages.get(i);
					String img = e.attr("href").toString();
					secundarias_array.put( img );
				}
			}
			if(secundarias_array.length() > 0) {
				secondaryImages = secundarias_array.toString();
			}

			// Descrição
			String description = "";
			Elements elementDescricao = doc.select("#detalhes");
			description = elementDescricao.first().html();

			JSONArray marketplace = new JSONArray();
			Integer stock = null;
			Float price = null;
			boolean available = false;

			String urlMarketplaceInfo = (url.split(".html")[0] + "/lista-de-lojistas.html");
			urlMarketplaceInfo = url.replaceAll(PARTNERS_URL_REPLACEMENT_PATTERN, internalID + "/lista-de-lojistas.html");

			Document docMarketplaceInfo = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlMarketplaceInfo, null, null);

			Elements lines = docMarketplaceInfo.select("table#sellerList tbody tr");

			for(Element linePartner: lines) {

				String partnerName = linePartner.select("a.seller").first().text().trim().toLowerCase();
				Float partnerPrice = Float.parseFloat(linePartner.select(".valor").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

				if(partnerName.equals("extra")) {

					available = true;
					price = partnerPrice;

				} else {

					JSONObject partner = new JSONObject();
					partner.put("name", partnerName);
					partner.put("price", partnerPrice);

					marketplace.put(partner);

				}

			}

			// Olhar por outro produto no caso de seletores de voltagem para o produto
			Elements elementVoltageSelector = doc.select(".produtoSku option[value]:not([value=\"\"])");

			if(elementVoltageSelector.size() > 0) { // inserir informações dos dois produtos, o de 110v e o de 220v

				Elements elementsProducts = elementVoltageSelector.select("option");
				Element elementFirstProduct = elementsProducts.get(0);
				Element elementSecondProduct = elementsProducts.get(1);
				String textFirstProduct = elementFirstProduct.text();
				String textSecondProduct = elementSecondProduct.text();

				/* 
				 * Inserir o produto de 110V 
				 */

				String nameFirstProduct = name + " - 110V";
				String internalIdFirstProduct = elementFirstProduct.attr("value");
				boolean availableFirstProduct = true;
				Float priceFirstProduct = null;					

				if(textFirstProduct.contains("Produto Esgotado")) {
					availableFirstProduct = false;
				}
				if(availableFirstProduct) { // se o produto está disponível, então pegar o preço na string
					if(textFirstProduct.contains("|")) {
						priceFirstProduct = parsePrice(textFirstProduct);
					}
					else {
						priceFirstProduct = price;
					}
				}

				Product firstProduct = new Product();
				firstProduct.setSeedId(this.session.getSeedId());
				firstProduct.setUrl(url);
				firstProduct.setInternalId(internalIdFirstProduct);
				firstProduct.setInternalPid(internalPid);
				firstProduct.setName(nameFirstProduct);
				firstProduct.setPrice(priceFirstProduct);
				firstProduct.setCategory1(category1);
				firstProduct.setCategory2(category2);
				firstProduct.setCategory3(category3);
				firstProduct.setPrimaryImage(primaryImage);
				firstProduct.setSecondaryImages(secondaryImages);
				firstProduct.setDescription(description);
				firstProduct.setStock(stock);
				firstProduct.setMarketplace(marketplace);
				firstProduct.setAvailable(availableFirstProduct);

				products.add(firstProduct);					

				/*
				 * Inserir produto de 220V
				 */

				String nameSecondProduct = name + " - 220V";
				String internalIdSecondProduct = elementSecondProduct.attr("value");
				boolean availableSecondProduct = true;
				Float priceSecondProduct = null;

				if(textSecondProduct.contains("Produto Esgotado")) {
					availableSecondProduct = false;
				}
				if(availableSecondProduct) { // se o produto está disponível, então pegar o preço na string
					if(textSecondProduct.contains("|")) {
						priceSecondProduct = parsePrice(textSecondProduct);
					}
					else {
						priceSecondProduct = price;
					}
				}

				Product secondProduct = new Product();
				secondProduct.setSeedId(this.session.getSeedId());
				secondProduct.setUrl(url);
				secondProduct.setInternalId(internalIdSecondProduct);
				secondProduct.setInternalPid(internalPid);
				secondProduct.setName(nameSecondProduct);
				secondProduct.setPrice(priceSecondProduct);
				secondProduct.setCategory1(category1);
				secondProduct.setCategory1(category2);
				secondProduct.setCategory1(category3);
				secondProduct.setPrimaryImage(primaryImage);
				secondProduct.setSecondaryImages(secondaryImages);
				secondProduct.setDescription(description);
				secondProduct.setStock(stock);
				secondProduct.setMarketplace(marketplace);
				secondProduct.setAvailable(availableSecondProduct);

				products.add(secondProduct);

			}

			else { // se tem apenas um produto na página, insira apenas o que já foi capturado anteriormente

				Product product = new Product();
				product.setSeedId(this.session.getSeedId());
				product.setUrl(url);
				product.setInternalId(internalID);
				product.setInternalPid(internalPid);
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


	private Float parsePrice(String textProduct) {
		int begin = textProduct.indexOf('$') + 1;
		String priceString = textProduct.substring(begin);

		return Float.parseFloat(priceString.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
	}


}