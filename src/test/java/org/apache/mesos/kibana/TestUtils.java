package org.apache.mesos.kibana;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.kibana.scheduler.Resources;
import org.apache.mesos.kibana.scheduler.SchedulerConfiguration;
import java.util.ArrayList;
import java.util.List;

/**
 * Class containing static methods that make life easier
 */
public class TestUtils {

    /**
     * Creates given amount of offers with adequate resources
     *
     * @param amount amount of offers to create
     * @return given amount of offers with adequate resources
     */
    public static List<Offer> getValidOffers(int amount) {
        List<Offer> offers = new ArrayList<>();
        SchedulerConfiguration config = new SchedulerConfiguration();
        for (int i = 0; i < amount; i++) {
            Offer offer = Offer.newBuilder()
                    .setId(OfferID.newBuilder().setValue("valid-offer-" + i).build())
                    .setSlaveId(SlaveID.newBuilder().setValue("slave-" + i).build())
                    .setFrameworkId(FrameworkID.newBuilder().setValue("kibana").build())
                    .setHostname("localhost")
                    .addResources(Resources.buildCpuResource(config.getRequiredCpu()))
                    .addResources(Resources.buildMemResource(config.getRequiredMem()))
                    .addResources(Resources.buildDiskResource(config.getRequiredDisk()))
                    .addResources(Resources.buildPortResource(9000, (long) (8999 + SchedulerConfiguration.getRequiredPortCount())))
                    .build();
            offers.add(offer);
        }
        return offers;
    }

    /**
     * Creates given amount of offers with inadequate resources
     *
     * @param amount amount of offers to create
     * @return given amount of offers with inadequate resources
     */
    public static List<Offer> getInvalidOffers(int amount) {
        List<Offer> offers = new ArrayList<>();
        SchedulerConfiguration config = new SchedulerConfiguration();
        for (int i = 0; i < amount; i++) {
            Offer offer = Offer.newBuilder()
                    .setId(OfferID.newBuilder().setValue("invalid-offer-" + i).build())
                    .setSlaveId(SlaveID.newBuilder().setValue("slave-" + i).build())
                    .setFrameworkId(FrameworkID.newBuilder().setValue("kibana").build())
                    .setHostname("localhost")
                    .addResources(Resources.buildCpuResource(config.getRequiredCpu() / 2))
                    .addResources(Resources.buildMemResource(config.getRequiredMem() / 2))
                    .addResources(Resources.buildDiskResource(config.getRequiredDisk() / 2))
                    .build();
            offers.add(offer);
        }
        return offers;
    }
}
