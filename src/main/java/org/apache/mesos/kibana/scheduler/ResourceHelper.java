package org.apache.mesos.kibana.scheduler;

import org.apache.mesos.Protos;

public class ResourceHelper {

    public static Protos.Resource cpus(double cpus) {
        return Protos.Resource.newBuilder()
                .setName("cpus")
                .setType(Protos.Value.Type.SCALAR)
                .setScalar(Protos.Value.Scalar.newBuilder().setValue(cpus))
                .build();
    }

    public static Protos.Resource mem(double memory) {
        return Protos.Resource.newBuilder()
                .setName("mem")
                .setType(Protos.Value.Type.SCALAR)
                .setScalar(Protos.Value.Scalar.newBuilder().setValue(memory))
                .build();
    }

    public static Protos.Resource ports(long begin, long end) {
        Protos.Value.Range ports = Protos.Value.Range.newBuilder().setBegin(begin).setEnd(end).build();
        return Protos.Resource.newBuilder()
                .setName("ports")
                .setType(Protos.Value.Type.RANGES)
                .setRanges(Protos.Value.Ranges.newBuilder().addRange(ports))
                .build();
    }
}
