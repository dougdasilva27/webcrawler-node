package br.com.lett.crawlernode.processor.controller;

import java.io.File;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.processor.base.DigitalContentAnalyser;
import br.com.lett.crawlernode.processor.base.Queries;
import br.com.lett.crawlernode.processor.base.ReplacementMaps;
import br.com.lett.crawlernode.processor.digitalcontent.Pic;
import br.com.lett.crawlernode.processor.digitalcontent.RulesEvaluation;
import br.com.lett.crawlernode.processor.extractors.ExtractorFlorianopolisAngeloni;
import br.com.lett.crawlernode.processor.models.BrandModel;
import br.com.lett.crawlernode.processor.models.ClassModel;
import br.com.lett.crawlernode.processor.models.ProcessedModel;
import br.com.lett.crawlernode.server.S3Service;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.processor.base.Extractor;
import br.com.lett.crawlernode.processor.base.IdentificationLists;

import com.mongodb.client.MongoDatabase;

/**
 * Classe responsável por processar produtos da tabela crawler a fim de transformá-los em
 * produtos legíves para serem encaminhados para a tabela processed
 * 
 * @author Doug
 */
public class ResultManager {

	private static final Logger logger = LoggerFactory.getLogger(ResultManager.class);

	// Mongo para o SIFT similarity
	private MongoDatabase mongo;

	// Banco de dados, para usar Postgres
	private DatabaseManager db;

	// Mapas de Substituição
	private Map<String, String> unitsReplaceMap;
	private Map<String, String> recipientsReplaceMap;

	// Listas de identificação
	private List<String> recipientsList;
	private List<String> unitsList;
	private List<ClassModel> classModelList;
	private List<BrandModel> brandModelList;

	// Variáveis de controle
	private boolean logActivated;

	// Variáveis de apoio
	private DateFormat isoDateFormat;
	private Map<Integer, String> cityNameInfo;
	private Map<Integer, String> marketNameInfo;

	// Variável usada no teste de processer de um determinado supermercado
	private ArrayList<Integer> marketid;

	/**
	 * Construtor do ResultManager chamado pelo Crawler
	 * 
	 * @category Construtor
	 * @author fabricio
	 * @param activateLogging - Define a ativação ou não dos logs
	 */
	public ResultManager(
			boolean activateLogging, 
			MongoDatabase mongo, 
			DatabaseManager db
			) throws NullPointerException {

		this.db = db;
		this.mongo = mongo;

		this.initialize(activateLogging);
	}

	/**
	 * Construtora responsável pela inicialização dos mapas de identificação e substituição
	 * 
	 * @param activateLogging display log messages
	 */
	public ResultManager(boolean activateLogging) throws NullPointerException {
		this.initialize(activateLogging);
	}

