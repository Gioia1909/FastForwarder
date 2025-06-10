package scr;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;

public class ManualDriver extends Controller {

    private volatile boolean accel = false, brake = false, left = false, right = false;
    private volatile boolean recording = false;
    private int gear = 1;
    private float steering = 0.0f;
    private float clutch = 0.0f;
    private double currentAccel = 0.0;
    private double currentBrake = 0.0;
    private long lastSaveTime = 0;
    private final long MIN_SAVE_INTERVAL_MS = 100;

    final int[] gearUp = { 5000, 6000, 6000, 6500, 7000, 0 };
    final int[] gearDown = { 0, 2500, 3000, 3000, 3500, 3500 };

    public ManualDriver() {
        JFrame frame = new JFrame("Manual Driver ");
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
                    case KeyEvent.VK_UP -> gear++;
                    case KeyEvent.VK_DOWN -> gear--;
                    case KeyEvent.VK_1 -> {
                        recording = true;
                        System.out.println("▶ Scrittura attivata");
                    }
                    case KeyEvent.VK_0 -> {
                        recording = false;
                        System.out.println("⏹ Scrittura disattivata");
                    }
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
        double speed = sensors.getSpeed();
        double angle = Math.abs(sensors.getAngleToTrackAxis());

        // Acceleratore sfumato
        currentAccel = accel ? Math.min(1.0, currentAccel + 0.2) : Math.max(0.0, currentAccel - 0.2);
        currentBrake = brake ? Math.min(1.0, currentBrake + 0.2) : Math.max(0.0, currentBrake - 0.2);

        // Frenata adattiva per Forza
        if (angle > 0.4)
            currentBrake *= (1 - angle * 0.6); // curve strette
        if (speed < 30.0)
            currentBrake *= 0.5; // frenata ridotta a bassa velocità

        action.accelerate = currentAccel;
        action.brake = currentBrake;

        // Sterzo più morbido per Forza
        float steeringIntensity;
        if (speed <= 40.0)
            steeringIntensity = 0.6f;
        else if (speed <= 100.0)
            steeringIntensity = 0.35f;
        else
            steeringIntensity = 0.2f;

        float effectiveSteering = steeringIntensity * (1.0f - (float) currentBrake);

        if (left && !right)
            steering = -effectiveSteering;
        else if (right && !left)
            steering = effectiveSteering;
        else
            steering = 0.0f;

        steering = Math.max(-0.7f, Math.min(0.7f, steering)); // limitato per evitare sbandate
        action.steering = steering;

        // Cambio automatico morbido
        if (gear < 6 && sensors.getRPM() > gearUp[Math.max(gear - 1, 0)])
            gear++;
        if (gear > 1 && sensors.getRPM() < gearDown[Math.max(gear - 1, 0)])
            gear--;

        gear = Math.max(-1, Math.min(gear, 6));
        action.gear = gear;
        action.clutch = 0.0f;

        // Debug utile
        System.out.printf("Speed: %.1f | Pos: %.2f | Angle: %.2f | Gear: %d\n",
                speed, sensors.getTrackPosition(), sensors.getAngleToTrackAxis(), gear);

        // Scrittura dati
        if (recording && System.currentTimeMillis() - lastSaveTime >= MIN_SAVE_INTERVAL_MS) {
            lastSaveTime = System.currentTimeMillis();
            try {
                File file = new File("dataset.csv");
                boolean fileExists = file.exists();
                boolean fileIsEmpty = file.length() == 0;

                try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                    if (!fileExists || fileIsEmpty) {
                        bw.write(
                                "TrackLeft,TrackCenterLeft,TrackCenter,TrackCenterRight,TrackRight,TrackPosition,AngleToTrackAxis,Speed,Accelerate,Brake,Steering,Gear\n");
                    }

                    double[] t = sensors.getTrackEdgeSensors();
                    bw.write(
                            t[5] + "," + t[7] + "," + t[9] + "," + t[11] + "," + t[13] + "," +
                                    sensors.getTrackPosition() + "," +
                                    sensors.getAngleToTrackAxis() + "," +
                                    speed + "," +
                                    action.accelerate + "," +
                                    action.brake + "," +
                                    action.steering + "," +
                                    action.gear + "\n");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return action;
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
        return super.initAngles();
    }
}
