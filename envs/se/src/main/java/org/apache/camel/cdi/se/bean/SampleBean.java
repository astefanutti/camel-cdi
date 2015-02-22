package org.apache.camel.cdi.se.bean;

public class SampleBean {

    protected String label;

    public SampleBean(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SampleBean that = (SampleBean) o;

        if (!label.equals(that.label)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return label.hashCode();
    }
}
