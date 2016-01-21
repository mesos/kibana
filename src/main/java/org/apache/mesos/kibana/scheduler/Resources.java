package org.apache.mesos.kibana.scheduler;

import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.Value;
import java.util.List;

/**
 * A helper class for resources
 */
public class Resources {

    private Resources() {}

    /**
     * Creates a cpu Resource with given amount of cpus
     *
     * @param cpus amount of cpus to assign
     * @return a cpu Resource with given amount of cpus
     */
    public static Resource buildCpuResource(double cpus) {
        return Resource.newBuilder()
                .setName("cpus")
                .setType(Value.Type.SCALAR)
                .setScalar(Value.Scalar.newBuilder().setValue(cpus))
                .build();
    }

    /**
     * Creates a disk Resource with given amount of disk space
     * @param disk required amount of disk space in MB
     * @return a disk Resource with given amount of disk space
     */
    public static Resource buildDiskResource(double disk) {
        return Resource.newBuilder()
                .setName("disk")
                .setType(Value.Type.SCALAR)
                .setScalar(Value.Scalar.newBuilder().setValue(disk))
                .build();
    }

    /**
     * Creates a mem Resource with given amount of memory
     *
     * @param memory amount of mem to assign
     * @return a mem Resource with given amount of memory
     */
    public static Resource buildMemResource(double memory) {
        return Resource.newBuilder()
                .setName("mem")
                .setType(Value.Type.SCALAR)
                .setScalar(Value.Scalar.newBuilder().setValue(memory))
                .build();
    }

    /**
     * Creates a port Resource with given port range
     *
     * @param begin the beginning of the port range
     * @param end   the end of the port range
     * @return a port Resource with given port range
     */
    public static Resource buildPortResource(long begin, long end) {
        Value.Range ports = Value.Range.newBuilder().setBegin(begin).setEnd(end).build();
        return Resource.newBuilder()
                .setName("ports")
                .setType(Value.Type.RANGES)
                .setRanges(Value.Ranges.newBuilder().addRange(ports))
                .build();
    }

    /**
     * Counts the amount of ports offered in the given resource
     *
     * @param resource the resource to count the ports for
     * @return the number of ports offered in the resource
     */
    public static int getPortCount(Resource resource) {
        int portCount = 0;
        List<Value.Range> offeredRanges = resource.getRanges().getRangeList();
        for (Value.Range portRange : offeredRanges) {
            long begin = portRange.getBegin();
            long end = portRange.getEnd();
            portCount += end - begin + 1;
        }
        return portCount;
    }
}
