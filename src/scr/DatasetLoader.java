package scr;
//leggo il csv e creo una lista tipo dataPoint


import java.io.*;
import java.util.*;

public class DatasetLoader {
    //legge ogni riga del CSV 
    //trasformo in un DataPoint
    //Restituisco Lista Data Point
    public static List<DataPoint> load(String path) throws IOException {
        List<DataPoint> dataset = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        br.readLine(); // salta intestazione

        while ((line = br.readLine()) != null) {
            String[] tokens = line.split(",");
            double[] input = new double[6];
            for (int i = 0; i < 6; i++) {
                input[i] = Double.parseDouble(tokens[i]);
            }

            int accel = (int) Math.round(Double.parseDouble(tokens[6]));
            int brake = (int) Math.round(Double.parseDouble(tokens[7]));
            int steer = (int) Math.round(Double.parseDouble(tokens[8]));

            dataset.add(new DataPoint(input, steer, accel, brake));
        }

        return dataset;
    }
}
