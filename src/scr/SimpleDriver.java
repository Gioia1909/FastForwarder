package scr;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class SimpleDriver extends Controller {
	private KNNClassifier classifier;
	private double[] min;
	private double[] max;
	private BufferedWriter logWriter;
	// logging
	private double[] lastRawInput = null;
	private double lastSteer = 0;
	private double lastAccel = 0;
	private int lastGear = -999;
	private static int stepCounter = 0;
	private long lastTime = System.currentTimeMillis();

	public SimpleDriver() {
		try {
			List<DataPoint> dataset = DatasetLoader.load("dataset.csv");
			classifier = new KNNClassifier(dataset, 51);
			double[][] minMax = DatasetLoader.loadMinMaxDaFile("min.txt", "max.txt");
			logWriter = new BufferedWriter(new FileWriter("log_predizioni.csv"));
			logWriter.write(
					"TrackLeft,TrackCenterLeft,TrackCenter,TrackCenterRight,TrackRight,TrackPosition,AngleToTrackAxis,Speed,Norm_TrackLeft,Norm_TrackCenterLeft,Norm_TrackCenter,Norm_TrackCenterRight,Norm_TrackRight,Norm_TrackPosition,Norm_AngleToTrackAxis,Norm_Speed,Pred_Gear,Pred_Steer,Pred_Accel,Pred_Brake\n");
			min = minMax[0];
			max = minMax[1];
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* Costanti di cambio marcia */
	final int[] gearUp = { 5000, 6000, 6000, 6500, 7000, 0 };
	final int[] gearDown = { 0, 2500, 3000, 3000, 3500, 3500 };

	/* Constanti */
	final int stuckTime = 25;
	final float stuckAngle = (float) 0.523598775; // PI/6

	/* Costanti di accelerazione e di frenata */
	final float maxSpeedDist = 70;
	final float maxSpeed = 150;
	final float sin5 = (float) 0.08716;
	final float cos5 = (float) 0.99619;

	/* Costanti di sterzata */
	final float steerLock = (float) 0.785398;
	final float steerSensitivityOffset = (float) 80.0;
	final float wheelSensitivityCoeff = 1;

	/* Costanti del filtro ABS */
	final float wheelRadius[] = { (float) 0.3179, (float) 0.3179, (float) 0.3276, (float) 0.3276 };
	final float absSlip = (float) 2.0;
	final float absRange = (float) 3.0;
	final float absMinSpeed = (float) 3.0;

	/* Costanti da stringere */
	final float clutchMax = (float) 0.5;
	final float clutchDelta = (float) 0.05;
	final float clutchRange = (float) 0.82;
	final float clutchDeltaTime = (float) 0.02;
	final float clutchDeltaRaced = 10;
	final float clutchDec = (float) 0.01;
	final float clutchMaxModifier = (float) 1.3;
	final float clutchMaxTime = (float) 1.5;

	private int stuck = 0;

	// current clutch
	private float clutch = 0;

	public void reset() {
		System.out.println("Restarting the race!");
		try {
			List<DataPoint> dataset = DatasetLoader.load("dataset.csv");
			classifier = new KNNClassifier(dataset, 29); // oppure 1
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void shutdown() {
		System.out.println("Bye bye!");
		try {
			if (logWriter != null)
				logWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int getGear(SensorModel sensors) {
		int gear = sensors.getGear();
		double rpm = sensors.getRPM();

		// Se la marcia è 0 (N) o -1 (R) restituisce semplicemente 1
		if (gear < 1)
			return 1;

		// Se il valore di RPM dell'auto è maggiore di quello suggerito
		// sale di marcia rispetto a quella attuale
		if (gear < 6 && rpm >= gearUp[gear - 1])
			return gear + 1;
		else

		// Se il valore di RPM dell'auto è inferiore a quello suggerito
		// scala la marcia rispetto a quella attuale
		if (gear > 1 && rpm <= gearDown[gear - 1])
			return gear - 1;
		else // Altrimenti mantenere l'attuale
			return gear;
	}

	private float getSteer(SensorModel sensors) {
		/**
		 * L'angolo di sterzata viene calcolato correggendo l'angolo effettivo della
		 * vettura
		 * rispetto all'asse della pista [sensors.getAngle()] e regolando la posizione
		 * della vettura
		 * rispetto al centro della pista [sensors.getTrackPos()*0,5].
		 */
		float targetAngle = (float) (sensors.getAngleToTrackAxis() - sensors.getTrackPosition() * 0.5);
		// ad alta velocità ridurre il comando di sterzata per evitare di perdere il
		// controllo
		if (sensors.getSpeed() > steerSensitivityOffset)
			return (float) (targetAngle
					/ (steerLock * (sensors.getSpeed() - steerSensitivityOffset) * wheelSensitivityCoeff));
		else
			return (targetAngle) / steerLock;
	}

	private float getAccel(SensorModel sensors) {
		// controlla se l'auto è fuori dalla carreggiata
		if (sensors.getTrackPosition() > -1 && sensors.getTrackPosition() < 1) {
			// lettura del sensore a +5 gradi rispetto all'asse dell'automobile
			float rxSensor = (float) sensors.getTrackEdgeSensors()[10];
			// lettura del sensore parallelo all'asse della vettura
			float sensorsensor = (float) sensors.getTrackEdgeSensors()[9];
			// lettura del sensore a -5 gradi rispetto all'asse dell'automobile
			float sxSensor = (float) sensors.getTrackEdgeSensors()[8];

			float targetSpeed;

			// Se la pista è rettilinea e abbastanza lontana da una curva, quindi va alla
			// massima velocità
			if (sensorsensor > maxSpeedDist || (sensorsensor >= rxSensor && sensorsensor >= sxSensor))
				targetSpeed = maxSpeed;
			else {
				// In prossimità di una curva a destra
				if (rxSensor > sxSensor) {

					// Calcolo dell'"angolo" di sterzata
					float h = sensorsensor * sin5;
					float b = rxSensor - sensorsensor * cos5;
					float sinAngle = b * b / (h * h + b * b);

					// Set della velocità in base alla curva
					targetSpeed = maxSpeed * (sensorsensor * sinAngle / maxSpeedDist);
				}
				// In prossimità di una curva a sinistra
				else {
					// Calcolo dell'"angolo" di sterzata
					float h = sensorsensor * sin5;
					float b = sxSensor - sensorsensor * cos5;
					float sinAngle = b * b / (h * h + b * b);

					// eSet della velocità in base alla curva
					targetSpeed = maxSpeed * (sensorsensor * sinAngle / maxSpeedDist);
				}
			}

			/**
			 * Il comando di accelerazione/frenata viene scalato in modo esponenziale
			 * rispetto
			 * alla differenza tra velocità target e quella attuale
			 */
			return (float) (2 / (1 + Math.exp(sensors.getSpeed() - targetSpeed)) - 1);
		} else
			// Quando si esce dalla carreggiata restituisce un comando di accelerazione
			// moderata
			return (float) 0.3;
	}

	public Action control(SensorModel sensors) {
		// Controlla se l'auto è attualmente bloccata
		/**
		 * Se l'auto ha un angolo, rispetto alla traccia, superiore a 30°
		 * incrementa "stuck" che è una variabile che indica per quanti cicli l'auto è
		 * in
		 * condizione di difficoltà.
		 * Quando l'angolo si riduce, "stuck" viene riportata a 0 per indicare che
		 * l'auto è
		 * uscita dalla situaizone di difficoltà
		 **/
		if (Math.abs(sensors.getAngleToTrackAxis()) > stuckAngle) {
			// update stuck counter
			stuck++;
		} else {
			// if not stuck reset stuck counter
			stuck = 0;
		}

		// Applicare la polizza di recupero o meno in base al tempo trascorso
		/**
		 * Se "stuck" è superiore a 25 (stuckTime) allora procedi a entrare in
		 * situaizone di RECOVERY
		 * per far fronte alla situazione di difficoltà
		 **/

		if (stuck > stuckTime) { // Auto Bloccata
			/**
			 * Impostare la marcia e il comando di sterzata supponendo che l'auto stia
			 * puntando
			 * in una direzione al di fuori di pista
			 **/

			// Per portare la macchina parallela all'asse TrackPos
			float steer = (float) (-sensors.getAngleToTrackAxis() / steerLock);
			int gear = -1; // Retromarcia

			// Se l'auto è orientata nella direzione corretta invertire la marcia e sterzare
			if (sensors.getAngleToTrackAxis() * sensors.getTrackPosition() > 0) {
				gear = 1;
				steer = -steer;
			}
			clutch = clutching(sensors, clutch);
			// Costruire una variabile CarControl e restituirla
			Action action = new Action();
			action.gear = gear;
			action.steering = steer;
			action.accelerate = 1.0;
			action.brake = 0;
			action.clutch = clutch;
			return action;
		} else { // Auto non bloccata
			// Leggi i sensori una volta sola
			double[] track = sensors.getTrackEdgeSensors();
			double[] focus = sensors.getFocusSensors();

			// Costruisci il rawInput usando gli stessi indici del DatasetLoader
			double[] rawInput = new double[DatasetLoader.FEATURE_INDICES.length];
			for (int i = 0; i < DatasetLoader.FEATURE_INDICES.length; i++) {
				int idx = DatasetLoader.FEATURE_INDICES[i];
				switch (idx) {
					// case DatasetLoader.IDX_DISTANCE -> rawInput[i] =
					// sensors.getDistanceFromStartLine();
					// case DatasetLoader.IDX_TRACK3 -> rawInput[i] = track[3];
					// case DatasetLoader.IDX_TRACK4 -> rawInput[i] = track[4];
					case DatasetLoader.IDX_TRACK5 -> rawInput[i] = track[5];
					// case DatasetLoader.IDX_TRACK6 -> rawInput[i] = track[6];
					case DatasetLoader.IDX_TRACK7 -> rawInput[i] = track[7];
					// case DatasetLoader.IDX_TRACK8 -> rawInput[i] = track[8];
					case DatasetLoader.IDX_TRACK9 -> rawInput[i] = track[9];
					// case DatasetLoader.IDX_TRACK10 -> rawInput[i] = track[10];
					case DatasetLoader.IDX_TRACK11 -> rawInput[i] = track[11];
					// case DatasetLoader.IDX_TRACK12 -> rawInput[i] = track[12];
					case DatasetLoader.IDX_TRACK13 -> rawInput[i] = track[13];
					// case DatasetLoader.IDX_TRACK14 -> rawInput[i] = track[14];
					// case DatasetLoader.IDX_TRACK15 -> rawInput[i] = track[15];
					// case DatasetLoader.IDX_TRACK16 -> rawInput[i] = track[16];
					// case DatasetLoader.IDX_FOCUS1 -> rawInput[i] = focus[1];
					// case DatasetLoader.IDX_FOCUS2 -> rawInput[i] = focus[2];
					// case DatasetLoader.IDX_FOCUS3 -> rawInput[i] = focus[3];
					case DatasetLoader.IDX_TRACK_POS -> rawInput[i] = sensors.getTrackPosition();
					case DatasetLoader.IDX_ANGLE -> rawInput[i] = sensors.getAngleToTrackAxis();
					case DatasetLoader.IDX_SPEED -> rawInput[i] = sensors.getSpeed();
					case DatasetLoader.IDX_SPEEDY -> rawInput[i] = sensors.getLateralSpeed();
					// case DatasetLoader.IDX_DAMAGE -> rawInput[i] = sensors.getDamage();
					// case DatasetLoader.IDX_DISTANCE_RACED -> rawInput[i] =
					// sensors.getDistanceRaced();
					// case DatasetLoader.IDX_RPM -> rawInput[i] = sensors.getRPM();
					default -> rawInput[i] = 0.0;
				}

			}

			// Normalizza
			double[] input = new double[rawInput.length];
			for (int i = 0; i < rawInput.length; i++) {
				input[i] = (max[i] != min[i]) ? (rawInput[i] - min[i]) / (max[i] - min[i]) : 0.0;
			}

			int gear = classifier.predictGear(input, sensors.getRPM(), sensors.getGear(), gearUp, gearDown);
			double steer = classifier.predictSteering(input);
			double accel = classifier.predictAccelerate(input);
			double brake = classifier.predictBrake(input);

			lastSteer = steer;
			lastAccel = accel;
			lastGear = gear;

			clutch = clutching(sensors, clutch);
			long currentTime = System.currentTimeMillis();
			double deltaTime = (currentTime - lastTime) / 1000.0;
			lastTime = currentTime;

			boolean shouldLog = false;
			if (lastRawInput == null) {
				lastRawInput = new double[rawInput.length];
				shouldLog = true;
			} else {
				for (int i = 0; i < rawInput.length; i++) {
					if (Math.abs(rawInput[i] - lastRawInput[i]) > 1e-4) {
						shouldLog = true;
						break;
					}
				}
				if (Math.abs(steer - lastSteer) > 0.01 || Math.abs(accel - lastAccel) > 0.01 || gear != lastGear) {
					shouldLog = true;
				}
			}

			if (shouldLog) {
				try {
					StringBuilder log = new StringBuilder();
					log.append(stepCounter++).append(",").append(currentTime).append(",").append(deltaTime).append(",");
					for (double val : rawInput)
						log.append(val).append(",");
					for (double val : input)
						log.append(val).append(",");
					log.append(gear).append(",").append(steer).append(",").append(accel).append(",").append(brake)
							.append("\n");

					logWriter.write(log.toString());
					logWriter.flush();
					System.arraycopy(rawInput, 0, lastRawInput, 0, rawInput.length);

				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			Action action = new Action();
			action.gear = gear;
			action.steering = steer;
			action.accelerate = accel;
			action.brake = brake;
			action.clutch = clutch;

			/*
			 * if (Math.abs(steer) > 0.3) {
			 * int dynamicFocus = (int) Math.round(steer * 90);
			 * action.focus = Math.max(-90, Math.min(90, dynamicFocus));
			 * } else {
			 * action.focus = 0;
			 * }
			 * 
			 * if (Math.abs(sensors.getAngleToTrackAxis()) > 0.2 && sensors.getSpeed() > 30)
			 * {
			 * accel *= 0.5;
			 * }
			 */

			return action;
		}
	}

	private float filterABS(SensorModel sensors, float brake) {
		// Converte la velocità in m/s
		float speed = (float) (sensors.getSpeed() / 3.6);

		// Quando la velocità è inferiore alla velocità minima per l'abs non interviene
		// in caso di frenata
		if (speed < absMinSpeed)
			return brake;

		// Calcola la velocità delle ruote in m/s
		float slip = 0.0f;
		for (int i = 0; i < 4; i++) {
			slip += sensors.getWheelSpinVelocity()[i] * wheelRadius[i];
		}

		// Lo slittamento è la differenza tra la velocità effettiva dell'auto e la
		// velocità media delle ruote
		slip = speed - slip / 4.0f;

		// Quando lo slittamento è troppo elevato, si applica l'ABS
		if (slip > absSlip) {
			brake = brake - (slip - absSlip) / absRange;
		}

		// Controlla che il freno non sia negativo, altrimenti lo imposta a zero
		if (brake < 0)
			return 0;
		else
			return brake;
	}

	float clutching(SensorModel sensors, float clutch) {

		float maxClutch = clutchMax;

		// Controlla se la situazione attuale è l'inizio della gara
		if (sensors.getCurrentLapTime() < clutchDeltaTime && getStage() == Stage.RACE
				&& sensors.getDistanceRaced() < clutchDeltaRaced)
			clutch = maxClutch;

		// Regolare il valore attuale della frizione
		if (clutch > 0) {
			double delta = clutchDelta;
			if (sensors.getGear() < 2) {

				// Applicare un'uscita più forte della frizione quando la marcia è una e la
				// corsa è appena iniziata.
				delta /= 2;
				maxClutch *= clutchMaxModifier;
				if (sensors.getCurrentLapTime() < clutchMaxTime)
					clutch = maxClutch;
			}

			// Controllare che la frizione non sia più grande dei valori massimi
			clutch = Math.min(maxClutch, clutch);

			// Se la frizione non è al massimo valore, diminuisce abbastanza rapidamente
			if (clutch != maxClutch) {
				clutch -= delta;
				clutch = Math.max((float) 0.0, clutch);
			}
			// Se la frizione è al valore massimo, diminuirla molto lentamente.
			else
				clutch -= clutchDec;
		}
		return clutch;
	}

	public float[] initAngles() {

		float[] angles = new float[19];

		/*
		 * set angles as
		 * {-90,-75,-60,-45,-30,-20,-15,-10,-5,0,5,10,15,20,30,45,60,75,90}
		 */
		for (int i = 0; i < 5; i++) {
			angles[i] = -90 + i * 15;
			angles[18 - i] = 90 - i * 15;
		}

		for (int i = 5; i < 9; i++) {
			angles[i] = -20 + (i - 5) * 5;
			angles[18 - i] = 20 - (i - 5) * 5;
		}
		angles[9] = 0;
		return angles;
	}
}
