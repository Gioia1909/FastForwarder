package scr;

import java.util.*;
//questa classe implementa un KD-Tree per la ricerca di punti nello spazio multidimensionale

public class KDTree {

    // Classe interna per rappresentare un punto dati
    private static class Node {
        DataPoint point; // Punto dati contenuto nel nodo
        Node left, right; // Sottoalbero sinistro e destro
        // Asse lungo il quale il nodo è diviso
        // Questo asse è usato per decidere se il punto si trova nel sottoalbero
        // sinistro o destro
        // L'asse è determinato dal livello di profondità del nodo nell'albero
        // Ad esempio, se il nodo è al livello 0, l'asse sarà 0 (primo asse),
        // se è al livello 1, l'asse sarà 1 (secondo asse), e così via.
        int axis;

        // Costruttore per inizializzare un nodo con un punto e un asse
        // Il punto rappresenta le coordinate del nodo e l'asse indica la dimensione
        // lungo la quale il nodo divide lo spazio
        Node(DataPoint point, int axis) {
            this.point = point;
            this.axis = axis;
        }
    }

    // Radice del KD-Tree
    // La radice è il nodo principale dell'albero e
    // rappresenta il punto di partenza per tutte le operazioni di ricerca
    private final Node root;

    // Costruttore che accetta una lista di punti dati e costruisce il KD-Tree
    // La lista di punti deve essere non vuota e contenere oggetti DataPoint
    public KDTree(List<DataPoint> points) {
        root = build(points, 0);
    }

    // Metodo ricorsivo per costruire il KD-Tree
    // Accetta una lista di punti e un livello di profondità
    // La profondità determina l'asse lungo il quale i punti saranno divisi
    private Node build(List<DataPoint> points, int depth) {
        // Se la lista di punti è vuota, ritorna null
        if (points.isEmpty())
            return null;

        // Calcola l'asse corrente in base alla profondità
        // L'asse è determinato dal modulo della profondità per il numero di dimensioni
        // dei punti (features)
        // Ad esempio, se i punti hanno 3 dimensioni e la profondità è 0, l'asse sarà 0,
        // se la profondità è 1, l'asse sarà 1, e così via.
        int axis = depth % points.get(0).features.length;
        points.sort(Comparator.comparingDouble(p -> p.features[axis]));

        // Trova il punto mediano della lista ordinata
        int median = points.size() / 2;
        // Crea un nuovo nodo con il punto mediano e l'asse corrente
        Node node = new Node(points.get(median), axis);

        // Costruisce ricorsivamente i sottoalberi sinistro e destro
        // Il sottoalbero sinistro contiene i punti a sinistra del punto mediano
        // Il sottoalbero destro contiene i punti a destra del punto mediano
        node.left = build(points.subList(0, median), depth + 1);
        node.right = build(points.subList(median + 1, points.size()), depth + 1);

        // Ritorna il nodo creato
        // Questo nodo diventa parte del KD-Tree e rappresenta una divisione dello
        // spazio
        // in base al punto mediano e all'asse corrente
        return node;
    }

    // Metodo per trovare i k punti più vicini a un punto target
    // Accetta un array di coordinate del punto target e un intero k
    // Restituisce una lista di DataPoint che rappresentano i k punti più vicini
    public List<DataPoint> findKNearest(double[] target, int k) {
        // Crea una coda di priorità per mantenere i k punti più vicini
        // La coda di priorità ordina i punti in base alla distanza dal punto target
        // Utilizza un comparatore che ordina in ordine decrescente della distanza
        // In questo modo, il punto più vicino sarà sempre in cima alla coda
        PriorityQueue<DataPointDistance> heap = new PriorityQueue<>(Comparator.comparingDouble(a -> -a.distance));
        // Inizia la ricerca ricorsiva a partire dalla radice del KD-Tree
        // Passa il punto target, il numero k di punti da trovare e la coda di priorità
        search(root, target, k, heap);
        List<DataPoint> result = new ArrayList<>();
        for (DataPointDistance p : heap)
            result.add(p.point);
        return result;
    }

    // Metodo ricorsivo per cercare i k punti più vicini
    // Accetta un nodo corrente, le coordinate del punto target, il numero k di
    // punti da trovare
    // e una coda di priorità per mantenere i punti più vicini
    private void search(Node node, double[] target, int k, PriorityQueue<DataPointDistance> heap) {
        if (node == null)
            return;

        // Calcola la distanza euclidea tra il punto target e il punto del nodo corrente
        double dist = euclidean(target, node.point.features);
        // Aggiunge il punto corrente alla coda di priorità se la coda ha meno di k
        heap.offer(new DataPointDistance(node.point, dist));
        // Se la coda ha più di k punti, rimuove il punto più lontano
        // Questo mantiene la coda con al massimo k punti più vicini
        if (heap.size() > k)
            heap.poll();

        // Determina quale sottoalbero esplorare in base alla posizione del target
        // Confronta la coordinata del target con la coordinata del punto del nodo
        int axis = node.axis;
        Node near = target[axis] < node.point.features[axis] ? node.left : node.right;
        Node far = near == node.left ? node.right : node.left;

        search(near, target, k, heap);

        if (heap.size() < k || Math.abs(target[axis] - node.point.features[axis]) < heap.peek().distance) {
            search(far, target, k, heap);
        }
    }

    // Metodo per calcolare la distanza euclidea tra due punti
    private double euclidean(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++)
            sum += Math.pow(a[i] - b[i], 2);
        return Math.sqrt(sum);
    }

    // Classe interna per rappresentare un punto dati con la sua distanza
    // Questa classe è utilizzata per mantenere i punti e le loro distanze
    // dalla coda di priorità durante la ricerca dei k punti più vicini
    private static class DataPointDistance {
        DataPoint point;
        double distance;

        DataPointDistance(DataPoint point, double distance) {
            this.point = point;
            this.distance = distance;
        }
    }
}
