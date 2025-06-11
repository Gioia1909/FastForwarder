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
        action.steering = steering;
        action.gear = gear;
        action.clutch = clutching(sensors, clutch);

        double speed = sensors.getSpeed();
        double speedY = sensors.getLateralSpeed();
        double distance = sensors.getDistanceFromStartLine();

        // Imposta il focus per attivare i sensori focus (obbligatorio)
        if (Math.abs(steering) > 0.3) {
            int dynamicFocus = (int) Math.round(steering * 90);
            action.focus = Math.max(-90, Math.min(90, dynamicFocus));
        } else {
            action.focus = 0;
        }
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
                            bw.write(
                                    "Distanza," +
                                            "Track3,Track4,Track5,Track6,Track7,Track8,Track9,Track10,Track11,Track12,Track13,Track14,Track15,Track16,"
                                            +
                                            "Focus1,Focus2,Focus3," +
                                            "TrackPosition,AngleToTrackAxis,Speed,SpeedY,Damage," +
                                            "DistanceRaced,RPM," +
                                            "Accelerate,Brake,Steering,Gear\n");
                        }

                        double[] trackSensors = sensors.getTrackEdgeSensors();
                        double[] focusSensors = sensors.getFocusSensors();

                        double damage = sensors.getDamage();
                        double distanceRaced = sensors.getDistanceRaced();
                        double rpm = sensors.getRPM();

                        // Sicurezza
                        if (trackSensors.length < 17 || focusSensors.length < 5) {
                            System.err.println("Errore: array sensori non sufficienti!");
                            return action;
                        }

                        // Scrittura dati
                        StringBuilder sb = new StringBuilder();
                        sb.append(distance).append(",");

                        // TrackEdgeSensors 3–16
                        for (int i = 3; i <= 16; i++) {
                            sb.append(trackSensors[i]).append(",");
                        }

                        // FocusSensors 0–4
                        for (int i = 1; i <= 3; i++) {
                            sb.append(focusSensors[i]).append(",");
                        }

                        sb.append(sensors.getTrackPosition()).append(",");
                        sb.append(sensors.getAngleToTrackAxis()).append(",");
                        sb.append(speed).append(",");
                        sb.append(speedY).append(",");
                        sb.append(damage).append(",");
                        sb.append(distanceRaced).append(",");
                        sb.append(rpm).append(",");

                        sb.append(action.accelerate).append(",");
                        sb.append(action.brake).append(",");
                        sb.append(action.steering).append(",");
                        sb.append(action.gear).append("\n");

                        bw.write(sb.toString());
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