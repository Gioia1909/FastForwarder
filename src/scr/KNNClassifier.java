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

    //cerco i k punti del dataset con feature simili all'input
    //calcola la media dei valori di steering, accelerate e brake associati
    //retituisce il valore previsto 

     // Metodo per previsione continua: restituisce la media dei k vicini
     private double predictContinuous(double[] input, String target) {
        //creo una lista neighbors con tutti i punti del dataset
        List<DataPoint> neighbors = new ArrayList<>(dataset);
       
        //ordina i punti in base alla distanza euclidea dall'input
        //piu è piccola la distanza, più il punto è vicino
        neighbors.sort(Comparator.comparingDouble(p -> euclidean(p.features, input)));

        //somma dei valori del target dei primi k vicini
        double sum = 0.0;

        //prendo i primi k punti più vicini
        for (int i = 0; i < k; i++) {
            //in base al target scelgo il valore da sommare
            DataPoint dp = neighbors.get(i);
            double value = switch (target) {
                case "steering" -> dp.steering;
                case "accelerate" -> dp.accelerate;
                case "brake" -> dp.brake;
                default -> throw new IllegalArgumentException("Unknown target: " + target);
            };
            //aggiungo il valore alla somma
            sum += value;
        }
        //calcolo la media dei valori dei primi k vicini
        return sum / k;
    }

    // Calcola la distanza euclidea tra due vettori (features)
    // La distanza euclidea è la radice quadrata della somma dei quadrati delle differenze tra le coordinate corrispondenti
    private double euclidean(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) sum += Math.pow(a[i] - b[i], 2);
        return Math.sqrt(sum);
    }
}
