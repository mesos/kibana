package org.apache.mesos.kibana.web;

/**
 * A simple DTO used to request the spin-up or killing off of Kibana tasks
 */
public class TaskRequest {
    private String elasticSearchUrl;
    private int delta;

    /**
     * Returns the TaskRequest's elasticSearchUrl
     *
     * @return the TaskRequest's elasticSearchUrl
     */
    public String getElasticSearchUrl() {
        return elasticSearchUrl;
    }

    /**
     * Sets the TaskRequest's elasticSearchUrl
     *
     * @param elasticSearchUrl the TaskRequest's elasticSearchUrl
     */
    public void setElasticSearchUrl(String elasticSearchUrl) {
        this.elasticSearchUrl = elasticSearchUrl;
    }

    /**
     * Returns the TaskRequest's delta
     *
     * @return the TaskRequest's delta
     */
    public int getDelta() {
        return delta;
    }

    /**
     * Sets the TaskRequest's delta
     *
     * @param delta the TaskRequest's delta
     */
    public void setDelta(int delta) {
        this.delta = delta;
    }
}
