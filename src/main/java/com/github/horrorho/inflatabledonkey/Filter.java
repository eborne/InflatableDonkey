package com.github.horrorho.inflatabledonkey;

/**
 * Created by jason on 5/5/2016.
 */
public class Filter {
    private String domain;
    private String path;

    public Filter(String domainValue, String pathValue)
    {
        domain = domainValue;
        path = pathValue;
    }

    public String getDomain() { return domain; }
    public void setDomain(String value) { domain = value; }
    public String getPath() { return path; }
    public void setPath(String value) { path = value; }
}
