package org.apache.mesos.kibana.scheduler;

import junit.framework.TestCase;
import org.apache.mesos.Protos;

/**
 * Tests for the Resources class
 */
public class ResourcesTest extends TestCase {
    public void testGetPortCount() throws Exception {
        Protos.Value.Ranges ranges = Protos.Value.Ranges.newBuilder()
                .addRange(Protos.Value.Range.newBuilder().setBegin(0).setEnd(4))        // 5
                .addRange(Protos.Value.Range.newBuilder().setBegin(11).setEnd(15))      //+5
                .addRange(Protos.Value.Range.newBuilder().setBegin(9800).setEnd(9800))  //+1
                .build();                                                               //=11
        Protos.Resource ports = Protos.Resource.newBuilder()
                .setType(Protos.Value.Type.RANGES)
                .setName("ports")
                .setRanges(ranges)
                .build();

        int portCount = Resources.getPortCount(ports);
        assertEquals(11, portCount);
    }
}