package scr;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class DatasetMerger {

    public static void main(String[] args) throws IOException {
        String realDataset = "dataset.csv";
        String guidedDataset = "guided_dataset.csv";
        String outputDataset = "mixed_dataset.csv";

        List<String> allLines = new ArrayList<>();

        // Leggi intestazione una sola volta
        String header = Files.lines(Paths.get(realDataset)).findFirst().orElse(null);
        if (header == null) {
            System.err.println("Errore: file reale vuoto.");
            return;
        }

        allLines.add(header); // aggiungi intestazione all'inizio

        // Aggiungi dati reali (escludi intestazione)
        Files.lines(Paths.get(realDataset)).skip(1).forEach(allLines::add);

        // Aggiungi dati guidati (escludi intestazione)
        Files.lines(Paths.get(guidedDataset)).skip(1).forEach(allLines::add);

        // Mescola tutto casualmente
        Collections.shuffle(allLines.subList(1, allLines.size())); // lascia l'intestazione in alto

        // Scrivi su nuovo file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputDataset))) {
            for (String line : allLines) {
                bw.write(line);
                bw.newLine();
            }
        }

        System.out.println("Dataset combinato salvato in " + outputDataset);
    }
}
