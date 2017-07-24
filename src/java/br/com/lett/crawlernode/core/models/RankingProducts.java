package br.com.lett.crawlernode.core.models;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

public class RankingProducts {

	protected String internalPid;
	protected int position;
	protected String url;
	protected List<Long> processedIds = new ArrayList<>();
	protected String screenshot;
	
	
	public Document getDocument(){
		
		return new Document()
			.append("position", this.position)
			//.append("url", this.url)
			.append("processed_ids", this.processedIds);
		
	}
	
	public String getInteranlPid() {
		return internalPid;
	}
	
	public void setInteranlPid(String interanlPid) {
		this.internalPid = interanlPid;
	}
	
	public int getPosition() {
		return position;
	}
	
	public void setPosition(int position) {
		this.position = position;
	}
	
	public List<Long> getProcessedIds() {
		return processedIds;
	}
	
	public void addProcessedId(String x){
		
	}
	
	public void setProcessedIds(List<Long> processedIds) {
		this.processedIds = processedIds;
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}
	
}
