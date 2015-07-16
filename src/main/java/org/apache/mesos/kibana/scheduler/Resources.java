package org.apache.mesos.kibana.scheduler;

import org.apache.mesos.Protos;

/**
 * A helper class for managing Mesos Resources
 */
public class Resources {
    /**
     * Creates a cpu Resource with given amount of cpus
     *
     * @param cpus amount of cpus to assign
     * @return a cpu Resource with given amount of cpus
     */
    public static Protos.Resource cpus(double cpus) {
        return Protos.Resource.newBuilder()
                .setName("cpus")
                .setType(Protos.Value.Type.SCALAR)
                .setScalar(Protos.Value.Scalar.newBuilder().setValue(cpus))
                .build();
    }

    /**
     * Creates a mem Resource with given amount of memory
     *
     * @param memory amount of mem to assign
     * @return a mem Resource with given amount of memory
     */
    public static Protos.Resource mem(double memory) {
        return Protos.Resource.newBuilder()
                .setName("mem")
                .setType(Protos.Value.Type.SCALAR)
                .setScalar(Protos.Value.Scalar.newBuilder().setValue(memory))
                .build();
    }

    /**
     * Creates a port Resource with given port range
     *
     * @param begin the beginning of the port range
     * @param end   the end of the port range
     * @return a port Resource with given port range
     */
    public static Protos.Resource ports(long begin, long end) {
        Protos.Value.Range ports = Protos.Value.Range.newBuilder().setBegin(begin).setEnd(end).build();
        return Protos.Resource.newBuilder()
                .setName("ports")
                .setType(Protos.Value.Type.RANGES)
                .setRanges(Protos.Value.Ranges.newBuilder().addRange(ports))
                .build();
    }

    /**
     * Picks a port number from the given offer's resources' ports
     *
     * @param offer the offer from which's resources to pick a port
     * @return a port number
     */
    public static long pickPort(Protos.Offer offer) {
        for (Protos.Resource resource : offer.getResourcesList()) {
            if (resource.getName().equals("ports")) {
                return resource.getRanges().getRangeList().get(0).getBegin();
            }
        }
        return -1;
    }
}