	/**
	 * Função responsável pela inicialização do ResultManager
	 * 
	 * @author fabricio
	 */
	private void initialize(boolean activateLogging) throws NullPointerException {

		// Busca hora e data atual no sistema
		this.isoDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		this.logActivated = activateLogging;

		// Cria as informaçẽos do supermercado
		this.createMarketInfo();

		// Inicializa os mapas de substituição
		this.unitsReplaceMap = new LinkedHashMap<String, String>();
		this.recipientsReplaceMap = new LinkedHashMap<String, String>();

		// Inicializa os mapas de identificação
		this.recipientsList = new ArrayList<String>();
		this.unitsList = new ArrayList<String>();
		this.classModelList = new ArrayList<ClassModel>();
		this.brandModelList = new ArrayList<BrandModel>();

		// Cria Modelo de manipulação de Marcas para substituição e identificação
		try {

			// ResultSet com resultados da consulta das classes
			ResultSet rs = this.db.runSqlConsult(Queries.queryForLettBrandProducts);

			// Enquanto houver linhas...
			while(rs.next()) {

				// Verificação se a marca deve ser ignorada
				// Exemplo: papel higiênico leve 3 pague 2 PAMPERS
				// LEVE também é uma marca, mas queremos PAMPERS, por isso LEVE não será incluída na nossa lista de marcas
				if(!rs.getBoolean("ignored")){		
					BrandModel aux = null;

					// Procurando se já existe a marca na lista de marcas
					for(BrandModel bm: brandModelList) {
						if(bm.getBrand().equals(rs.getString("denomination"))) {
							aux = bm;
							break;
						}
					}

					// Se não existe, cria um novo BrandModel e o incluí na lista de brandsModelList
					if(aux == null) {
						aux = new BrandModel(rs.getString("denomination"), rs.getString("lett_supplier"));
						aux.putOnList(rs.getString("mistake"));
						brandModelList.add(aux);
					}

					// Caso já exista uma incidência, apenas adiciona o erro no mapa
					else {
						aux.putOnList(rs.getString("mistake"));
					}
				}
			}

			// close the result set
			rs.close();

		} catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		// Completa marcas ou corrige marcas através da lista de marcas	
		for(BrandModel bm : brandModelList) {

			// Automação 1: marcas sem espaço
			// Exemplo: Marca de nome: jack daniels
			//          Nós adicionaremos automáticamente com brandsReplaceMap e suas combinações: 
			//          jackdaniels (errado) -> jack daniels (correto)
			if(bm.getBrand().contains(" ")) {
				bm.putOnList(bm.getBrand().replace(" ", ""));

				Pattern space = Pattern.compile(" ");
				Matcher matcherSpace = space.matcher(bm.getBrand());
				while (matcherSpace.find()) {
					bm.putOnList(bm.getBrand().substring(0, matcherSpace.start()) + bm.getBrand().substring(matcherSpace.end(), bm.getBrand().length()));
				}
			}

			// Automação 2: marcas com hifén.
			// Exemplo: marca de nome: arco-íris
			//          Nós adicionaremos automáticamente com brandsReplaceMap e suas combinações: 
			//          arco íris (errado) -> arco-íris (correto)
			//          arcoíris (errado)  -> arco-íris (correto)
			if(bm.getBrand().contains("-")) {
				bm.putOnList(bm.getBrand().replace("-", ""));
				bm.putOnList(bm.getBrand().replace("-", " "));

				Pattern space = Pattern.compile("-");
				Matcher matcherSpace = space.matcher(bm.getBrand());
				while (matcherSpace.find()) {
					bm.putOnList(bm.getBrand().substring(0, matcherSpace.start()) + bm.getBrand().substring(matcherSpace.end(), bm.getBrand().length()));
					bm.putOnList(bm.getBrand().substring(0, matcherSpace.start()) + " " + bm.getBrand().substring(matcherSpace.end(), bm.getBrand().length()));
				}
			}
		}

		// Cria listas de identificação de recipientes
		this.recipientsList = IdentificationLists.recipientsList;

		// Cria listas de identificação de unidades
		this.unitsList = IdentificationLists.unitsList;

		// Cria listas de substituição de unidades
		this.unitsReplaceMap = ReplacementMaps.unitsReplaceMap;

		// Cria listas de substituição de recipientes
		this.recipientsReplaceMap = ReplacementMaps.recipientsReplaceMap;

		// Cria Modelo de manipulação da classes para torná-las LettClasses
		try{

			// ResultSet com resultados da consulta das classes
			ResultSet rs = this.db.runSqlConsult(Queries.queryForLettClassProducts);

			// Enquanto houver linhas...
			while (rs.next()) {

				ClassModel aux = null;

				// Procurando se já existe na lista a classe
				for(ClassModel lcm: classModelList) {
					if(lcm.getLettName().equals(rs.getString("denomination"))) {
						aux = lcm;
						break;
					}
				}

				// Se não existe, cria um nome lettClassesModel e adiciona na lista
				if (aux == null) {
					aux = new ClassModel(rs.getString("denomination"));
					aux.putOnMap(rs.getString("mistake"), rs.getString("extra"));
					classModelList.add(aux);
				} 
				// Caso já exista uma incidência, apenas adiciona o erro no mapa
				else {
					aux.putOnMap(rs.getString("mistake"), rs.getString("extra"));
				}
			}
			// Encerra o ResultSet
			rs.close();

			// Trata exceções do ResultSet
		}	catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		// Completa classes ou corrige classes através da lista de classes	
		for (ClassModel cm : classModelList){

			Pattern space = Pattern.compile(" ");

			// Automação 1: marcas sem espaço
			// Exemplo: Marca de nome: jack daniels
			//          Nós adicionaremos automaticamente com brandsReplaceMap e suas combinações: 
			//          jackdaniels (errado) -> jack daniels (correto)
			if (cm.getLettName().contains(" ")) {

				if(!cm.getMistakes().containsKey(cm.getLettName().replace(" ", ""))) 	cm.putOnMap(cm.getLettName().replace(" ", ""), "");
				if(!cm.getMistakes().containsKey(cm.getLettName().replace(" ", "-"))) 	cm.putOnMap(cm.getLettName().replace(" ", "-"), "");

				Matcher matcherSpace = space.matcher(cm.getLettName());
				while(matcherSpace.find()){
					cm.putOnMap(cm.getLettName().substring(0, matcherSpace.start()) + cm.getLettName().substring(matcherSpace.end(), cm.getLettName().length()), "");
				}
			}

			// Automação 2: marcas com hifén.
			// Exemplo: marca de nome: arco-íris
			//          Nós adicionaremos automáticamente com brandsReplaceMap e suas combinações: 
			//          arco íris (errado) -> arco-íris (correto)
			//          arcoíris (errado)  -> arco-íris (correto)
			if (cm.getLettName().contains("-")) {

				if(!cm.getMistakes().containsKey(cm.getLettName().replace("-", ""))) 	cm.putOnMap(cm.getLettName().replace("-", ""), "");
				if(!cm.getMistakes().containsKey(cm.getLettName().replace("-", " "))) 	cm.putOnMap(cm.getLettName().replace("-", " "), "");

				Matcher matcherSpace = space.matcher(cm.getLettName());
				while(matcherSpace.find()){
					cm.putOnMap(cm.getLettName().substring(0, matcherSpace.start()) + cm.getLettName().substring(matcherSpace.end(), cm.getLettName().length()), "");
				}
			}
		}
		Logging.printLogDebug(logger, "success!");

	}

