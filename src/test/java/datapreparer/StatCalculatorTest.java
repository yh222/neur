package datapreparer;

import static datapreparer.StatCalculator.CalcluateSituation;
import static datapreparer.StatCalculator.CalculateRawTrend;
import static datapreparer.StatCalculator.CalcluateVelocity;

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
        raw_data_map.put("2005-12-21", new Object[]{110.56, 111.12, 112.10, 112.0, 53549000.0});
        raw_data_map.put("2005-12-22", new Object[]{113.56, 117.12, 112.25, 112.62, 54119000.0});
        raw_data_map.put("2005-12-23", new Object[]{117.38, 118.62, 112.00, 116.56, 53228400.0});
        raw_data_map.put("2005-12-24", new Object[]{111.38, 118.62, 112.00, 112.47, 55528400.0});
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCalculateRawTrend() {
        //Test today
        Object out = CalculateRawTrend("2005-12-23", raw_data_map, 0, 0);
        System.out.println(out);
        assertTrue((double) out == (116.56 - 117.38) / 117.38);
        //Test yesterday
        out = CalculateRawTrend("2005-12-23", raw_data_map, 1, 0);
        System.out.println(out);
        assertTrue((double) out == (112.62 - 113.56) / 113.56); //-0.0082775625220148
        //Test for past 3 days
        out = CalculateRawTrend("2005-12-23", raw_data_map, 3, 3);
        System.out.println(out);
        assertTrue((double) out == (116.56 - 110.56) / 110.56); //0.0542691751085384

        //Test for future 3 days
        out = CalculateRawTrend("2005-12-21", raw_data_map, 0, 2);
        System.out.println(out);
        assertTrue((double) out == (116.56 - 110.56) / 110.56); //0.0542691751085384
    }

    @Test
    public void testCalcluateVelocity() {
        //Test 3 day velocity
        Object out = CalcluateVelocity("2005-12-24", raw_data_map, 3);
        System.out.println(out);
        assertTrue((double) out == 55528400.0 / ((53549000.0 + 54119000.0 + 53228400.0) / 3));//1.035356912895503
    }

    @Test
    public void testCalcluateSituation() {
        Object out = CalcluateSituation("2005-12-21", raw_data_map, 0, 2, 0.03);
        System.out.println(out);
        assertTrue( out.equals("Rise"));
    }
}
