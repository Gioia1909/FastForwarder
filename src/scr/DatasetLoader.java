package scr;
//leggo il csv e creo una lista tipo dataPoint

import java.io.*;
import java.util.*;

public class DatasetLoader {
    // legge ogni riga del CSV
    // trasformo in un DataPoint
    // Restituisco Lista Data Point
    public static List<DataPoint> load(String path) throws IOException {
        List<DataPoint> dataset = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        br.readLine(); // salta intestazione

        while ((line = br.readLine()) != null) {
            String[] tokens = line.split(",");
             if (tokens.length < 12) continue;
             double[] input = new double[8];
            for (int i = 0; i < 8; i++) {
                input[i] = Double.parseDouble(tokens[i]);
            }

            // azioni 
            double accel = Double.parseDouble(tokens[8]);
            double brake = Double.parseDouble(tokens[9]);
            double steer = Double.parseDouble(tokens[10]);
            int gear = Integer.parseInt(tokens[11]);

            dataset.add(new DataPoint(input, steer, accel, brake, gear));
        }

                br.close();
        return dataset;
    }
}