	/**
	 * Extrai informações a partir dos campos "original_" do ProcessModel, e por fim
	 * salva os dados recebidos dentro do ProcessModel a ser retornado.
	 * 
	 * @author Fabricio
	 * @param cm Recebe valores do Crawler e os transfere para o ProcessModel
	 * @return pm Retorna processModel com valores do Crawler 
	 */
	public ProcessedModel processProduct(ProcessedModel pm, CrawlerSession session) {	
		Logging.printLogDebug(logger, session, "Processing product in ResultManager...");

		// preventing extra field to be null
		if (pm.getExtra() == null) {
			pm.setExtra("");
		}

		Extractor extractor = new ExtractorFlorianopolisAngeloni();
		extractor.setAttrs(logActivated, 
				brandModelList,
				unitsReplaceMap, 
				recipientsReplaceMap,
				recipientsList,
				unitsList,
				classModelList);

		// extract processed model fields
		pm = extractor.extract(pm);

		// if we are not running tests, update digital content only if in insights mode
		if (Main.executionParameters.getMode().equals(ExecutionParameters.MODE_INSIGHTS)) {
			this.updateDigitalContent(pm, session);
		}

		if (logActivated) Logging.printLogDebug(logger, "\n---> Final result:");

		if (logActivated) Logging.printLogDebug(logger, pm.toString());

		return pm;
	}

