package scr;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;

public class ManualDriver extends Controller {

    private boolean accel = false, brake = false, left = false, right = false;
    private float clutch = 0;

    public ManualDriver() {
        // Finestra invisibile per intercettare i tasti
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
        action.gear = 1;
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
                action.steering + "\n"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return action;
    }

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
