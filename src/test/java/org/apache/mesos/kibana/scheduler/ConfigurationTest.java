package org.apache.mesos.kibana.scheduler;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigurationTest {

    @Test
    public void handleArguments_parsesArguments() {
        String hostName = "myHostName:myPort";
        String elasticSearch1 = "myElasticSearch1:9200";
        String elasticSearch2 = "myElasticSearch2:9200";
        String args = hostName + " " + elasticSearch1 + " " + elasticSearch2 + " " + elasticSearch2;
        Configuration config = new Configuration();

        config.handleArguments(args.split(" "));

        assertEquals(hostName, config.getMasterAddress());
        assertEquals(2, config.requiredInstances.size());
        assertTrue(config.requiredInstances.containsKey(elasticSearch1));
        assertEquals(1, config.requiredInstances.get(elasticSearch1).intValue());
        assertTrue(config.requiredInstances.containsKey(elasticSearch2));
        assertEquals(2, config.requiredInstances.get(elasticSearch2).intValue());
    }

    @Test
    public void putRequiredInstances_addsRequirements() {
        String elasticSearch1 = "myElasticSearch1:9200";
        String elasticSearch2 = "myElasticSearch2:9200";
        Configuration config = new Configuration();

        config.putRequiredInstances(elasticSearch1, 1);
        assertEquals(1, config.requiredInstances.size());
        assertTrue(config.requiredInstances.containsKey(elasticSearch1));
        assertEquals(1, config.requiredInstances.get(elasticSearch1).intValue());

        config.putRequiredInstances(elasticSearch1, 2);
        assertEquals(1, config.requiredInstances.size());
        assertTrue(config.requiredInstances.containsKey(elasticSearch1));
        assertEquals(3, config.requiredInstances.get(elasticSearch1).intValue());

        config.putRequiredInstances(elasticSearch2, 1);
        assertEquals(2, config.requiredInstances.size());
        assertTrue(config.requiredInstances.containsKey(elasticSearch2));
        assertEquals(1, config.requiredInstances.get(elasticSearch2).intValue());
    }

    @Test
    public void putRequiredInstances_removesRequirements() {
        String elasticSearch1 = "myElasticSearch1:9200";
        Configuration config = new Configuration();

        config.putRequiredInstances(elasticSearch1, 1);
        assertEquals(1, config.requiredInstances.size());
        assertTrue(config.requiredInstances.containsKey(elasticSearch1));
        assertEquals(1, config.requiredInstances.get(elasticSearch1).intValue());

        config.putRequiredInstances(elasticSearch1, -1);
        assertEquals(0, config.requiredInstances.size());
        assertFalse(config.requiredInstances.containsKey(elasticSearch1));
    }
}