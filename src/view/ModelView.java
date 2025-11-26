package view;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view;
    private Map<String, Object> model;

    public ModelView() {
        this.model = new HashMap<>();
    }

    public ModelView(String view) {
        this.view = view;
        this.model = new HashMap<>();
    }

    public String getView() { return view; }
    public void setView(String view) { this.view = view; }

    public Map<String, Object> getModel() { return model; }
    public void setModel(Map<String, Object> model) { this.model = model; }

    public void addObject(String key, Object value) {
        this.model.put(key, value);
    }
}
