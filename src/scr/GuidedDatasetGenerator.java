package scr;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;

// Questa classe è un generatore di dataset guidati per la raccolta di dati
// per l'addestramento di modelli di intelligenza artificiale.
public class GuidedDatasetGenerator {
    public static void main(String[] args) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("guided_dataset.csv"))) {
            bw.write("TrackLeft,TrackCenter,TrackRight,TrackPosition,AngleToTrackAxis,Speed,Accelerate,Brake,Steering\n");

            for (int i = 0; i < 1000; i++) {
                double[] sensors = generateRandomSensors();
                DataPoint dp = generateGuided(sensors);
                double[] f = dp.features;

                bw.write(String.format(Locale.US, "%.2f,%.2f,%.2f,%.3f,%.3f,%.1f,%.2f,%.2f,%.2f\n",
                        f[0], f[1], f[2], f[3], f[4], f[5],
                        dp.accelerate, dp.brake, dp.steering));
            }
        }

        System.out.println("Dataset guidato generato!");
    }


    public static double[] generateRandomSensors() {
    Random r = new Random();
    double trackLeft = 20 + r.nextDouble() * 80;
    double trackCenter = 20 + r.nextDouble() * 80;
    double trackRight = 20 + r.nextDouble() * 80;
    double trackPos = -1.2 + r.nextDouble() * 2.4;
    double angle = -0.4 + r.nextDouble() * 0.8;
    double speed = 10 + r.nextDouble() * 70;
    return new double[] {trackLeft, trackCenter, trackRight, trackPos, angle, speed};
}


    // Genera un DataPoint guidato basato sui sensori dell'auto
    // I sensori sono rappresentati da un array di 6 valori:
    // [trackLeft, trackCenter, trackRight, trackPos, angle, speed]
    // Restituisce un DataPoint con le azioni di sterzata, accelerazione e frenata calcolate.
    // Le azioni sono calcolate in base alla posizione dell'auto sulla pista e all'angolo di sterzata.
    // Le azioni sono normalizzate tra -1 e 1 per la sterzata, e tra 0 e 1 per accelerazione e frenata.
    
    // I casi gestiti sono: 
    // 1. Rettilineo: accelera e sterza leggermente verso il centro.
    // 2. Curva a destra: sterza a destra, accelera meno.
    // 3. Curva a sinistra: sterza a sinistra, accelera meno.
    // 4. Leggera correzione in uscita da curva: sterza in base alla posizione e all'angolo.

    public static DataPoint generateGuided(double[] sensors) {
        // questi sono i sensori dell'auto

        double trackLeft = sensors[0];
        double trackCenter = sensors[1];
        double trackRight = sensors[2];
        double trackPos = sensors[3];
        double angle = sensors[4];
        double speed = sensors[5];
    
        // Target da generare
        double accel = 0.0;
        double brake = 0.0;
        double steering = 0.0;
    
        // Caso 1: RETTILINEO – vai forte e dritto
        if (Math.abs(angle) < 0.05 && trackCenter > 70) {
            accel = 1.0;
            brake = 0.0;
            steering = -trackPos * 0.4; // leggera centratura
        }
    
        // Caso 2: CURVA A DESTRA – sterza a destra, accelera meno
        else if (angle > 0.2) {
            steering = 0.5 + angle; // curva destra
            accel = 0.5;
            brake = 0.2;
        }
    
        // Caso 3: CURVA A SINISTRA – sterza a sinistra, accelera meno
        else if (angle < -0.2) {
            steering = -0.5 + angle; // curva sinistra
            accel = 0.5;
            brake = 0.2;
        }
    
        // Caso 4: leggera correzione in uscita da curva
        else {
            steering = -trackPos * 0.6 + angle;
            accel = 0.8;
            brake = 0.0;
        }
    
        // Clamp valori
        // Assicura che i valori di sterzata, accelerazione e frenata siano nei limiti
        // -1.0 <= steering <= 1.0
        // 0.0 <= accel <= 1.0
        // 0.0 <= brake <= 1.0
        // Questo è importante per evitare comportamenti imprevisti nell'auto simulata.
        // Ad esempio, se la sterzata è troppo alta, l'auto potrebbe ribaltarsi.
        // Se l'accelerazione è negativa, l'auto potrebbe frenare quando non dovrebbe.
        // Se la frenata è negativa, l'auto potrebbe accelerare quando dovrebbe frenare.
        // Quindi, clamping aiuta a mantenere l'auto in un comportamento controllato e prevedibile.

        steering = Math.max(-1.0, Math.min(1.0, steering));
        accel = Math.max(0.0, Math.min(1.0, accel));
        brake = Math.max(0.0, Math.min(1.0, brake));
    
        // Crea un DataPoint con le features e le azioni calcolate
        double[] features = new double[] {trackLeft, trackCenter, trackRight, trackPos, angle, speed};
        return new DataPoint(features, steering, accel, brake);
    }
    
}
