package util;

import com.healthmarketscience.jackcess.*;
import core.GConfigs;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccessDB {

  static Database m_Database;

  static ArrayList<Tag> m_Tags = new ArrayList();

  //Subcat-> cat, multiple subcats to one cat
  static HashMap<String, String> m_SubToCat = new HashMap();

  //cat-> sector, multiple cats to one sector
  static HashMap<String, String> m_CatToSector = new HashMap();

  public static void InitDB() {
    if (m_Database == null) {
      try {
        m_Database = DatabaseBuilder.open(new File(GConfigs.RESOURCE_PATH + "\\STK\\Index.accdb"));
        Table tags = m_Database.getTable("Tags");
        Table country = m_Database.getTable("Country");
        Table industry = m_Database.getTable("Industry");
        Table industry_group = m_Database.getTable("IndustryGroup");
        Table sector = m_Database.getTable("Sector");

        for (Row row : tags) {
          String c = (String) queryTable(country, row.getInt("Country"), "Cname");
          String ind = (String) queryTable(industry, row.getInt("Industry1"), "Cname");
          String ind2 = "";
          if (row.get("Industry2") != null) {
            ind2 = (String) queryTable(industry, row.getInt("Industry2"), "Cname");
          }

          m_Tags.add(new Tag(row.getString("Tag"), c, ind, ind2,row.getInt("MarketCap")));
        }

        //Skip empty subcats
        m_SubToCat.put("", "");
        for (Row row : industry) {
          String grp = (String) queryTable(industry_group, row.getInt("ParentCate"), "Cname");
          m_SubToCat.put(row.getString("Cname"), grp);
        }

        m_CatToSector.put("", "");
        for (Row row : industry_group) {
          String sec = (String) queryTable(sector, row.getInt("ParentCate"), "Cname");
          m_CatToSector.put(row.getString("Cname"), sec);
        }

      } catch (IOException ex) {
        Logger.getLogger(AccessDB.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  private static Object queryTable(Table t, int index, String key) {
    for (Row row : t) {
      if (row.getInt("ID") == index) {
        return row.get(key);
      }
    }
    return "";
  }

  public static ArrayList<String> loadSTKCodes() {
    InitDB();
    ArrayList l = new ArrayList();
    for (Tag t : m_Tags) {
      l.add(t.m_Name);
    }
    return l;
  }

  public static ArrayList<String> queryTagsByIndustry(String ind) {
    ArrayList l = new ArrayList();
    for (Tag t : m_Tags) {
      if (t.m_Industry.equals(ind) || t.m_Industry2.equals(ind)) {
        l.add(t);
      }
    }
    return l;
  }

  public static ArrayList<String> queryTagsByIndustryGroup(String grp) {
    ArrayList l = new ArrayList();
    for (Tag t : m_Tags) {
      if (m_SubToCat.get(t.m_Industry).equals(grp)
              || m_SubToCat.get(t.m_Industry2).equals(grp)) {
        l.add(t);
      }
    }
    return l;
  }

  public static ArrayList<String> queryTagsBySector(String sec) {
    ArrayList l = new ArrayList();
    for (Tag t : m_Tags) {
      if (m_CatToSector.get(m_SubToCat.get(t.m_Industry)).equals(sec)
              || m_CatToSector.get(m_SubToCat.get(t.m_Industry2)).equals(sec)) {
        l.add(t);
      }
    }
    return l;
  }

  //Test
  public static void main(String[] args) {
    ArrayList<String> l = loadSTKCodes();
    l = queryTagsByIndustryGroup("Technology");
    l = queryTagsBySector("FINANCIALS");
    l = null;
  }

}
