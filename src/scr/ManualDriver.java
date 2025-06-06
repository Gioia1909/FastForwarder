package scr;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;

public class ManualDriver extends Controller {

    private boolean accel = false, brake = false, left = false, right = false;
    private float clutch = 0;

    // Costanti cambio automatico
    final int[] gearUp = {5000, 6000, 6000, 6500, 7000, 0};
    final int[] gearDown = {0, 2500, 3000, 3000, 3500, 3500};

    public ManualDriver() {
        JFrame frame = new JFrame("Manual Driver Control");
        frame.setSize(200, 100);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setOpacity(0f); // finestra invisibile
        frame.setVisible(true);
        frame.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> accel = true;
                    case KeyEvent.VK_S -> brake = true;
                    case KeyEvent.VK_A -> left = true;
                    case KeyEvent.VK_D -> right = true;
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
        action.accelerate = accel ? 1 : 0;
        action.brake = brake ? 1 : 0;
        action.steering = left ? -1 : (right ? 1 : 0);

        // Cambio automatico
        int gear = sensors.getGear();
        double rpm = sensors.getRPM();
        if (gear < 1) gear = 1;
        if (gear < 6 && rpm >= gearUp[gear - 1]) gear++;
        else if (gear > 1 && rpm <= gearDown[gear - 1]) gear--;
        action.gear = gear;

        // Frizione dinamica
        action.clutch = clutching(sensors, clutch);

        // Salvataggio dati
        try (FileWriter fw = new FileWriter("dataset.csv", true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(
                sensors.getTrackPosition() + "," +
                sensors.getAngleToTrackAxis() + "," +
                sensors.getSpeed() + "," +
                action.accelerate + "," +
                action.brake + "," +
                action.steering + "," +
                action.gear + "\n"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return action;
    }

    private float clutching(SensorModel sensors, float clutch) {
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
    public float[] initAngles() {
        float[] angles = new float[19];
        for (int i = 0; i < 5; i++) angles[i] = -90 + i * 15;
        for (int i = 5; i < 9; i++) angles[i] = -20 + (i - 5) * 5;
        angles[9] = 0;
        for (int i = 0; i < 5; i++) angles[18 - i] = 90 - i * 15;
        for (int i = 5; i < 9; i++) angles[18 - i] = 20 - (i - 5) * 5;
        return angles;
    }

    public void reset() {
        System.out.println("Reset!");
    }

    public void shutdown() {
        System.out.println("Shutdown!");
    }
}
