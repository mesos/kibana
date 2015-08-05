package org.apache.mesos.kibana.scheduler;

import org.apache.commons.cli.MissingArgumentException;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Unit tests for the SchedulerConfiguration class
 */
public class SchedulerConfigurationTest {

    /**
     * Tests if launch arguments are parsed and added to the SchedulerConfiguration
     */
    @Test
    public void handleArguments_parsesArguments() throws Exception {
        String zookeeper = "zk://myZookeeper:9000/myPath";
        String apiPort = "9001";
        String elasticSearch1 = "myElasticSearch1:9200";
        String elasticSearch2 = "myElasticSearch2:9200";
        String args = "-zk " + zookeeper + " -p " + apiPort + " -es " + elasticSearch1 + ";" + elasticSearch2 + ";" + elasticSearch2;
        SchedulerConfiguration config = new SchedulerConfiguration();

        config.parseLaunchArguments(args.split(" "));

        assertEquals(zookeeper, config.getZookeeper());
        assertEquals(apiPort, config.getApiPort());
        assertEquals(2, config.requiredTasks.size());
        assertTrue(config.requiredTasks.containsKey(elasticSearch1));
        assertEquals(1, config.requiredTasks.get(elasticSearch1).intValue());
        assertTrue(config.requiredTasks.containsKey(elasticSearch2));
        assertEquals(2, config.requiredTasks.get(elasticSearch2).intValue());
    }

    /**
     * Tests if not passing in a zookeeper url causes parseLaunchArguments to fail
     */
    @Test(expected = MissingArgumentException.class)
    public void handleArguments_zookeeperRequired() throws Exception {
        String apiPort = "9001";
        String elasticSearch = "myElasticSearch2:9200";
        String args = " -p " + apiPort + " -es " + elasticSearch;
        SchedulerConfiguration config = new SchedulerConfiguration();
        config.parseLaunchArguments(args.split(" "));
    }

    /**
     * Tests if adding required instances works properly
     */
    @Test
    public void putRequiredInstances_addsRequirements() throws Exception {
        String elasticSearch1 = "myElasticSearch1:9200";
        String elasticSearch2 = "myElasticSearch2:9200";
        SchedulerConfiguration config = new SchedulerConfiguration();

        config.registerRequirement(elasticSearch1, 1);
        assertEquals(1, config.requiredTasks.size());
        assertTrue(config.requiredTasks.containsKey(elasticSearch1));
        assertEquals(1, config.requiredTasks.get(elasticSearch1).intValue());

        config.registerRequirement(elasticSearch1, 2);
        assertEquals(1, config.requiredTasks.size());
        assertTrue(config.requiredTasks.containsKey(elasticSearch1));
        assertEquals(3, config.requiredTasks.get(elasticSearch1).intValue());

        config.registerRequirement(elasticSearch2, 1);
        assertEquals(2, config.requiredTasks.size());
        assertTrue(config.requiredTasks.containsKey(elasticSearch2));
        assertEquals(1, config.requiredTasks.get(elasticSearch2).intValue());
    }

    /**
     * Tests if requesting enough negative instances removes the requirement entry
     */
    @Test
    public void putRequiredInstances_removesRequirements() throws Exception {
        String elasticSearch1 = "myElasticSearch1:9200";
        SchedulerConfiguration config = new SchedulerConfiguration();

        config.registerRequirement(elasticSearch1, 1);
        assertEquals(1, config.requiredTasks.size());
        assertTrue(config.requiredTasks.containsKey(elasticSearch1));
        assertEquals(1, config.requiredTasks.get(elasticSearch1).intValue());

        config.registerRequirement(elasticSearch1, -1);
        assertEquals(0, config.requiredTasks.size());
        assertFalse(config.requiredTasks.containsKey(elasticSearch1));
    }
}