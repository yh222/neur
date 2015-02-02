package core;

import datapreparer.CSVDownloader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * global configuration class
 */
public class GlobalConfigs {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    //Default start date for data download
    public static final String DEFAULT_START_DATE = "2000-01-01";

    // Global Value:  project path
    public static final String DEFAULT_PATH = "D:\\Documents\\NetBeansProjects\\ProjectSPA\\";
    public static final String RESOURCE_PATH = DEFAULT_PATH + "resources\\";
    
    public static final ArrayList<String> INSTRUMENT_CODES = loadInstrumentCodes(DEFAULT_PATH + "resources\\instrument_list.txt");
    public static final ArrayList<String> INDICE_CODES = loadInstrumentCodes(DEFAULT_PATH + "resources\\indice_list.txt");
    public static ConcurrentHashMap<String, String> REVELANT_INDICIES;
    public static final String IXIC = "^IXIC";
    public static final String GSPC = "^GSPC";
    public static final int[] WEEK_MULTIPIER = new int[]{1, 2, 3, 4, 7, 9, 12};

    private static ConcurrentHashMap<String, Double> NormalSigMap;

    public static final double getSignificanceNormal(String code) {
        return 0.04;
//        if (NormalSigMap == null) {
//            loadSignificanceMaps();
//        } else if (NormalSigMap.containsKey(code)) {
//            return NormalSigMap.get(code);
//        } else {
//            
//        }
    }

    private static ConcurrentHashMap<String, Double> DailySigMap;

    public static final double getSignificanceDaily(String code) {
        return 0.015;
//        if (DailySigMap == null) {
//            loadSignificanceMaps();
//        } else if (DailySigMap.containsKey(code)) {
//            return DailySigMap.get(code);
//        } else {
//
//        }
    }

//    private static void loadSignificanceMaps() {
//        ObjectInputStream inputStream = null;
//        try {
//            String fileName = DEFAULT_PATH + "resources\\NormalSigMap.bin";
//            inputStream = new ObjectInputStream(new FileInputStream(fileName));
//            NormalSigMap = (ConcurrentHashMap<String, Double>) inputStream.readObject();
//            fileName = DEFAULT_PATH + "resources\\DailySigMap.bin";
//            inputStream = new ObjectInputStream(new FileInputStream(fileName));
//            DailySigMap = (ConcurrentHashMap<String, Double>) inputStream.readObject();
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(GlobalConfigs.class.getName()).log(Level.SEVERE, null, ex);
//            NormalSigMap = new ConcurrentHashMap();
//            DailySigMap = new ConcurrentHashMap();
//        } catch (IOException | ClassNotFoundException ex) {
//            Logger.getLogger(GlobalConfigs.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            try {
//                inputStream.close();
//            } catch (IOException ex) {
//                Logger.getLogger(GlobalConfigs.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
    public static enum TRAINING_VALUES_NOMINAL {

        DAY_OF_WEEK,
        SEASON_YEAR,
        //
        CadRead_1d,
        CadRead_2d,
        CadRead_3d,
        CadRead_4d,
        CadRead_5d,
        CadRead_6d,
        CadRead_7d,
        CadRead_8d,
        CadRead_9d,
        CadRead_10d,
        CadRead_11d,
        CadRead_12d,
        CadRead_13d,
        CadRead_14d,
        CadRead_15d,
        CadRead_16d,
        CadRead_17d,
        CadRead_18d,
        CadRead_19d,
        CadRead_20d,
        CadRead_21d,
        CadRead_22d,
        CadRead_23d,
        CadRead_24d,
        CadRead_25d,
        CadRead_26d,
        CadRead_27d,
        CadRead_28d,
        CadRead_29d,
        CadRead_30d;

        public static final int SIZE = TRAINING_VALUES_NOMINAL.values().length;
    }

    public static enum TRAINING_VALUES_NUMERIC {

