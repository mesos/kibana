package org.apache.mesos.kibana;

import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.apache.mesos.kibana.scheduler.KibanaScheduler;

public class KibanaFramework {

    private static void usage() {
        String name = KibanaFramework.class.getName();
        System.err.println("Usage: " + name + "[Master:Port] [ElasticSearchUrl]");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            usage();
            System.exit(1);
        }
        final String masterHost = args[0];
        final String elasticSearchUrl = args[1];

        Protos.FrameworkInfo framework = Protos.FrameworkInfo.newBuilder()
                .setId(Protos.FrameworkID.newBuilder().setValue("KibanaFramework"))
                .setName("KibanaFramework")
                .setUser("")
                .setCheckpoint(true)
                .setFailoverTimeout(10D) //in seconds
                .build();

        final Scheduler scheduler = new KibanaScheduler(elasticSearchUrl);
        MesosSchedulerDriver driver = new MesosSchedulerDriver(scheduler, framework, masterHost);

        int status = driver.run() == Protos.Status.DRIVER_STOPPED ? 0 : 1;
        driver.stop();
        System.exit(status);
    }
}