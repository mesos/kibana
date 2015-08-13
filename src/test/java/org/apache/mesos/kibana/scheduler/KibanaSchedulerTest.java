package org.apache.mesos.kibana.scheduler;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.SchedulerDriver;
import org.apache.mesos.kibana.TestUtils;
import org.junit.Test;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the KibanaScheduler class
 */
public class KibanaSchedulerTest {

    /***
     * Tests if the scheduler properly accepts good offers
     */
    @Test
    public void testOfferIsAcceptable_acceptsGoodOffers() {
        Offer goodOffer = TestUtils.getValidOffers(1).get(0);
        KibanaScheduler scheduler = new KibanaScheduler(mock(SchedulerConfiguration.class));

        boolean isAcceptable = scheduler.offerIsAcceptable(goodOffer);

        assertTrue(isAcceptable);
    }

    /***
     * Tests if the scheduler properly rejects bad offers
     */
    @Test
    public void testOfferIsAcceptable_rejectsBadOffers() {
        Offer badOffer = TestUtils.getInvalidOffers(1).get(0);
        KibanaScheduler scheduler = new KibanaScheduler(mock(SchedulerConfiguration.class));

        boolean isAcceptable = scheduler.offerIsAcceptable(badOffer);

        assertFalse(isAcceptable);
    }

    /**
     * Tests if a single task is started for one ElasticSearch when given enough offers
     */
    @Test
    public void testResourceOffers_startsSingleInstanceForSingleElasticSearch() throws Exception {
        String elasticSearch1 = "myElasticSearch1:9200";
        SchedulerConfiguration configuration = new SchedulerConfiguration();
        KibanaScheduler scheduler = new KibanaScheduler(configuration);
        SchedulerDriver driver = mock(SchedulerDriver.class);

        configuration.registerRequirement(elasticSearch1, 1);
        scheduler.resourceOffers(driver, TestUtils.getValidOffers(1));

        assertTrue(configuration.runningTasks.containsKey(elasticSearch1));
        assertEquals(1, configuration.runningTasks.get(elasticSearch1).size());
    }

    /**
     * Tests if multiple tasks are started for one ElasticSearch when given enough offers
     */
    @Test
    public void testResourceOffers_startsMultipleInstancesForSingleElasticSearch() throws Exception {
        String elasticSearch1 = "myElasticSearch1:9200";
        SchedulerConfiguration configuration = new SchedulerConfiguration();
        KibanaScheduler scheduler = new KibanaScheduler(configuration);
        SchedulerDriver driver = mock(SchedulerDriver.class);

        configuration.registerRequirement(elasticSearch1, 3);
        scheduler.resourceOffers(driver, TestUtils.getValidOffers(3));

        assertTrue(configuration.runningTasks.containsKey(elasticSearch1));
        assertEquals(3, configuration.runningTasks.get(elasticSearch1).size());
    }

    /**
     * Tests if a running task is stopped when given a negative requirement
     */
    @Test
    public void testResourceOffers_stopsSingleInstanceForSingleElasticSearch() throws Exception {
        String elasticSearch1 = "myElasticSearch1:9200";
        SchedulerConfiguration configuration = new SchedulerConfiguration();
        KibanaScheduler scheduler = new KibanaScheduler(configuration);
        SchedulerDriver driver = mock(SchedulerDriver.class);

        configuration.registerRequirement(elasticSearch1, 1);
        scheduler.resourceOffers(driver, TestUtils.getValidOffers(1));
        assertTrue(configuration.runningTasks.containsKey(elasticSearch1));
        assertEquals(1, configuration.runningTasks.get(elasticSearch1).size());

        configuration.registerRequirement(elasticSearch1, -1);
        scheduler.resourceOffers(driver, TestUtils.getValidOffers(0));

        assertFalse(configuration.runningTasks.containsKey(elasticSearch1));
        assertEquals(0, configuration.runningTasks.size());
    }

}