        RSI10,
        RSI20,
        RSI30,
        RSI50,
        RSI10_P10,
        RSI20_P20,
        //
        EMA5,
        EMA10,
        EMA20,
        EMA50,
        EMA100,
        EMA200,
        //
        SMA5,
        SMA10,
        SMA20,
        SMA50,
        SMA100,
        SMA200,
        //
        CLUTRENDHIGH_7d,
        CLUTRENDHIGH_14d,
        CLUTRENDHIGH_21d,
        CLUTRENDHIGH_28d,
        CLUTRENDHIGH_49d,
        CLUTRENDHIGH_63d,
        CLUTRENDHIGH_84d,
        //
        CLUTRENDLOW_7d,
        CLUTRENDLOW_14d,
        CLUTRENDLOW_21d,
        CLUTRENDLOW_28d,
        CLUTRENDLOW_49d,
        CLUTRENDLOW_63d,
        CLUTRENDLOW_84d,
        //
        RAWTREND_0d,
        RAWTREND_1d,
        RAWTREND_3d,
        RAWTREND_7d,
        RAWTREND_14d,
        RAWTREND_21d,
        RAWTREND_28d,
        RAWTREND_49d,
        RAWTREND_63d,
        RAWTREND_84d,
        VELOCITY_15d,
        VELOCITY_30d,
        //
        PASTLOW_7d,
        PASTLOW_14d,
        PASTLOW_21d,
        PASTLOW_28d,
        PASTLOW_49d,
        PASTLOW_63d,
        PASTLOW_84d,
        //
        PASTHIGH_7d,
        PASTHIGH_14d,
        PASTHIGH_21d,
        PASTHIGH_28d,
        PASTHIGH_49d,
        PASTHIGH_63d,
        PASTHIGH_84d,
        //
        DividendAmount,
        DaysTillNextDividend,
        //
        GreatW_7d,
        GreadB_7d,
        LongW_7d,
        LongB_7d,
        ShortW_7d,
        ShortB_7d,
        ShortWTT_7d,
        ShortBTT_7d,
        ShortWU_7d,
        ShortWD_7d,
        ShortBU_7d,
        ShortBD_7d,
        StarW_7d,
        StarB_7d,
        StarWTT_7d,
        StarBTT_7d,
        StarWU_7d,
        StarWD_7d,
        StarBU_7d,
        StarBD_7d,
        //
        GreatW_15d,
        GreadB_15d,
        LongW_15d,
        LongB_15d,
        ShortW_15d,
        ShortB_15d,
        ShortWTT_15d,
        ShortBTT_15d,
        ShortWU_15d,
        ShortWD_15d,
        ShortBU_15d,
        ShortBD_15d,
        StarW_15d,
        StarB_15d,
        StarWTT_15d,
        StarBTT_15d,
        StarWU_15d,
        StarWD_15d,
        StarBU_15d,
        StarBD_15d,
        //
        GreatW_30d,
        GreadB_30d,
        LongW_30d,
        LongB_30d,
        ShortW_30d,
        ShortB_30d,
        ShortWTT_30d,
        ShortBTT_30d,
        ShortWU_30d,
        ShortWD_30d,
        ShortBU_30d,
        ShortBD_30d,
        StarW_30d,
        StarB_30d,
        StarWTT_30d,
        StarBTT_30d,
        StarWU_30d,
        StarWD_30d,
        StarBU_30d,
        StarBD_30d,
        //
        GreatW_60d,
        GreadB_60d,
        LongW_60d,
        LongB_60d,
        ShortW_60d,
        ShortB_60d,
        ShortWTT_60d,
        ShortBTT_60d,
        ShortWU_60d,
        ShortWD_60d,
        ShortBU_60d,
        ShortBD_60d,
        StarW_60d,
        StarB_60d,
        StarWTT_60d,
        StarBTT_60d,
        StarWU_60d,
        StarWD_60d,
        StarBU_60d,
        StarBD_60d,
        //Indicie infulences
        //IXIC
        IXICCLUTRENDHIGH_7d,
        IXICCLUTRENDHIGH_14d,
        IXICCLUTRENDHIGH_21d,
        IXICCLUTRENDHIGH_28d,
        IXICCLUTRENDHIGH_49d,
        IXICCLUTRENDHIGH_63d,
        IXICCLUTRENDHIGH_84d,
        //
        IXICCLUTRENDLOW_7d,
        IXICCLUTRENDLOW_14d,
        IXICCLUTRENDLOW_21d,
        IXICCLUTRENDLOW_28d,
        IXICCLUTRENDLOW_49d,
        IXICCLUTRENDLOW_63d,
        IXICCLUTRENDLOW_84d,
        //NYI
        GSPCCLUTRENDHIGH_7d,
        GSPCCLUTRENDHIGH_14d,
        GSPCCLUTRENDHIGH_21d,
        GSPCCLUTRENDHIGH_28d,
        GSPCCLUTRENDHIGH_49d,
        GSPCCLUTRENDHIGH_63d,
        GSPCCLUTRENDHIGH_84d,
        //
        GSPCCLUTRENDLOW_7d,
        GSPCCLUTRENDLOW_14d,
        GSPCCLUTRENDLOW_21d,
        GSPCCLUTRENDLOW_28d,
        GSPCCLUTRENDLOW_49d,
        GSPCCLUTRENDLOW_63d,
        GSPCCLUTRENDLOW_84d,
        //Revelant
        REVCLUTRENDHIGH_7d,
        REVCLUTRENDHIGH_14d,
        REVCLUTRENDHIGH_21d,
        REVCLUTRENDHIGH_28d,
        REVCLUTRENDHIGH_49d,
        REVCLUTRENDHIGH_63d,
        REVCLUTRENDHIGH_84d,
        //
        REVCLUTRENDLOW_7d,
        REVCLUTRENDLOW_14d,
        REVCLUTRENDLOW_21d,
        REVCLUTRENDLOW_28d,
        REVCLUTRENDLOW_49d,
        REVCLUTRENDLOW_63d,
        REVCLUTRENDLOW_84d;
        ;
        //

