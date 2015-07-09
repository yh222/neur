package util;

public class Tag {

  public String m_Name;
  public String m_Country;
  public String m_Industry;
  public String m_Industry2;
  double m_MktCap;

  Tag(String name, String country, String industry, String industry2, double mktCap) {
    m_Name = name;
    m_Country = country;
    m_Industry = industry;
    m_Industry2 = industry2;
    m_MktCap = mktCap;
  }
}
