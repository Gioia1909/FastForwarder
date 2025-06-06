package scr;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;

public class ManualDriver extends Controller {

    private volatile boolean accel = false, brake = false, left = false, right = false;
    private float clutch = 0;
    private int gear = 1;

    final int[] gearUp = {5000, 6000, 6000, 6500, 7000, 0};
    final int[] gearDown = {0, 2500, 3000, 3000, 3500, 3500};

    public ManualDriver() {
        // Finestra invisibile con focus permanente per leggere i tasti
        JFrame frame = new JFrame("Manual Driver");
        frame.setSize(200, 100);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setOpacity(0f);
        frame.setFocusable(true);
        frame.setVisible(true);
        frame.requestFocus();

        frame.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> accel = true;
                    case KeyEvent.VK_S -> brake = true;
                    case KeyEvent.VK_A -> left = true;
                    case KeyEvent.VK_D -> right = true;
                    case KeyEvent.VK_UP -> gear++;  // Cambio marcia manuale su
                    case KeyEvent.VK_DOWN -> gear--; // Cambio marcia manuale giù
                }
            }

            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> accel = false;
                    case KeyEvent.VK_S -> brake = false;
                    case KeyEvent.VK_A -> left = false;
                    case KeyEvent.VK_D -> right = false;
                }
            }
        });
    }

    @Override
    public Action control(SensorModel sensors) {
        Action action = new Action();

        action.accelerate = accel ? 1.0 : 0.0;
        action.brake = brake ? 0.5 : 0.0;
        action.steering = right ? -0.2f : (left ? 0.2f : 0.0f);

        // Gestione marce
        if (gear < -1) gear = -1;
        if (gear > 6) gear = 6;
        action.gear = gear;

        // Frizione dinamica
        action.clutch = clutching(sensors, clutch);

        // Salvataggio dati
        try {
            File file = new File("dataset.csv");
            boolean fileExists = file.exists();
            boolean fileIsEmpty = file.length() == 0;
        
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
        
            if (!fileExists || fileIsEmpty) {
                // Scrivi intestazione
                bw.write("TrackPosition,AngleToTrackAxis,Speed,Accelerate,Brake,Steering,Gear\n");
            }
        
            // Scrivi i dati
            bw.write(
                sensors.getTrackPosition() + "," +
                sensors.getAngleToTrackAxis() + "," +
                sensors.getSpeed() + "," +
                action.accelerate + "," +
                action.brake + "," +
                action.steering + "," +
                action.gear + "\n"
            );
        
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        } 

        return action;
    }

    private float clutching(SensorModel sensors, float clutch) {
        // Stesso codice della tua funzione: dinamico nei primi secondi
        final float clutchMax = 0.5f;
        final float clutchDelta = 0.05f;
        final float clutchMaxTime = 1.5f;
        final float clutchDec = 0.01f;
        final float clutchDeltaTime = 0.02f;
        final float clutchDeltaRaced = 10f;
        final float clutchMaxModifier = 1.3f;

        float maxClutch = clutchMax;

        if (sensors.getCurrentLapTime() < clutchDeltaTime && getStage() == Stage.RACE
                && sensors.getDistanceRaced() < clutchDeltaRaced)
            clutch = maxClutch;

        if (clutch > 0) {
            double delta = clutchDelta;
            if (sensors.getGear() < 2) {
                delta /= 2;
                maxClutch *= clutchMaxModifier;
                if (sensors.getCurrentLapTime() < clutchMaxTime)
                    clutch = maxClutch;
            }

            clutch = Math.min(maxClutch, clutch);

            if (clutch != maxClutch) {
                clutch -= delta;
                clutch = Math.max(0.0f, clutch);
            } else {
                clutch -= clutchDec;
            }
        }

        return clutch;
    }

    @Override
    public void reset() {
        System.out.println("Reset!");
    }

    @Override
    public void shutdown() {
        System.out.println("Shutdown!");
    }

    @Override
    public float[] initAngles() {
        return super.initAngles(); // oppure puoi personalizzarli
    }
}
