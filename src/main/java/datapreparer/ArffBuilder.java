/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datapreparer;

import core.GlobalConfigs;
import core.GlobalConfigs.CLASS_VALUES;
import core.GlobalConfigs.TRAINING_VALUES_NOMINAL;
import core.GlobalConfigs.TRAINING_VALUES_NUMERIC;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArffBuilder {

    public static String covenertArff(String fileName, String code) {
        String newName = fileName.substring(0, fileName.length() - 3) + "arff";
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(
                new FileWriter(newName, false)))) {
            writer.println("@relation " + code + "_Training\n");
            //writer.println(h1 + h2);
            //Write as nominal training
            writeToNominal(writer, true);
            //Write as numeric training
            writeToNumeric(writer);
            //Write as Class
            writeToNominal(writer, false);

            writer.println("@data");
            return newName;
        } catch (IOException ex) {
            Logger.getLogger(ArffBuilder.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private static void writeToNominal(PrintWriter writer, boolean training) {
        if (training) {
            for (TRAINING_VALUES_NOMINAL o : TRAINING_VALUES_NOMINAL.values()) {
                String str;
                if (o.name()
                        .equals(TRAINING_VALUES_NOMINAL.DAY_OF_WEEK.name())) {
                    str = "{Fri,Tue,Mon,Thu,Wed}";
                } else if (o.name()
                        .equals(TRAINING_VALUES_NOMINAL.SEASON_YEAR.name())) {
                    str = "{Spring,Summer,Autumn,Winter}";
                } else {
                    str = "{StarB,StarW,ShortWD,ShortB,LongB,StarBD"
                            + ",StarWTT,ShortWTT,StarBTT,ShortBD,ShortBTT"
                            + ",StarBU,StarWU,ShortW,GreadB,LongW,ShortBU"
                            + ",StarWD,ShortWU,GreatW}";
                }
                writer.println("@attribute " + o.name() + " " + str);
            }
        } else {
            for (CLASS_VALUES o : CLASS_VALUES.values()) {
                String str = "{Very_Low,Low,Little_Low,Stay"
                        + ",Little_High,High,Very_High}";
                writer.println("@attribute " + o.name() + " " + str);
            }
        }
    }

    private static void writeToNumeric(PrintWriter writer) {
        for (TRAINING_VALUES_NUMERIC o : TRAINING_VALUES_NUMERIC.values()) {
            writer.println("@attribute " + o.name() + " numeric");
        }
    }

}
