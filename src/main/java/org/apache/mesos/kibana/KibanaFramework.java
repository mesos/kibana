package org.apache.mesos.kibana;

import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.apache.mesos.kibana.scheduler.Configuration;
import org.apache.mesos.kibana.scheduler.KibanaScheduler;

public class KibanaFramework {

    private static void printUsage() {
        String name = KibanaFramework.class.getName();
        System.err.println("Usage: " + name + "master:port ElasticSearchUrls..");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        Configuration config = new Configuration();
        config.handleArguments(args);

        //TODO: Move these things to the config?
        Protos.FrameworkInfo framework = Protos.FrameworkInfo.newBuilder()
                .setId(Protos.FrameworkID.newBuilder().setValue("KibanaFramework"))
                .setName("KibanaFramework")
                .setUser("")
                .setCheckpoint(true)
                .setFailoverTimeout(10D) //in seconds
                .build();

        final Scheduler scheduler = new KibanaScheduler(config);
        MesosSchedulerDriver driver = new MesosSchedulerDriver(scheduler, framework, config.getMasterAddress());

        int status = driver.run() == Protos.Status.DRIVER_STOPPED ? 0 : 1;
        driver.stop();
        System.exit(status);
    }
}