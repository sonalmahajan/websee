package prioritization;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import websee.HtmlElement;

public class Predict {
	private Model model;

	public Predict(String modelFile) {
		loadModel(modelFile);
	}

	private void loadModel(String modelFile) {
		this.model = new Model();
		try {
			BufferedReader br = new BufferedReader(new FileReader(modelFile));
			String line;
			while ((line = br.readLine()) != null) {
				String split[] = line.split("\t");
				VisualProperty vp = new VisualProperty(split[0], split[1]);
				List<Integer> list = new ArrayList<Integer>();
				for (int i = 2; i < split.length; i++) {
					list.add(Integer.parseInt(split[i]));
				}
				Feature f = new Feature(list);
				this.model.add(vp, f);
			}
			br.close();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public TreeMap<VisualProperty, Double> predict(String oracleImage,
			String testPage, String elementXpath) {
		// Feature feature = new Feature(oracleImage, testPage, elementXpath);
		Extract e = new Extract("", "", "");
		Feature f = e.extract();
		System.out.println(f);
		Map<VisualProperty, Double> score = rankBayes(f);
		ValueComparator bvc = new ValueComparator(score);
		TreeMap<VisualProperty, Double> sorted_score = new TreeMap<VisualProperty, Double>(
				bvc);
		sorted_score.putAll(score);
		return sorted_score;
	}

	public TreeMap<VisualProperty, Double> predict(Feature f) {
		Map<VisualProperty, Double> score = rankBayes(f);
		ValueComparator bvc = new ValueComparator(score);
		TreeMap<VisualProperty, Double> sorted_score = new TreeMap<VisualProperty, Double>(
				bvc);
		sorted_score.putAll(score);
		return sorted_score;
	}

	public List<String> scoreToRank(TreeMap<VisualProperty, Double> score) {
		List<String> list = new ArrayList<String>();
		for (VisualProperty vp : score.keySet())
			list.add(vp.toString());
		return list;
	}

	public List<VisualProperty> scoreToRank(
			TreeMap<VisualProperty, Double> score, HtmlElement element) {
		List<VisualProperty> list = new ArrayList<VisualProperty>();
		for (VisualProperty vp : score.keySet()) {
			if (vp.getType().equals("css")
					&& element.getCssProperties().keySet()
							.contains(vp.getName())
					|| (vp.getType().equals("html") && element
							.getHtmlAttributes().keySet()
							.contains(vp.getName()))) {
				list.add(vp);
			}
		}
		return list;
	}

	public Map<VisualProperty, Double> rankBayes(Feature f) {
		Map<VisualProperty, Integer> pMap = new HashMap<VisualProperty, Integer>();
		Map<VisualProperty, Integer> qMap = new HashMap<VisualProperty, Integer>();

		for (VisualProperty vp : this.model.getVisualPropertySet()) {
			int p = 0;
			int q = 0;
			for (int i = 0; i < this.model.getLength(); i++) {
				p += this.model.getFrequency(vp, i);
				q += this.model.getTotal(vp) - this.model.getFrequency(vp, i);
			}
			pMap.put(vp, p);
			qMap.put(vp, q);
		}

		Map<VisualProperty, Double> scoreMap = new HashMap<VisualProperty, Double>();
		for (VisualProperty vp : this.model.getVisualPropertySet()) {
			Double score = this.model.getTotal(vp).doubleValue();
			for (int i = 0; i < this.model.getLength(); i++) {
				score *= f.getList().get(i)
						* this.model.getFrequency(vp, i).doubleValue()
						/ pMap.get(vp).doubleValue()
						+ (1 - f.getList().get(i))
						* (this.model.getTotal(vp).doubleValue() - this.model
								.getFrequency(vp, i).doubleValue())
						/ qMap.get(vp).doubleValue();
			}
			scoreMap.put(vp, score);
		}
		return scoreMap;
	}

	public Model getModel() {
		return this.model;
	}
}

class ValueComparator implements Comparator<VisualProperty> {

	Map<VisualProperty, Double> base;

	public ValueComparator(Map<VisualProperty, Double> base) {
		this.base = base;
	}

	// Note: this comparator imposes orderings that are inconsistent with
	// equals.
	public int compare(VisualProperty a, VisualProperty b) {
		if (base.get(a) >= base.get(b)) {
			return -1;
		} else {
			return 1;
		} // returning 0 would merge keys
	}
}