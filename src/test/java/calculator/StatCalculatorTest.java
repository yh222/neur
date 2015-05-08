package calculator;

import static calculator.StatCalculator.*;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class StatCalculatorTest {

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    ConcurrentHashMap<String, Object[]> raw_data_map;

    @Before
    public void setUp() {
        raw_data_map = new ConcurrentHashMap();

        //System.out.println("start: " + date.getTime());
        //open high low close
        raw_data_map.put("2005-12-16", new Object[]{100.0, 100.0, 98.0, 102.0, 54567500.0});
        raw_data_map.put("2005-12-19", new Object[]{112.16, 113.49, 111.97, 112.94, 45167500.0});
        raw_data_map.put("2005-12-20", new Object[]{108.29, 108.65, 105.41, 106.25, 64285500.0});
        raw_data_map.put("2005-12-21", new Object[]{110.70, 111.60, 106.35, 108.23, 67218100.0});
        raw_data_map.put("2005-12-22", new Object[]{113.56, 117.12, 112.25, 112.62, 54119000.0});
        raw_data_map.put("2005-12-23", new Object[]{117.38, 118.62, 112.00, 116.56, 53228400.0});

        raw_data_map.put("2005-12-27", new Object[]{107.03, 107.58, 105.20, 105.99, 78513300.0});
        raw_data_map.put("2005-12-28", new Object[]{115.99, 126.08, 114.64, 125.00, 38318900.0});
        raw_data_map.put("2005-12-29", new Object[]{108.60, 108.79, 107.80, 108.70, 34968500.0});
        raw_data_map.put("2005-12-30", new Object[]{116.85, 118.77, 116.62, 118.63, 47450800.0});
        raw_data_map.put("2006-01-03", new Object[]{106.37, 110.16, 106.26, 106.75, 60790700.0});
        raw_data_map.put("2006-01-04", new Object[]{106.37, 110.16, 96.26, 96.75, 60790700.0});
    }

    @After
    public void tearDown() {
    }

//    @Test void testgetUsableDate(){
//        getUsableDate("2005-12-23", raw_data_map, 3, 3, true);
//        
//        
//    }
    @Test
    public void testCalculateRawTrend() {
        //Test today
        Object out = getMomentum("2005-12-23", raw_data_map, 0, 0);
        System.out.println(out);
        assertTrue((double) out == (116.56 - 117.38) / 117.38);//-0.006985857897427102
        //Test yesterday
        out = getMomentum("2005-12-23", raw_data_map, 1, 0);
        System.out.println(out);
        assertTrue((double) out == (112.62 - 113.56) / 113.56); //-0.0082775625220148
        //Test for past 3 days
        out = getMomentum("2005-12-23", raw_data_map, 3, 3);
        System.out.println(out);
        assertTrue((double) out == (116.56 - 108.29) / 108.29); //0.0542691751085384

        //Test for future 7 days
        out = getMomentum("2005-12-21", raw_data_map, 0, 6);
        System.out.println(out);
        assertTrue((double) out == (105.99 - 110.70) / 110.70); //0.0542691751085384
    }

    @Test
    public void testCalcluateVelocity() {
        //Test 3 day velocity
        Object out = getVelocity("2005-12-27", raw_data_map, 3);
        System.out.println(out);
        assertTrue((double) out == 78513300.0 / ((67218100.0 + 53228400.0 + 54119000.0) / 3));//1.349292385952551

        out = getVelocity("2006-01-03", raw_data_map, 8);
        System.out.println(out);
        assertTrue((double) out == 60790700.0 / ((64285500.0 + 67218100.0 + 53228400.0
                + 54119000.0 + 47450800.0 + 34968500.0 + 38318900.0 + 78513300.0) / 8));//1.349292385952551
    }

    @Test
    public void testCalcluateSituation() {
        Object out = getNominalRawTrend("MSFT", "2005-12-21", raw_data_map, 0, 2);
        System.out.println(out);
        assertTrue(out.equals("Little_High"));

        out = getNominalRawTrend("MSFT", "2005-12-20", raw_data_map, 0, 8);
        System.out.println(out);
        assertTrue(out.equals("Very_High"));

        out = getNominalRawTrend("MSFT", "2005-12-19", raw_data_map, 0, 8);
        System.out.println(out);
        assertTrue(out.equals("Little_Low"));

        out = getNominalRawTrend("MSFT", "2005-12-28", raw_data_map, 0, 7);
        System.out.println(out);
        assertTrue(out.equals("Very_Low"));

        out = getNominalRawTrend("MSFT", "2005-12-22", raw_data_map, 0, 0);
        System.out.println(out);
        assertTrue(out.equals("Stay"));
    }

    @Test
    public void testCalculateNominalExtreme() {
        Object out = Extreme.getNominalExtreme("MSFT", "2005-12-21", raw_data_map, 0, 7, true);
        System.out.println(out);
        assertTrue(out.equals("Very_High"));

        out = Extreme.getNominalExtreme("MSFT", "2005-12-21", raw_data_map, 0, 8, false);
        System.out.println(out);
        assertTrue(out.equals("Little_Low"));

        out = Extreme.getNominalExtreme("MSFT", "2006-01-04", raw_data_map, 6, 6, true);
        System.out.println(out);
        assertTrue(out.equals("High"));
    }

    @Test
    public void testCalculateNominalCluTrend() {
        Object out = getNominalCluTrend("MSFT", "2005-12-21", raw_data_map, 0, 7, true);
        System.out.println(out);
        assertTrue(out.equals("Very_High"));

        out = getNominalCluTrend("MSFT", "2005-12-19", raw_data_map, 0, 7, true);
        System.out.println(out);
        assertTrue(out.equals("Very_High"));

    }

    @Test
    public void testCalculateCandleUnitForDay() {
        String out = CandleStick.getCandleUnitForDay("MSFT", "2005-12-20", raw_data_map, 1);
        System.out.println(out);
        assertTrue(out.equals("StarW"));
        
        out = CandleStick.getCandleUnitForDay("MSFT", "2005-12-19", raw_data_map, 1);
        System.out.println(out);
        assertTrue(out.equals("ShortWD"));
    }

}