        public static final int SIZE = TRAINING_VALUES_NUMERIC.values().length;
    }

    public static enum CLASS_VALUES {

        //
        FCTrdHigh7d,
        FCTrdHigh14d,
        FCTrdHigh21d,
        FCTrdHigh28d,
        FCTrdHigh49d,
        FCTrdHigh63d,
        FCTrdHigh84d,
        //
        FCTrdLow7d,
        FCTrdLow14d,
        FCTrdLow21d,
        FCTrdLow28d,
        FCTrdLow49d,
        FCTrdLow63d,
        FCTrdLow84d,
        //
        FHighest7d,
        FHighest14d,
        FHighest21d,
        FHighest28d,
        FHighest49d,
        FHighest63d,
        FHighest84d,
        //
        FLowest7d,
        FLowest14d,
        FLowest21d,
        FLowest28d,
        FLowest49d,
        FLowest63d,
        FLowest84d;
        //private int value;
        public static final int SIZE = CLASS_VALUES.values().length;

        public int appendedIndex() {
            return this.ordinal() + TRAINING_VALUES_NUMERIC.values().length;
        }
    }

    private static ArrayList<String> loadInstrumentCodes(String path) {
        if (REVELANT_INDICIES == null) {
            REVELANT_INDICIES = new ConcurrentHashMap();
        }

        ArrayList<String> codes = new ArrayList();
        int count = 0;
        try (BufferedReader reader = new BufferedReader(
                new FileReader(path))) {
            String line;
            String[] split;
            while ((line = reader.readLine()) != null) {
                count++;
                split = line.split(",");
                codes.add(split[0]);
                if (split.length > 1) {
                    REVELANT_INDICIES.put(split[0], split[1]);
                }
            }
        } catch (FileNotFoundException fe) {
            Logger.getLogger(CSVDownloader.class
                    .getName()).log(Level.SEVERE, path + " does not exist", fe);
        } catch (Exception e) {
            Logger.getLogger(CSVDownloader.class
                    .getName()).log(Level.SEVERE, " error when load " + path, e);
        }

        System.out.println(count + " codes loaded.");
        return codes;
    }
}
