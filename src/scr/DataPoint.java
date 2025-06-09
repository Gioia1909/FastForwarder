package scr;

//rappresenta un singolo esempio del nostro dataset 
//in inout le 6 features 
//prevedo le azioni di sterzata, accelerazione, freno
//serve per salvare in RAM ogni riga del CSV come oggetto JAVA
//passare i dati facilmente al classificatore
public class DataPoint {
    public double[] features;
    public double steering; // -1, 0, 1
    public double accelerate; // 0 o 1
    public double brake; // 0 o 1
    public int gear;

    public DataPoint(double[] features, double steering, double accelerate, double brake, int gear) {
        this.features = features;
        this.steering = steering;
        this.accelerate = accelerate;
        this.brake = brake;
        this.gear = gear;
    }

    public double[] getFeatures() {
        return features;
    }

    public double getSteering() {
        return steering;
    }

    public double getAccelerate() {
        return accelerate;
    }

    public double getBrake() {
        return brake;
    }

    public int getGear() {
        return gear;
    }
}
