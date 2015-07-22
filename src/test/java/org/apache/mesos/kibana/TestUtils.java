package org.apache.mesos.kibana;

import org.apache.mesos.Protos;
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
    public static List<Protos.Offer> getValidOffers(int amount) {
        List<Protos.Offer> offers = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            Protos.Offer offer = Protos.Offer.newBuilder()
                    .setId(Protos.OfferID.newBuilder().setValue("valid-offer-" + i).build())
                    .setSlaveId(Protos.SlaveID.newBuilder().setValue("slave-" + i).build())
                    .setFrameworkId(Protos.FrameworkID.newBuilder().setValue("KibanaFramework").build())
                    .setHostname("localhost")
                    .addResources(Resources.buildCpuResource(SchedulerConfiguration.getRequiredCpu()))
                    .addResources(Resources.buildMemResource(SchedulerConfiguration.getRequiredMem()))
                    .addResources(Resources.buildPortResource(9000, (long) (9000 + SchedulerConfiguration.getRequiredPortCount())))
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
    public static List<Protos.Offer> getInvalidOffers(int amount) {
        List<Protos.Offer> offers = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            Protos.Offer offer = Protos.Offer.newBuilder()
                    .setId(Protos.OfferID.newBuilder().setValue("invalid-offer-" + i).build())
                    .setSlaveId(Protos.SlaveID.newBuilder().setValue("slave-" + i).build())
                    .setFrameworkId(Protos.FrameworkID.newBuilder().setValue("KibanaFramework").build())
                    .setHostname("localhost")
                    .addResources(Resources.buildCpuResource(SchedulerConfiguration.getRequiredCpu() / 2))
                    .addResources(Resources.buildMemResource(SchedulerConfiguration.getRequiredMem() / 2))
                            //.addResources(Resources.ports(9000, 9000))
                    .build();
            offers.add(offer);
        }
        return offers;
    }
}
