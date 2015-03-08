package datapreparer;

import core.TrainingFileGenerator;
import static core.GlobalConfigs.RESOURCE_PATH;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RawDataLoader {

  // Return a map of raw data array
  // Data order(index start at 0!): 1, open; 2, high; 3, low; 4, close; 5, volume
  // TODO: import data from different files to one map
  public static ConcurrentHashMap<String, Object[]> loadRawDataFromFile(String code, String modelTypePath) {
    ConcurrentHashMap<String, Object[]> raw_data_map = new ConcurrentHashMap();

    File folder = new File(RESOURCE_PATH + modelTypePath + code);
    if (!folder.isDirectory()) {
      Logger.getLogger(TrainingFileGenerator.class.getName()).log(Level.WARNING, "Failed to find directory for: {0}", code);
      return null;
    }
    File file = new File(RESOURCE_PATH + modelTypePath + code + "//" + code + ".csv");
    if (file.isFile()) {
      try (BufferedReader reader = new BufferedReader(
              new FileReader(file))) {
        String line;
        while ((line = reader.readLine()) != null) {
          String[] parts = line.split(",");
          // Data order: 0,date 1, open; 2, high; 3, low; 4, close; 5, volume
          Object[] data = new Object[]{parts[0],
            Float.parseFloat(parts[1]),
            Float.parseFloat(parts[2]),
            Float.parseFloat(parts[3]),
            Float.parseFloat(parts[4]),
            Float.parseFloat(parts[5])};
          raw_data_map.put(parts[0], data);
        }
      } catch (Exception ex) {
        Logger.getLogger(TrainingFileGenerator.class.getName()).log(Level.SEVERE, "Error when loading csv file for raw data.", ex);
      }
    } else {
      Logger.getLogger(TrainingFileGenerator.class.getName()).log(Level.WARNING, "Data file for {0} does not exist.", code);
      return null;
    }
    return raw_data_map;
  }
}
