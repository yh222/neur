package datapreparer;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import datapreparer.TrainingDataGenerator;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import core.GlobalConfigs.TRAINIG_TYPES;
import static core.GlobalConfigs.TRAINING_TYPES_SIZE;

/**
 *
 * @author Yichen
 */
public class TrainingDataGeneratorTest {

//    public TrainingDataGeneratorTest() {
//    }
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    @Test
    public void addRawTrend() {
        try {
            TrainingDataGenerator tdg = new TrainingDataGenerator();

            Method method = TrainingDataGenerator.class.getDeclaredMethod("addRawTrend", String.class, ConcurrentHashMap.class, double[].class, int.class, int.class, int.class);
            method.setAccessible(true);
            ConcurrentHashMap<String, double[]> raw_data_map = new ConcurrentHashMap();
            double[] storage_row = new double[TRAINING_TYPES_SIZE];
            
            //System.out.println("start: " + date.getTime());
            raw_data_map.put("2005-12-21", new double[]{110.56, 111.12, 112.10, 112.0, 53549000});
            raw_data_map.put("2005-12-22", new double[]{113.56, 117.12, 112.25, 112.62, 54119000});
            raw_data_map.put("2005-12-23", new double[]{117.38, 118.62, 112.00, 116.56, 53228400});

            method.invoke(tdg, "2005-12-23", raw_data_map, storage_row, 0, 0, TRAINIG_TYPES.RAWTREND_0d.index());
            System.out.println(storage_row[TRAINIG_TYPES.RAWTREND_0d.index()]);
            assertTrue(storage_row[TRAINIG_TYPES.RAWTREND_0d.index()] == (116.56 - 117.38) / 117.38);
            method.invoke(tdg, "2005-12-23", raw_data_map, storage_row, 1, 0, TRAINIG_TYPES.RAWTREND_1d.index());
            System.out.println(storage_row[TRAINIG_TYPES.RAWTREND_1d.index()]);
            assertTrue(storage_row[TRAINIG_TYPES.RAWTREND_1d.index()] == (112.62 - 113.56) / 113.56); //-0.0082775625220148
            method.invoke(tdg, "2005-12-23", raw_data_map, storage_row, 3, 3, TRAINIG_TYPES.RAWTREND_3d.index());
            System.out.println(storage_row[TRAINIG_TYPES.RAWTREND_3d.index()]);
            assertTrue(storage_row[TRAINIG_TYPES.RAWTREND_3d.index()] == (116.56 - 110.56) / 110.56); //0.0542691751085384

        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(TrainingDataGeneratorTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
