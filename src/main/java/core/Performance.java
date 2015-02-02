package core;

import core.GlobalConfigs.CLASS_VALUES;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Performance {

    // Structure: Code -> (Classifier-> Data)
    public static final ConcurrentHashMap<String, ConcurrentHashMap<String, double[][]>> PERFORMANCES = loadMap();

    public static void putClassifier(String classifierName) {
        for (ConcurrentHashMap<String, double[][]> m : PERFORMANCES.values()) {
            if (!m.containsKey(classifierName)) {
                m.put(classifierName, new double[CLASS_VALUES.SIZE][]);
            } else {
                break;
            }
        }
    }

    public static void putClassResult(String code, String classifierName, double[] result, int index) {
        PERFORMANCES.get(code).get(classifierName)[index] = result;
    }

    private static ConcurrentHashMap<String, ConcurrentHashMap<String, double[][]>> loadMap() {
        ConcurrentHashMap<String, ConcurrentHashMap<String, double[][]>> map
                = new ConcurrentHashMap();
        ArrayList<String> instruments = GlobalConfigs.INSTRUMENT_CODES;
        for (String i : instruments) {
            map.put(i, new ConcurrentHashMap<String, double[][]>());
        }
        return map;
    }

    public static void outputPerformanceData() {
        String code, classifier;
        NumberFormat defaultFormat = NumberFormat.getNumberInstance();
        defaultFormat.setMinimumFractionDigits(2);

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(
                new FileWriter("report.csv", true)))) {

            for (Entry<String, ConcurrentHashMap<String, double[][]>> e1 : PERFORMANCES.entrySet()) {
                code = e1.getKey();
                writer.println("\nCode: " + code);
                for (Entry<String, double[][]> e2 : e1.getValue().entrySet()) {
                    classifier = e2.getKey();
                    //String[] split;
                    writer.println("Classifier: " + classifier);
                    writer.println(",VL,ML,LL,LH,MH,VH");
                    for (int i = 0; i < CLASS_VALUES.SIZE; i++) {
                        writer.println(CLASS_VALUES.values()[i].name()
                                + "," + defaultFormat.format(e2.getValue()[i][0])
                                + "," + defaultFormat.format(e2.getValue()[i][1])
                                + "," + defaultFormat.format(e2.getValue()[i][2])
                                + "," + defaultFormat.format(e2.getValue()[i][4])
                                + "," + defaultFormat.format(e2.getValue()[i][5])
                                + "," + defaultFormat.format(e2.getValue()[i][6])
                        );
                    }
                }
                writer.println();
            }
        } catch (IOException ex) {
            Logger.getLogger(Performance.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
