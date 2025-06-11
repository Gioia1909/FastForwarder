package scr;

import java.io.*;
import java.util.*;

//VERSIONE CON DISTANZA

public class DatasetLoader {
    // legge ogni riga del CSV, normalizza gli 8 input, crea DataPoint
    public static List<DataPoint> load(String path) throws IOException {
        List<DataPoint> dataset = new ArrayList<>();

        // Calcola min e max
        double[][] minMax = calcolaMinMax(path);
        salvaMinMaxSuFile(minMax);
        double[] min = minMax[0];
        double[] max = minMax[1];

        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        br.readLine(); // salta intestazione

        while ((line = br.readLine()) != null) {
            String[] tokens = line.trim().split(",");
            if (tokens.length < 16)
                continue;

            try {
                // NUOVA AGGIUNTA : DISTANZA
                double[] input = new double[12]; // 8 input + 1 distanza

                // double[] input = new double[8];

                // NUOVA AGGIUNTA : DISTANZA
                for (int i = 0; i < 12; i++) {

                    // for (int i = 0; i < 8; i++) {
                    double valore = Double.parseDouble(tokens[i]);
                    input[i] = (max[i] != min[i]) ? (valore - min[i]) / (max[i] - min[i]) : 0.0;
                }

                /*
                 * PRIMA VERSIONE SENZA DISTANZA
                 * double accel = Double.parseDouble(tokens[8]);
                 * double brake = Double.parseDouble(tokens[9]);
                 * double steer = Double.parseDouble(tokens[10]);
                 * int gear = Integer.parseInt(tokens[11]);
                 */

                // NUOVA AGGIUNTA : DISTANZA
                double accel = Double.parseDouble(tokens[12]);
                double brake = Double.parseDouble(tokens[13]);
                double steer = Double.parseDouble(tokens[14]);
                int gear = Integer.parseInt(tokens[15]);

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
        br.readLine(); // salta intestazione

        // NUOVA AGGIUNTA : DISTANZA
        double[] min = new double[12];
        double[] max = new double[12];

        /*
         * PRIMA VERSIONE SENZA DISTANZA
         * double[] min = new double[8];
         * double[] max = new double[8];
         */
        Arrays.fill(min, Double.POSITIVE_INFINITY);
        Arrays.fill(max, Double.NEGATIVE_INFINITY);

        while ((line = br.readLine()) != null) {
            String[] tokens = line.trim().split(",");

            // NUOVA AGGIUNTA : DISTANZA
            if (tokens.length < 16)
                // VERSIONE SENZA DISTANZA
                // if (tokens.length < 8)
                continue;

            try {
                // NUOVA AGGIUNTA : DISTANZA
                for (int i = 0; i < 12; i++) {

                    // VERSIONE SENZA DISTANZA
                    /* for (int i = 0; i < 8; i++) { */
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

    // 3. Salvataggio di min e max su file
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
        /** NUOVA AGGIUNTA : DISTANZA */
        double[] min = new double[12];
        double[] max = new double[12];

        /*
         * double[] min = new double[8];
         * double[] max = new double[8];
         */

        BufferedReader minIn = new BufferedReader(new FileReader(minPath));
        BufferedReader maxIn = new BufferedReader(new FileReader(maxPath));

        // NUOVA AGGIUNTA : DISTANZA
        for (int i = 0; i < 12; i++) {

            // VERSIONE SENZA DISTANZA
            // for (int i = 0; i < 8; i++) {
            min[i] = Double.parseDouble(minIn.readLine());
            max[i] = Double.parseDouble(maxIn.readLine());
        }
        minIn.close();
        maxIn.close();

        return new double[][] { min, max };
    }

}
