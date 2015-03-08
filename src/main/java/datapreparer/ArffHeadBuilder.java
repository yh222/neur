/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datapreparer;

import datapreparer.valuemaker.ClassValueMaker;
import datapreparer.valuemaker.TrainingValueMaker;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArffBuilder {

    public static void buildArffHeader(Set<String> keys, String fileName, String code) {
        //String newName = fileName.substring(0, fileName.length());
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(
                new FileWriter(fileName, false)))) {
            writer.println("@relation " + code + "_Training\n");

            String str;
            for (String key : keys) {
                if (key.regionMatches(0, ClassValueMaker.cls, 0, ClassValueMaker.cls.length())) {
                    //Class
                    str = "{Very_Low,Low,Little_Low,Stay"
                            + ",Little_High,High,Very_High}";
                } else if (key.regionMatches(0, TrainingValueMaker.nom, 0, TrainingValueMaker.nom.length())) {
                    //Nominal
                    switch (key) {
                        case TrainingValueMaker.nom + "DayOfWeek":
                            str = "{Fri,Tue,Mon,Thu,Wed}";
                            break;
                        case TrainingValueMaker.nom + "Season":
                            str = "{Spring,Summer,Autumn,Winter}";
                            break;
                        default:
                            str = "{StarB,StarW,ShortWD,ShortB,LongB,StarBD"
                                    + ",StarWTT,ShortWTT,StarBTT,ShortBD,ShortBTT"
                                    + ",StarBU,StarWU,ShortW,GreadB,LongW,ShortBU"
                                    + ",StarWD,ShortWU,GreatW}";
                            break;
                    }
                } else {
                    //Numeric
                    str="numeric";
                }
                writer.println("@attribute " + key + " " + str);
            }

            writer.println("@data");
            //return newName;
        } catch (IOException ex) {
            Logger.getLogger(ArffBuilder.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        //return null;
    }

}
