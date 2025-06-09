package scr;

import java.util.List;

public class TrainingMain {
    public static void main(String[] args) {
        try {
            List<DataPoint> dataset = DatasetLoader.load("dataset.csv");
            KNNClassifier classifier = new KNNClassifier(dataset, 1);

            valutaClassifier(classifier, dataset);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void valutaClassifier(KNNClassifier classifier, List<DataPoint> testSet) {
        double erroreSteering = 0;
        double erroreAccelerate = 0;
        double erroreBrake = 0;
        int erroriGear = 0;

        for (DataPoint dp : testSet) {
            double[] input = dp.getFeatures(); // già normalizzato
            double predSteer = classifier.predictSteering(input);
            double predAccel = classifier.predictAccelerate(input);
            double predBrake = classifier.predictBrake(input);
            int predGear = classifier.predictGear(input);

            erroreSteering += Math.abs(dp.getSteering() - predSteer);
            erroreAccelerate += Math.abs(dp.getAccelerate() - predAccel);
            erroreBrake += Math.abs(dp.getBrake() - predBrake);
            if (predGear != dp.getGear())
                erroriGear++;
        }

        int n = testSet.size();
        System.out.printf("Errore medio sterzo: %.4f\n", erroreSteering / n);
        System.out.printf("Errore medio accelerazione: %.4f\n", erroreAccelerate / n);
        System.out.printf("Errore medio frenata: %.4f\n", erroreBrake / n);
        System.out.printf("Accuratezza marcia: %.2f%%\n", 100.0 * (n - erroriGear) / n);
    }
}
