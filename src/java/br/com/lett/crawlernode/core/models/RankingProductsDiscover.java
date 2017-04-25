package br.com.lett.crawlernode.core.models;

import org.bson.Document;

public class RankingProductsDiscover extends RankingProducts {

	private String taskId;
	private String type;

	public static final String TYPE_NEW = "new-product";
	public static final String TYPE_OLD = "processed-product";
	
	public Document getDocument(){
		
		Document doc = new Document()
			.append("position", this.position)
			.append("url", this.url)
			.append("type", this.type);
			
		if(this.type.equals(TYPE_NEW)) {
			doc.append("task_id", this.taskId);
		} else {
			doc.append("processed_ids", this.processedIds);
		}
		
		return doc;
	}
	
	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
