package scr;

import java.util.List;
import java.io.IOException;

public class TrainingMain {
    public static void main(String[] args) throws IOException {
        // 1. Carico il dataset da CSV
        List<DataPoint> dataset = DatasetLoader.load("dataset.csv");

        // 2. Calcolo min e max
        double[][] minMax = DatasetLoader.calcolaMinMax("dataset.csv");

        // 3. Salvo min e max su file
        DatasetLoader.salvaMinMaxSuFile(minMax);

        // 4. Costruisco il classificatoreAdd commentMore actions
        KNNClassifier classifier = new KNNClassifier(dataset, 5);

        // 5. Faccio qualche test a video
        double[] inputTest = dataset.get(0).features;
        System.out.println("Predicted gear: " + classifier.predictGear(inputTest));
        System.out.println("Predicted steering: " + classifier.predictSteering(inputTest));
    }
}