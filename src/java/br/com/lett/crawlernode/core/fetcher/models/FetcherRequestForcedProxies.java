package br.com.lett.crawlernode.core.fetcher.models;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class FetcherRequestForcedProxies {

  private LettProxy specific;
  private List<String> any = new ArrayList<>();

  public JSONObject toJson() {
    JSONObject forcedProxies = new JSONObject();

    if (specific != null) {
      forcedProxies.put("specific", specific.toJson());
    } else if (any != null && !any.isEmpty()) {
      JSONArray anyArray = new JSONArray();

      for (String proxy : any) {
        anyArray.put(proxy);
      }

      forcedProxies.put("any", anyArray);
    }

    return forcedProxies;
  }

  public boolean isEmpty() {
    return this.specific == null && (this.any == null || this.any.isEmpty());
  }

  public LettProxy getSpecific() {
    return specific;
  }

  public FetcherRequestForcedProxies setSpecific(LettProxy specific) {
    this.specific = specific;
    return this;
  }

  public List<String> getAny() {
    return any;
  }

  public FetcherRequestForcedProxies setAny(List<String> any) {
    this.any = any;
    return this;
  }
}
