package prioritization;

import java.util.ArrayList;
import java.util.List;

public class Feature {
	private List<Integer> feature;

	public Feature(String feature_string) {
		if (feature_string != null) {
			this.feature = new ArrayList<Integer>();
			String split[] = (feature_string.substring(1,
					feature_string.length() - 1)).split(",");
			for (String s : split) {
				this.feature.add(Integer.parseInt(s.trim()));
			}
		}
	}

	public Feature(List<Integer> list) {
		if (list != null) {
			this.feature = list;
		}
	}

	public String toString() {
		return this.feature.toString();
	}

	public String tabString() {
		if (this.feature.size() == 0) {
			return null;
		}
		StringBuilder out = new StringBuilder();
		out.append(feature.get(0));
		for (int i = 1; i < this.feature.size(); i++) {
			out.append("\t").append(feature.get(i));
		}
		return out.toString();
	}

	public List<Integer> getList() {
		return this.feature;
	}

	public Integer getLength() {
		if (this.feature == null)
			return 0;
		else
			return this.feature.size();
	}
}
