package prioritization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Model {
	private int length;
	private Map<VisualProperty, List<Integer>> frequence;
	private Map<VisualProperty, Integer> total;

	public Model() {
		this.frequence = new HashMap<VisualProperty, List<Integer>>();
		this.total = new HashMap<VisualProperty, Integer>();
		this.length = 0;
	}

	public void add(VisualProperty vp, Feature f) {
		if (this.length == 0) {
			this.length = f.getLength();
		}
		if (this.length != f.getLength()) {
			return;
		}
		addTotal(vp);
		addFeature(vp, f);
	}

	private void addTotal(VisualProperty vp) {
		if (this.total.containsKey(vp)) {
			Integer count = this.total.get(vp);
			count++;
			this.total.put(vp, count);
		} else {
			this.total.put(vp, 1);
		}
	}

	private void addFeature(VisualProperty vp, Feature f) {
		if (this.frequence.containsKey(vp)) {
			List<Integer> list = this.frequence.get(vp);
			for (int i = 0; i < list.size(); i++) {
				list.set(i, list.get(i) + f.getList().get(i));
			}
			this.frequence.put(vp, list);
		} else {
			this.frequence.put(vp, f.getList());
		}
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("Total:\n");
		for (Map.Entry<VisualProperty, Integer> entry : this.total.entrySet()) {
			VisualProperty key = entry.getKey();
			Integer value = entry.getValue();
			s.append(String.format("<%s>: %s\n", key, value));
		}
		s.append("Frequency:\n");
		for (Map.Entry<VisualProperty, List<Integer>> entry : this.frequence
				.entrySet()) {
			VisualProperty key = entry.getKey();
			List<Integer> value = entry.getValue();
			s.append(String.format("<%s>: %s\n", key, value));
		}
		return s.toString();
	}

	public Integer getLength() {
		return this.length;
	}

	public Integer getVisualPropertyNumber() {
		if (this.total == null)
			return 0;
		else
			return this.total.size();
	}

	public Set<VisualProperty> getVisualPropertySet() {
		if (this.total == null)
			return null;
		else
			return this.total.keySet();
	}

	public Integer getFrequency(VisualProperty vp, int index) {
		List<Integer> list = this.frequence.get(vp);
		return list.get(index);
	}

	public Integer getTotal(VisualProperty vp) {
		return this.total.get(vp);
	}

	public static void main(String args[]) {
		Model model = new Model();
		Feature f = new Feature("[1,0,1,0]");
		VisualProperty vp = new VisualProperty("css", "color");
		model.add(vp, f);
		model.add(new VisualProperty("css", "color"), new Feature("[0,1,1,1]"));
		model.add(new VisualProperty("css", "background-color"), new Feature(
				"[1,0,1,1]"));
		System.out.println(model);
	}
}