	/**
	 * Reevaluates all names and description rules, and also all the images.
	 * 
	 * @param pm
	 * @param session
	 */
	private void updateDigitalContent(ProcessedModel pm, CrawlerSession session) {  
		Logging.printLogDebug(logger, session, "Updating digital content...");

		// if the processed model doesn't have a digital content
		// we must create an empty one, to be populated
		if(pm.getDigitalContent() == null) { 
			pm.setDigitalContent(new JSONObject()); 
		}

		// get reference digital content
		JSONObject lettDigitalContent = fetchReferenceDigitalContent(pm.getLettId(), session);

		// analysing images
		// count pics
		// evaluate primary image
		// evaluate secondary images
		JSONObject pic = new JSONObject();
		try {
			pic = pm.getDigitalContent().getJSONObject("pic");
		} 
		catch (Exception e) { 
			Logging.printLogDebug(logger, session, CommonMethods.getStackTraceString(e));
		}

		// count images
		pic.put("count", DigitalContentAnalyser.imageCount(pm));

		// evaluate primary image
		JSONObject picPrimary = new JSONObject();
		if(pic.has("primary") ) {
			picPrimary = pic.getJSONObject("primary");
		}
		
		// assembling path to primary image stored on Amazon S3
		// this image is the last downloaded image in the image crawler
		StringBuilder primaryImageAmazonKey = new StringBuilder();
		primaryImageAmazonKey.append("product-image/");
		primaryImageAmazonKey.append(this.cityNameInfo.get(pm.getMarket()) + "/");
		primaryImageAmazonKey.append(this.marketNameInfo.get(pm.getMarket()) + "/");
		primaryImageAmazonKey.append(pm.getInternalId());
		primaryImageAmazonKey.append("/1-original.jpg");
		
		// assembling path to the desired primary image on Amazon S3
		// this image is the one that was previously stored as the image that goes to insights
		StringBuilder desiredPrimaryImageAmazonKey = new StringBuilder();
		desiredPrimaryImageAmazonKey.append("product-image/");
		desiredPrimaryImageAmazonKey.append("lett/");
		desiredPrimaryImageAmazonKey.append(pm.getLettId());
		desiredPrimaryImageAmazonKey.append("/1-original.jpg");
		
		// fetch md5 of the desired image
		String desiredPrimaryMd5 = S3Service.fetchMd5FromAmazon(session, desiredPrimaryImageAmazonKey.toString());
		
		// fetch md5 for the supposed new image
		String primaryMd5 = S3Service.fetchMd5FromAmazon(session, primaryImageAmazonKey.toString());

		String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss.SSS");

		// if md 5 is null, clean and add set as no_image
		if(primaryMd5 == null) {
			picPrimary = new JSONObject();
			picPrimary.put("status", Pic.NO_IMAGE);
			picPrimary.put("verified_by", "crawler_" + nowISO);

		} 
		else {
			if( !(picPrimary.has("md5") && picPrimary.get("md5").equals(primaryMd5)) ) { // if image has changed
				File primaryImage = S3Service.fetchImageFromAmazon(session, primaryImageAmazonKey.toString());

				// get dimensions from image
				picPrimary.put("dimensions", DigitalContentAnalyser.imageDimensions(primaryImage));

				// old similarity value
				// was calculate using the naive similarity finder
				// set to 0 because this algorithm was removed
				picPrimary.put("similarity", 0);

				// compute similarity of the new image using the SIFT algorithm
				JSONObject similaritySiftResult = null;
				try {
					similaritySiftResult = DigitalContentAnalyser.similaritySIFT(mongo, db, primaryMd5, pm.getLettId(), desiredPrimaryMd5);
				} catch (Exception e) {
					Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
				}				

				picPrimary.put("similarity_sift", similaritySiftResult);

				// setting fields of the new primary image
				picPrimary.put("md5", primaryMd5); // updated md5
				picPrimary.put("status", Pic.NOT_VERIFIED);
				picPrimary.put("verified_by", "crawler_" + nowISO);

				// delete local images
				if(primaryImage != null) {
					primaryImage.delete();
				}
			}
		}

		pic.put("primary", picPrimary);

		// computing pic secondary
		Pic.setPicSecondary(lettDigitalContent, pic);

		// set pic on digital content
		pm.getDigitalContent().put("pic", pic);

		// naming rules
		JSONArray nameRulesResults = RulesEvaluation.computeNameRulesResults(lettDigitalContent, pm.getOriginalName());
		pm.getDigitalContent().put("name_rules_results", nameRulesResults);

		// description rules
		JSONArray descriptionRulesResults = RulesEvaluation.computeDescriptionRulesResults(lettDigitalContent, pm.getOriginalDescription());
		pm.getDigitalContent().put("description_rules_results", descriptionRulesResults);

		// create rules summary
		JSONObject rules_results = RulesEvaluation.sumarizeRules(nameRulesResults, descriptionRulesResults);
		pm.getDigitalContent().put("rules_results", rules_results);
	}

	/**
	 * Fetch all markets from database.
	 */
	private void createMarketInfo() {
		this.cityNameInfo = new HashMap<Integer, String>();
		this.marketNameInfo = new HashMap<Integer, String>();
		this.marketid = new ArrayList<Integer>();

		try {

			ResultSet rs = this.db.runSqlConsult("SELECT * FROM market");

			while(rs.next()) {

				// Mapa de informaçẽos da cidade
				this.cityNameInfo.put(rs.getInt("id"), rs.getString("city"));

				// Mapa de informaçẽos do Supermercado
				this.marketNameInfo.put(rs.getInt("id"), rs.getString("name"));

				// Contém ids dos supermercados para teste do processer
				this.marketid.add(rs.getInt("id"));

			}
		} catch (SQLException e) {
			Logging.printLogError(logger, "Error fetching market info on postgres!");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}

	/**
	 * Fetch the digital content from lett table.
	 * 
	 * @param lettId
	 * @param session
	 * @return the json object containing the reference digital content or an empty json object
	 */
	private JSONObject fetchReferenceDigitalContent(Long lettId, CrawlerSession session) {
		try {
			ResultSet rs = this.db.runSqlConsult("SELECT digital_content FROM lett WHERE id = " + lettId);
			while(rs.next()) {
				return new JSONObject(rs.getString("digital_content"));
			}

		} catch (Exception e) { 
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
		}

		return new JSONObject();
	}


	public List<Integer> getMarketid() {
		return this.marketid;
	}

	public Map<Integer, String> getCityNameInfo() {
		return this.cityNameInfo;
	}

	public Map<Integer, String> getMarketNameInfo() {
		return this.marketNameInfo;
	}

}
