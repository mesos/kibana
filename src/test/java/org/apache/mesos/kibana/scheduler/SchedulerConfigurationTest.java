package org.apache.mesos.kibana.scheduler;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.ParseException;
import org.apache.mesos.kibana.KibanaFramework;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Unit tests for the SchedulerConfiguration class
 */
public class SchedulerConfigurationTest {

    @Test
    /**
     * Simply tests if arguments are parsed and added to the SchedulerConfiguration nicely
     */
    public void handleArguments_parsesArguments() throws Exception {
        String hostName = "myHostName:myPort";
        String apiPort = "9001";
        String elasticSearch1 = "myElasticSearch1:9200";
        String elasticSearch2 = "myElasticSearch2:9200";
        String args = "-zk " + hostName + " -p " + apiPort + " -es " + elasticSearch1 + ";" + elasticSearch2 + ";" + elasticSearch2;
        SchedulerConfiguration config = new SchedulerConfiguration();
        config.parseLaunchArguments(args.split(" "));

        assertEquals(hostName, config.getZookeeperUrl());
        assertEquals(apiPort, config.getApiPort());
        assertEquals(2, config.requiredTasks.size());
        assertTrue(config.requiredTasks.containsKey(elasticSearch1));
        assertEquals(1, config.requiredTasks.get(elasticSearch1).intValue());
        assertTrue(config.requiredTasks.containsKey(elasticSearch2));
        assertEquals(2, config.requiredTasks.get(elasticSearch2).intValue());
    }

    @Test(expected=MissingArgumentException.class)
    /**
     * Simply tests if arguments are parsed and added to the SchedulerConfiguration nicely
     */
    public void handleArguments_zookeeperRequired() throws Exception {
        String apiPort = "9001";
        String elasticSearch = "myElasticSearch2:9200";
        String args = " -p " + apiPort + " -es " + elasticSearch;
        SchedulerConfiguration config = new SchedulerConfiguration();
        config.parseLaunchArguments(args.split(" "));
    }

    @Test
    /**
     * Tests if adding instance requiredTasks works properly
     */
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

    @Test
    /**
     * Tests if requesting enough negative instances removes the requirement entry
     */
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