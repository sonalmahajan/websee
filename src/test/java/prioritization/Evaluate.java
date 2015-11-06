package prioritization;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class Evaluate {
	public static void main(String args[]) throws NumberFormatException,
			IOException {
		String modelfile = "scripts/input/gen/subjects/mix.odd";
		String testfile = "scripts/input/gen/subjects/mix.even";
		List<Integer> rank = new ArrayList<Integer>();
		Predict p = new Predict(modelfile);
		BufferedReader br = new BufferedReader(new FileReader(testfile));
		String line;
		while ((line = br.readLine()) != null) {
			String split[] = line.split("\t");
			VisualProperty vp = new VisualProperty(split[0], split[1]);
			List<Integer> list = new ArrayList<Integer>();
			for (int i = 2; i < split.length; i++) {
				list.add(Integer.parseInt(split[i]));
			}
			Feature f = new Feature(list);
			TreeMap<VisualProperty, Double> score = p.predict(f);
			int pos = 0;
			for (VisualProperty key : score.keySet()) {
				pos++;
				if (key.equals(vp)) {
					rank.add(pos);
					break;
				}
			}
			System.out.println(vp + " " + pos);
		}
		br.close();
		System.out.println(calculateAverage(rank) + " "
				+ p.getModel().getVisualPropertyNumber());
	}

	public static double calculateAverage(List<Integer> marks) {
		long sum = 0;
		if (!marks.isEmpty()) {
			for (Integer mark : marks) {
				sum += mark;
			}
			return (double) (sum) / marks.size();
		}
		return sum;
	}
}
