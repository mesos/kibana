package org.apache.mesos.kibana.scheduler;

import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the KibanaScheduler class
 */
public class KibanaSchedulerTest {

    /**
     * Creates given amount of offers that fulfill Kibana's requiredTasks
     * TODO Move to a TestUtils class or something...
     *
     * @param amount amount of offers to generate
     * @return a list of valid offers
     */
    private static List<Protos.Offer> createValidOffers(int amount) {
        List<Protos.Offer> offers = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            Protos.Offer offer = Protos.Offer.newBuilder()
                    .setId(Protos.OfferID.newBuilder().setValue("offer-" + i).build())
                    .setSlaveId(Protos.SlaveID.newBuilder().setValue("slave-" + i).build())
                    .setFrameworkId(Protos.FrameworkID.newBuilder().setValue("KibanaFramework").build())
                    .setHostname("localhost")
                    .addResources(Resources.cpus(SchedulerConfiguration.getRequiredCpus()))
                    .addResources(Resources.mem(SchedulerConfiguration.getRequiredMem()))
                    .addResources(Resources.ports(5601L, (long) (5600L + SchedulerConfiguration.getRequiredPortCount())))
                    .build();
            offers.add(offer);
        }
        return offers;
    }

    @Test
    /**
     * Tests if a single task is started for one ElasticSearch when given enough offers
     */
    public void testResourceOffers_startsSingleInstanceForSingleElasticSearch() throws Exception {
        String elasticSearch1 = "myElasticSearch1:9200";
        SchedulerConfiguration configuration = new SchedulerConfiguration();
        KibanaScheduler scheduler = new KibanaScheduler(configuration);
        SchedulerDriver driver = mock(SchedulerDriver.class);

        configuration.registerRequirement(elasticSearch1, 1);

        scheduler.resourceOffers(driver, createValidOffers(1));

        assertTrue(configuration.runningTasks.containsKey(elasticSearch1));
        assertEquals(1, configuration.runningTasks.get(elasticSearch1).size());
    }

    @Test
    /**
     * Tests if multiple tasks are started for one ElasticSearch when given enough offers
     */
    public void testResourceOffers_startsMultipleInstancesForSingleElasticSearch() throws Exception {
        String elasticSearch1 = "myElasticSearch1:9200";
        SchedulerConfiguration configuration = new SchedulerConfiguration();
        KibanaScheduler scheduler = new KibanaScheduler(configuration);
        SchedulerDriver driver = mock(SchedulerDriver.class);

        configuration.registerRequirement(elasticSearch1, 3);

        scheduler.resourceOffers(driver, createValidOffers(3));

        assertTrue(configuration.runningTasks.containsKey(elasticSearch1));
        assertEquals(3, configuration.runningTasks.get(elasticSearch1).size());
    }

}