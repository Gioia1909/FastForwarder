package scr;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;

public class ManualDriver extends Controller {

    private volatile boolean accel = false, brake = false, left = false, right = false;
    private volatile boolean recording = false;
    private float clutch = 0;
    private int gear = 1;
    private long lastSaveTime = 0;
    private final long MIN_SAVE_INTERVAL_MS = 300; // salva ogni 300 ms max

    private double lastSteering = 0;
    private double lastSpeed = 0;
    private double lastAngle = 0;

    final int[] gearUp = { 5000, 6000, 6000, 6500, 7000, 0 };
    final int[] gearDown = { 0, 2500, 3000, 3000, 3500, 3500 };

    public ManualDriver() {
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
                    case KeyEvent.VK_UP -> gear++;
                    case KeyEvent.VK_DOWN -> gear--;
                    case KeyEvent.VK_1 -> {
                        recording = true;
                        System.out.println("Scrittura attivata");
                    }
                    case KeyEvent.VK_0 -> {
                        recording = false;
                        System.out.println("Scrittura disattivata");
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

        action.accelerate = accel ? 1.0 : 0.0;
        action.brake = brake ? 0.5 : 0.0;

        double speed = sensors.getSpeed(); // puoi usare getSpeedX() se più preciso
        float steeringIntensity = (speed <= 40.0) ? 1.0f : 0.2f;

        if (left) {
            action.steering = steeringIntensity;
        } else if (right) {
            action.steering = -steeringIntensity;
        } else {
            action.steering = 0.0f;
        }

        if (gear < -1)
            gear = -1;
        if (gear > 6)
            gear = 6;
        action.gear = gear;

        action.clutch = clutching(sensors, clutch);

        // Scrivi nel CSV solo se recording è attivo
        if (recording) {
            long currentTime = System.currentTimeMillis();
            boolean timeElapsed = currentTime - lastSaveTime >= MIN_SAVE_INTERVAL_MS;

            double steering = action.steering;
            speed = sensors.getSpeed(); // senza "double"
            double angle = sensors.getAngleToTrackAxis();

            boolean significantChange = Math.abs(steering - lastSteering) > 0.05 ||
                    Math.abs(speed - lastSpeed) > 2.0 ||
                    Math.abs(angle - lastAngle) > 0.02;

            if (timeElapsed && significantChange) {
                lastSaveTime = currentTime;
                lastSteering = steering;
                lastSpeed = speed;
                lastAngle = angle;

                try {
                    File file = new File("dataset.csv");
                    boolean fileExists = file.exists();
                    boolean fileIsEmpty = file.length() == 0;
                    String mode = "normal";
                    if (Math.abs(sensors.getTrackPosition()) > 0.9 || sensors.getSpeed() < 3
                            || sensors.getDamage() > 0) {
                        mode = "recovery";
                    }

                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                        if (!fileExists || fileIsEmpty) {
                            bw.write(
                                    "TrackLeft, TrackCenterLeft, TrackCenter, TrackCenterRight, TrackRight,TrackPosition,AngleToTrackAxis,Speed,Accelerate,Brake,Steering, Gear\n");

                        }

                        double[] trackSensors = sensors.getTrackEdgeSensors();

                        bw.write(
                            trackSensors[5] + "," +   
                            trackSensors[7] + "," +   
                            trackSensors[9] + "," +   
                            trackSensors[11] + "," +  
                            trackSensors[13] + "," +  
                            sensors.getTrackPosition() + "," +
                            sensors.getAngleToTrackAxis() + "," +
                            speed + "," +
                            action.accelerate + "," +
                            action.brake + "," +
                            steering + "," +
                            action.gear + "\n"
);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
