package br.com.lett.crawlernode.database;

public class ProcessedModelPersistenceResult extends PersistenceResult {
	
	public ProcessedModelPersistenceResult() {
		super();
	}
	
	public void addCreatedId(Long id) {
		this.information.put("created_id", id);
	}
	
	public Long getCreatedId() {
		if (this.information.has("created_id") && this.information.get("created_id") != null) {
			return this.information.getLong("created_id");
		}
		return null;
	}
	
	public void addModifiedId(Long id) {
		this.information.put("modified_id", id);
	}
	
	public Long getModifiedId() {
		if (this.information.has("modified_id") && this.information.get("modified_id") != null) {
			return this.information.getLong("modified_id");
		}
		return null;
	}
	
}
