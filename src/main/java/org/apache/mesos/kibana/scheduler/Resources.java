package org.apache.mesos.kibana.scheduler;

import org.apache.mesos.Protos;

import java.util.List;

/**
 * A helper class for resources
 */
public class Resources {

    /**
     * Creates a cpu Resource with given amount of cpus
     *
     * @param cpus amount of cpus to assign
     * @return a cpu Resource with given amount of cpus
     */
    public static Protos.Resource buildCpuResource(double cpus) {
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
    public static Protos.Resource buildMemResource(double memory) {
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
    public static Protos.Resource buildPortResource(long begin, long end) {
        Protos.Value.Range ports = Protos.Value.Range.newBuilder().setBegin(begin).setEnd(end).build();
        return Protos.Resource.newBuilder()
                .setName("ports")
                .setType(Protos.Value.Type.RANGES)
                .setRanges(Protos.Value.Ranges.newBuilder().addRange(ports))
                .build();
    }

    /**
     * Counts the amount of ports offered in the given resource
     *
     * @param resource the resource to count the ports for
     * @return the number of ports offered in the resource
     */
    public static int getPortCount(Protos.Resource resource) {
        int portCount = 0;
        List<Protos.Value.Range> offeredRanges = resource.getRanges().getRangeList();
        for (Protos.Value.Range portRange : offeredRanges) {
            long begin = portRange.getBegin();
            long end = portRange.getEnd();
            portCount += end - begin + 1;
        }
        return portCount;
    }
}
