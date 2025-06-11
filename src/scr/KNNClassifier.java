package scr;

import java.util.*;

public class KNNClassifier {

    private final KDTree tree;
    private final int k;

    public KNNClassifier(List<DataPoint> dataset, int k) {
        this.tree = new KDTree(dataset);
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

    public int predictGear(double[] input, double rpm, int currentGear, int[] gearUp, int[] gearDown) {
        List<DataPoint> neighbors = tree.findKNearest(input, k);

        Map<Integer, Integer> gearVotes = new HashMap<>();
        for (DataPoint dp : neighbors) {
            gearVotes.put(dp.gear, gearVotes.getOrDefault(dp.gear, 0) + 1);
        }

        // Marcia più votata
        int predicted = gearVotes.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .get().getKey();

        // Controllo di sicurezza per evitare -1 e 0
        if (predicted < 1)
            predicted = 1;

        // Logica di regolazione fine (simile a getGear)
        if (currentGear < 1) {
            return 1;
        }

        if (currentGear < 6 && rpm >= gearUp[currentGear - 1]) {
            return currentGear + 1;
        } else if (currentGear > 1 && rpm <= gearDown[currentGear - 1]) {
            return currentGear - 1;
        }

        return predicted;
    }

    private double predictContinuous(double[] input, String target) {
        List<DataPoint> neighbors = tree.findKNearest(input, k);
        double sum = 0.0;
        for (DataPoint dp : neighbors) {
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
}
