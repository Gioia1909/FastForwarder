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

    public int predictGear(double[] input) {
        List<DataPoint> neighbors = tree.findKNearest(input, k);
        Map<Integer, Integer> gearCount = new HashMap<>();
        for (DataPoint dp : neighbors) {
            gearCount.put(dp.gear, gearCount.getOrDefault(dp.gear, 0) + 1);
        }
        return gearCount.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .get().getKey();
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
