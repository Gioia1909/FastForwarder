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

    public double predictSteering(double[] input) {
        return predictContinuous(input, "steering");
    }

    public double predictAccelerate(double[] input) {
        return predictContinuous(input, "accelerate");
    }

    public double predictBrake(double[] input) {
        return predictContinuous(input, "brake");
    }

     // Metodo per previsione continua: restituisce la media dei k vicini
     private double predictContinuous(double[] input, String target) {
        List<DataPoint> neighbors = new ArrayList<>(dataset);
        neighbors.sort(Comparator.comparingDouble(p -> euclidean(p.features, input)));

        double sum = 0.0;
        for (int i = 0; i < k; i++) {
            DataPoint dp = neighbors.get(i);
            double value = switch (target) {
                case "steering" -> dp.steering;
                case "accelerate" -> dp.accelerate;
                case "brake" -> dp.brake;
                default -> throw new IllegalArgumentException("Unknown target: " + target);
            };
            sum += value;
        }

        return sum / k;
    }

    private double euclidean(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) sum += Math.pow(a[i] - b[i], 2);
        return Math.sqrt(sum);
    }
}
