package scr;

import java.io.*;
import java.util.*;

public class DatasetLoader {
    // Indici delle colonne nel CSV
    public static final int IDX_DISTANCE = 0;
    public static final int IDX_TRACK3 = 1;
    public static final int IDX_TRACK4 = 2;
    public static final int IDX_TRACK5 = 3;
    public static final int IDX_TRACK6 = 4;
    public static final int IDX_TRACK7 = 5;
    public static final int IDX_TRACK8 = 6;
    public static final int IDX_TRACK9 = 7;
    public static final int IDX_TRACK10 = 8;
    public static final int IDX_TRACK11 = 9;
    public static final int IDX_TRACK12 = 10;
    public static final int IDX_TRACK13 = 11;
    public static final int IDX_TRACK14 = 12;
    public static final int IDX_TRACK15 = 13;
    public static final int IDX_TRACK16 = 14;

    public static final int IDX_FOCUS1 = 15;
    public static final int IDX_FOCUS2 = 16;
    public static final int IDX_FOCUS3 = 17;

    public static final int IDX_TRACK_POS = 18;
    public static final int IDX_ANGLE = 19;
    public static final int IDX_SPEED = 20;
    public static final int IDX_SPEEDY = 21;
    public static final int IDX_DAMAGE = 22;
    public static final int IDX_DISTANCE_RACED = 23;
    public static final int IDX_RPM = 24;

    public static final int IDX_ACCEL = 25;
    public static final int IDX_BRAKE = 26;
    public static final int IDX_STEER = 27;
    public static final int IDX_GEAR = 28;

    // Seleziona qui le feature da usare come input
    public static final int[] FEATURE_INDICES = {
            IDX_DISTANCE,
            IDX_TRACK5, IDX_TRACK7,
            IDX_TRACK9, IDX_TRACK11, IDX_TRACK13,
            IDX_TRACK_POS, IDX_ANGLE, IDX_SPEED, IDX_SPEEDY, IDX_RPM

    };

    // , IDX_RPM,IDX_DAMAGE, IDX_TRACK4, IDX_TRACK6, IDX_TRACK8,IDX_TRACK10,
    // IDX_TRACK12, IDX_TRACK14,
    // IDX_TRACK16, IDX_FOCUS1, IDX_FOCUS2, IDX_FOCUS3, IDX_DISTANCE, IDX_TRACK3,
    // IDX_TRACK15, IDX_DISTANCE_RACED
    public static List<DataPoint> load(String path) throws IOException {
        List<DataPoint> dataset = new ArrayList<>();

        double[][] minMax = calcolaMinMax(path);
        salvaMinMaxSuFile(minMax);
        double[] min = minMax[0];
        double[] max = minMax[1];

        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        br.readLine(); // salta intestazione

        while ((line = br.readLine()) != null) {
            String[] tokens = line.trim().split(",");
            if (tokens.length <= IDX_GEAR)
                continue;

            try {
                double[] input = new double[FEATURE_INDICES.length];
                for (int i = 0; i < FEATURE_INDICES.length; i++) {
                    int idx = FEATURE_INDICES[i];
                    double valore = Double.parseDouble(tokens[idx]);
                    input[i] = (max[idx] != min[idx]) ? (valore - min[idx]) / (max[idx] - min[idx]) : 0.0;
                }

                double accel = Double.parseDouble(tokens[IDX_ACCEL]);
                double brake = Double.parseDouble(tokens[IDX_BRAKE]);
                double steer = Double.parseDouble(tokens[IDX_STEER]);
                int gear = Integer.parseInt(tokens[IDX_GEAR]);

                dataset.add(new DataPoint(input, steer, accel, brake, gear));
            } catch (NumberFormatException e) {
                System.out.println("Riga ignorata per errore di parsing: " + line);
            }
        }

        br.close();
        return dataset;
    }

    public static double[][] calcolaMinMax(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        br.readLine();

        int numCols = IDX_GEAR + 1;
        double[] min = new double[numCols];
        double[] max = new double[numCols];
        Arrays.fill(min, Double.POSITIVE_INFINITY);
        Arrays.fill(max, Double.NEGATIVE_INFINITY);

        while ((line = br.readLine()) != null) {
            String[] tokens = line.trim().split(",");
            if (tokens.length < numCols)
                continue;

            try {
                for (int i = 0; i < numCols; i++) {
                    double val = Double.parseDouble(tokens[i]);
                    if (val < min[i])
                        min[i] = val;
                    if (val > max[i])
                        max[i] = val;
                }
            } catch (NumberFormatException e) {
                System.out.println("Riga ignorata (errore parsing in calcolo min/max): " + line);
            }
        }

        br.close();
        return new double[][] { min, max };
    }

    public static void salvaMinMaxSuFile(double[][] minMax) throws IOException {
        try (
                BufferedWriter minWriter = new BufferedWriter(new FileWriter("min.txt"));
                BufferedWriter maxWriter = new BufferedWriter(new FileWriter("max.txt"))) {
            for (int i = 0; i < minMax[0].length; i++) {
                minWriter.write(minMax[0][i] + "\n");
                maxWriter.write(minMax[1][i] + "\n");
            }
        }
    }

    public static double[][] loadMinMaxDaFile(String minPath, String maxPath) throws IOException {
        int numCols = IDX_GEAR + 1;
        double[] min = new double[numCols];
        double[] max = new double[numCols];

        BufferedReader minIn = new BufferedReader(new FileReader(minPath));
        BufferedReader maxIn = new BufferedReader(new FileReader(maxPath));

        for (int i = 0; i < numCols; i++) {
            min[i] = Double.parseDouble(minIn.readLine());
            max[i] = Double.parseDouble(maxIn.readLine());
        }

        minIn.close();
        maxIn.close();

        return new double[][] { min, max };
    }
}