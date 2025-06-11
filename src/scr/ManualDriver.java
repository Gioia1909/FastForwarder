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
    private final long MIN_SAVE_INTERVAL_MS = 50; // salva ogni 50 ms max
    private float steering = 0.0f;
    private double currentAccel = 0.0;
    private double currentBrake = 0.0;

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
                    case KeyEvent.VK_UP -> {
                        if (gear < 6)
                            gear++;
                    }
                    case KeyEvent.VK_DOWN -> {
                        if (gear > -1)
                            gear--;
                    }

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

        updateState();

        action.accelerate = currentAccel;
        action.brake = currentBrake;

        double speed = sensors.getSpeed();

        action.steering = steering;
        action.gear = gear;

        /** NUOVA AGGIUNTA DISTANZA */
        double distance = sensors.getDistanceFromStartLine();

        action.clutch = clutching(sensors, clutch);

        // Scrivi nel CSV solo se recording è attivo
        if (recording) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSaveTime >= MIN_SAVE_INTERVAL_MS) {
                lastSaveTime = currentTime;

                try {
                    File file = new File("dataset.csv");
                    boolean fileExists = file.exists();
                    boolean fileIsEmpty = file.length() == 0;

                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                        if (!fileExists || fileIsEmpty) {
                            /** NUOVA AGGIUNTA DISTANZA */
                            bw.write(
                                    "Distanza,TrackLeft,TrackCenterLeft,TrackCenter,TrackCenterRight,TrackRight," +
                                            "FocusLeft,FocusCenter,FocusRight," +
                                            "TrackPosition,AngleToTrackAxis,Speed," +
                                            "Accelerate,Brake,Steering,Gear\n");
                        }
                        double[] trackSensors = sensors.getTrackEdgeSensors();
                        double[] focusSensors = sensors.getFocusSensors();

                        bw.write(
                                /** NUOVA AGGIUNTA DISTANZA */
                                distance + "," +
                                        trackSensors[5] + "," +
                                        trackSensors[7] + "," +
                                        trackSensors[9] + "," +
                                        trackSensors[11] + "," +
                                        trackSensors[13] + "," +
                                        focusSensors[1] + "," + // Focus sinistra
                                        focusSensors[2] + "," + // Focus centro
                                        focusSensors[3] + "," + // Focus destra
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

    // funzione aggiunta da Giuliano
    private void updateState() {
        // --- Accelerazione e frenata ---

        if (accel) {
            currentAccel += 0.2;
            if (currentAccel > 1)
                currentAccel = 1;
        }

        if (brake) {
            currentAccel = Math.max(0, currentAccel - 0.4); // frenata = riduzione accelerazione
            currentBrake += 0.2;
            if (currentBrake > 1)
                currentBrake = 1;
        } else {
            currentBrake -= 0.3;
            if (currentBrake < 0)
                currentBrake = 0;
        }

        // Decelerazione naturale se né accell né freno
        if (!accel && !brake) {
            currentAccel = Math.max(0, currentAccel - 0.2);
        }

        // --- Sterzo ---
        if (left) {
            steering += 0.2;
            if (steering > 1)
                steering = 1;
        } else if (right) {
            steering -= 0.2;
            if (steering < -1)
                steering = -1;
        } else {
            if (steering > 0) {
                steering -= 0.5;
                if (steering < 0)
                    steering = 0;
            } else if (steering < 0) {
                steering += 0.5;
                if (steering > 0)
                    steering = 0;
            }
        }
    }
}