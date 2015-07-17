package org.apache.mesos.kibana;

import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.apache.mesos.kibana.scheduler.SchedulerConfiguration;
import org.apache.mesos.kibana.scheduler.KibanaScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KibanaFramework {
    private static final Logger logger = LoggerFactory.getLogger(KibanaFramework.class);

    /**
     * Outputs how the KibanaFramework should be called
     */
    private static void printUsage() {
        String name = KibanaFramework.class.getName();
        System.err.println("Usage: " + name + "ZookeeperAddress ElasticSearchUrls[]");
    }

    /**
     * KibanaFramework entry point
     *
     * @param args application launch arguments
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            logger.error("Not enough arguments were passed in, expected at least two.");
            System.exit(1);
        }

        SchedulerConfiguration config = new SchedulerConfiguration();
        config.parseLaunchArguments(args);

        Protos.FrameworkInfo framework = Protos.FrameworkInfo.newBuilder()
                .setId(Protos.FrameworkID.newBuilder().setValue("KibanaFramework"))
                .setName("KibanaFramework")
                .setUser("")
                .setCheckpoint(true)
                .setFailoverTimeout(10D) //in seconds
                .build();

        final Scheduler scheduler = new KibanaScheduler(config);
        MesosSchedulerDriver driver = new MesosSchedulerDriver(scheduler, framework, config.getZookeeperAddress());

        int status = driver.run() == Protos.Status.DRIVER_STOPPED ? 0 : 1;
        driver.stop();
        System.exit(status);
    }
}