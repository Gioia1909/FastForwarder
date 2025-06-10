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
        double steer = predictContinuous(input, "steering");
        // If the predicted steering is very small but the angle to the track
        // axis is noticeable, recompute it using a simple heuristic similar to
        // SimpleDriver#getSteer
        if (Math.abs(steer) < 0.05 && Math.abs(input[6]) > 0.1) {
            double targetAngle = input[6] - input[5] * 0.5;
            steer = targetAngle / 0.785398;
            // clamp the value in [-1,1]
            steer = Math.max(-1.0, Math.min(1.0, steer));
        }
        return steer;
    }

    public double predictAccelerate(double[] input) {
        double accel = predictContinuous(input, "accelerate");
        // If the classifier predicts a very small acceleration but the track
        // ahead is clear (center sensor high) force a stronger acceleration
        if (accel < 0.2 && input[2] > 0.8)
            accel = 1.0;
        return accel;
    }

    public double predictBrake(double[] input) {
        double brake = predictContinuous(input, "brake");
        // If braking is predicted to be very low but the distance in front is
        // short, apply a moderate brake
        if (brake < 0.1 && input[2] < 0.3)
            brake = 0.5;
        return brake;
    }

    // Metodo per previsione discreta: restituisce la marcia più frequente tra i k
    // vicini
    public int predictGear(double[] input) {
        List<DataPoint> neighbors = new ArrayList<>(dataset);
        neighbors.sort(Comparator.comparingDouble(p -> euclidean(p.features, input)));

        if (input[7] < 0.017)
            return 1;
        // Mappa per contare la frequenza di ciascuna marcia tra i k vicini
        Map<Integer, Integer> gearCount = new HashMap<>();

        for (int i = 0; i < k; i++) {
            int g = neighbors.get(i).gear;
            gearCount.put(g, gearCount.getOrDefault(g, 0) + 1);
        }

        // Restituisci la marcia più frequente
        return gearCount.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .get().getKey();

    }

    // cerco i k punti del dataset con feature simili all'input
    // calcola la media dei valori di steering, accelerate e brake associati
    // retituisce il valore previsto

    // Metodo per previsione continua: restituisce la media dei k vicini
    private double predictContinuous(double[] input, String target) {
        // creo una lista neighbors con tutti i punti del dataset
        List<DataPoint> neighbors = new ArrayList<>(dataset);

        // ordina i punti in base alla distanza euclidea dall'input
        // piu è piccola la distanza, più il punto è vicino
        neighbors.sort(Comparator.comparingDouble(p -> euclidean(p.features, input)));

        // somma dei valori del target dei primi k vicini
        double sum = 0.0;

        // prendo i primi k punti più vicini
        for (int i = 0; i < k; i++) {
            // in base al target scelgo il valore da sommare
            DataPoint dp = neighbors.get(i);
            double value = switch (target) {
                case "steering" -> dp.steering;
                case "accelerate" -> dp.accelerate;
                case "brake" -> dp.brake;
                default -> throw new IllegalArgumentException("Unknown target: " + target);
            };
            // aggiungo il valore alla somma
            sum += value;
        }
        // calcolo la media dei valori dei primi k vicini
        return sum / k;
    }

    // Calcola la distanza euclidea tra due vettori (features)
    // La distanza euclidea è la radice quadrata della somma dei quadrati delle
    // differenze tra le coordinate corrispondenti
    private double euclidean(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++)
            sum += Math.pow(a[i] - b[i], 2);
        return Math.sqrt(sum);
    }
}
