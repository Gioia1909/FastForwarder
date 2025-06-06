package scr;

//rappresenta un singolo esempio del nostro dataset 
//in inout le 6 features 
//prevedo le azioni di sterzata, accelerazione, freno
//serve per salvare in RAM ogni riga del CSV come oggetto JAVA
//passare i dati facilmente al classificatore
public class DataPoint {
    public double[] features;  // track[8], track[9], track[10], trackPos, angle, speed
    public int steering;       // -1, 0, 1
    public int accelerate;     // 0 o 1
    public int brake;          // 0 o 1

    public DataPoint(double[] features, int steering, int accelerate, int brake) {
        this.features = features;
        this.steering = steering;
        this.accelerate = accelerate;
        this.brake = brake;
    }
}
