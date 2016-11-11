package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

public class BrasilNagemCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.nagem.com.br/";

	public BrasilNagemCrawler(Session session) {
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

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			String internalId = null;
			Element internalIdElement = doc.select("div #nome_produto").first();
			if (internalIdElement != null) {
				List<TextNode> textNodes = internalIdElement.textNodes();
				for (TextNode text : textNodes) {
					if (text.toString().contains("Código")) {
						internalId = text.toString().split(":")[1].trim();
					}
				}
			}			

			// Pid
			String internalPid = internalId;

			// Nome
			String name = null;
			Element elementName = doc.select("span.tituloProduto.produtoDescricao").first();
			if(elementName != null) {
				name = elementName.text().trim();
			}

			// Disponibilidade
			boolean available = true;
			Element elementNotifyMe = doc.select(".btn-avise").first();
			if (elementNotifyMe != null) {
				available = false;
			}

			// Preço
			Float price = null;
			if(available) {
				Element elementPriceDescricao = doc.select("span.precoDetalhe.precoDescricao").first();
				if (elementPriceDescricao != null) {
					price = Float.parseFloat(elementPriceDescricao.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				} else {
					Element elementPrice = doc.select("span.precoDetalhe").first();
					if(elementPrice != null) {
						price = Float.parseFloat(elementPrice.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
					}
				}

			}

			// Categoria -- só temos category1 nesse site
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Element category1Element = doc.select("#divSubtituloGeral .subtituloCentro-texto a").first();
			category1 = category1Element.text().trim();

			// Images

			// Capturando imagens de dentro do script javascript
			String script = getImagesScript(doc);
			String line = getImagesLine(script);
			ArrayList<String> images = extractImagesFromScriptLine(line);			

			// Imagem primária
			String primaryImage = images.get(0);

			// Imagens secundárias
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for (int i = 1; i < images.size(); i++) { // a primeira imagem do array é a primária
				secondaryImagesArray.put(images.get(i));
			}			
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementComercialText = doc.select("#divTextoComercial").first();
			Element elementSpecs = doc.select("#divEspecificacao").first();
			if (elementComercialText != null) {
				description = description + elementComercialText.html();
			}
			if (elementSpecs != null) {
				description = description + elementSpecs.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;
			
			// Prices
			Prices prices = crawlPrices(internalId, price, crawlBankTicketPrice(doc));

			Product product = new Product();
			
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
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

	private String getImagesScript(Document doc) {
		Elements scripts = doc.select("script");
		for (Element script : scripts) {
			if (script.outerHtml().contains("detalheProdutoImagem")) {
				return script.outerHtml();
			}
		}

		return null;
	}

	private String getImagesLine(String script) {
		Scanner scanner = new Scanner(script);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.startsWith("var aImagens")) {
				scanner.close();
				return line;
			}
		}

		scanner.close();

		return null;
	}

	private ArrayList<String> extractImagesFromScriptLine(String line) {
		ArrayList<String> images = new ArrayList<String>();
		String[] tokens = line.split(";");

		for (String token : tokens) {
			if (token.startsWith("aImagens[")) {
				images.add(token.split("\"")[1].replace("\"", "").trim());
			}
		}

		return images;
	}
	
	private Float crawlBankTicketPrice(Document doc){
		Float bankTicketPrice = null;
		Element boleto = doc.select(".detalhePrecoBoleto strong").first();
		
		if(boleto != null){
			bankTicketPrice = Float.parseFloat(boleto.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}
		
		return bankTicketPrice;
	}
	
	private Prices crawlPrices(String internalId, Float price, Float bankTicketPrice){
		Prices prices = new Prices();
		
		if(price != null){
			String url = "http://www.nagem.com.br/modulos/produto/ajaxparcelamento.php?requestTime=1477589708033&cp=" + internalId;
			
			JSONObject jsonPrices = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, cookies);
			
			if(jsonPrices.has("html")){
				String html = jsonPrices.getString("html").replaceAll("\\t", "").replaceAll("\\n", "");
				Document doc = Jsoup.parse(html);

				prices.insertBankTicket(bankTicketPrice);				
				
				Elements cardsElements = doc.select("#selDetalheParcelamento" + internalId + " option");
				
				for(Element e : cardsElements){
					String text = e.text().toLowerCase();
					
					if(text.contains("visa")){
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), internalId);
						prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
						
					} else if(text.contains("mastercard")){
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), internalId);
						prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
						
					} else if(text.contains("diners")){
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), internalId);
						prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
						
					} else if(text.contains("american") || text.contains("amex")) {
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), internalId);
						prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);					
						
					} else if(text.contains("hipercard") || text.contains("amex")){
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), internalId);
						prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
						
					} else if(text.contains("credicard") ){
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), internalId);
						prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);					
					}
				} 
			
			}
		}
		
		return prices;
	}
	
	private Map<Integer,Float> getInstallmentsForCard(Document doc, String idCard, String id){
		Map<Integer,Float> mapInstallments = new HashMap<>();
		
		Elements installments = doc.select("#divDetalheParcelamento"+ id +"_" + idCard + " li");
		
		for(Element e : installments){
			String text = e.ownText().trim().toLowerCase();
			
			if(!text.isEmpty()){
				int x = text.indexOf("x");
				
				Integer installment = Integer.parseInt(text.substring(0, x).trim());
				Float value = Float.parseFloat(text.substring(x+1).replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				
				mapInstallments.put(installment, value);
			}
		}
		
		return mapInstallments;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		Element elementProduct = document.select(".tbl-detalheProdutoInterno").first();
		return elementProduct != null;
	}
}
