package org.apache.mesos.kibana.web;

public class Request {
    private String elasticSearchUrl;
    private int delta;

    public Request(){

    }

    public Request(String elasticSearchUrl, int delta) {
        this.elasticSearchUrl = elasticSearchUrl;
        this.delta = delta;
    }

    public String getElasticSearchUrl() {
        return elasticSearchUrl;
    }

    public void setElasticSearchUrl(String elasticSearchUrl) {
        this.elasticSearchUrl = elasticSearchUrl;
    }

    public int getDelta() {
        return delta;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }
}
