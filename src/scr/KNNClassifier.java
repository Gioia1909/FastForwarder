package scr;

import java.util.*;

//un classificatore che dato un input cerca gli esempi più simili nel dataset 
//conttiene una lista di dataset
//un valore k (quanti vicini guardare)
//predict per ogni azione 
//serve a fare la previsione 

/* 
Riceve un input
Calcola la distanza euclidea da ogni punto del dataset
Ordina i punti in base alla distanza
Prende i k più vicini
Vota la classe più frequente tra i vicini

*/
public class KNNClassifier {

    private final List<DataPoint> dataset;
    private final int k;

    public KNNClassifier(List<DataPoint> dataset, int k) {
        this.dataset = dataset;
        this.k = k;
    }

    public int predictSteering(double[] input) {
        return predict(input, "steering");
    }

    public int predictAccelerate(double[] input) {
        return predict(input, "accelerate");
    }

    public int predictBrake(double[] input) {
        return predict(input, "brake");
    }

    private int predict(double[] input, String target) {
        List<DataPoint> neighbors = new ArrayList<>(dataset);
        neighbors.sort(Comparator.comparingDouble(p -> euclidean(p.features, input)));

        Map<Integer, Integer> counts = new HashMap<>();
        for (int i = 0; i < k; i++) {
            int label = switch (target) {
                case "steering" -> neighbors.get(i).steering;
                case "accelerate" -> neighbors.get(i).accelerate;
                case "brake" -> neighbors.get(i).brake;
                default -> throw new IllegalArgumentException("Unknown target: " + target);
            };
            counts.put(label, counts.getOrDefault(label, 0) + 1);
        }

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .get()
                .getKey();
    }

    private double euclidean(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) sum += Math.pow(a[i] - b[i], 2);
        return Math.sqrt(sum);
    }
}
