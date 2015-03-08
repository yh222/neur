/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datapreparer;

import core.GlobalConfigs;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArffHeadBuilder {

  public static void buildArffHeader(Set<String> keys, String fileName, String code) {
    //String newName = fileName.substring(0, fileName.length());
    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(fileName, false)))) {
      writer.println("@relation " + code + "_Training\n");

      String str;
      for (String key : keys) {
        if (key.regionMatches(0, GlobalConfigs.CLS, 0, GlobalConfigs.CLS.length())) {
          //Class
          str = "{Very_Low,Low,Little_Low,Stay"
                  + ",Little_High,High,Very_High}";
        } else if (key.regionMatches(0, GlobalConfigs.NOM, 0, GlobalConfigs.NOM.length())) {
          //Nominal
          switch (key) {
            case GlobalConfigs.NOM + "DayOfWeek":
              str = "{Fri,Tue,Mon,Thu,Wed}";
              break;
            case GlobalConfigs.NOM + "Season":
              str = "{Spring,Summer,Autumn,Winter}";
              break;
            case GlobalConfigs.NOM + "Date":
              str = "DATE \"yyyy-MM-dd\"";
              break;
            default:
              str = "{StarB,StarW,ShortWD,ShortB,LongB,StarBD"
                      + ",StarWTT,StarBTT,ShortBD"
                      + ",StarBU,StarWU,ShortW,GreatB,LongW,ShortBU"
                      + ",StarWD,ShortWU,GreatW}";
              break;
          }
        } else {
          //Numeric
          str = "numeric";
        }
        writer.println("@attribute " + key + " " + str);
      }

      writer.println("@data");
      //return newName;
    } catch (IOException ex) {
      Logger.getLogger(ArffHeadBuilder.class.getName())
              .log(Level.SEVERE, null, ex);
    }
    //return null;
    //return null;
  }

}