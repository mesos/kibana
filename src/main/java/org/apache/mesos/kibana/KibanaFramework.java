package org.apache.mesos.kibana;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.apache.mesos.kibana.scheduler.KibanaScheduler;
import org.apache.mesos.kibana.scheduler.SchedulerConfiguration;
import org.apache.mesos.kibana.web.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;

public class KibanaFramework {
    private static final Logger logger = LoggerFactory.getLogger(KibanaFramework.class);

    /**
     * KibanaFramework entry point
     *
     * @param args application launch arguments
     */
    public static void main(String[] args) {

        final SchedulerConfiguration configuration = new SchedulerConfiguration();
        try {
            configuration.parseLaunchArguments(args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("KibanaFramework", configuration.getOptions());
            System.exit(1);
        }

        Protos.FrameworkInfo framework = Protos.FrameworkInfo.newBuilder()
                .setId(Protos.FrameworkID.newBuilder().setValue("KibanaFramework"))
                .setName("KibanaFramework")
                .setUser("")
                .setCheckpoint(true)
                .setFailoverTimeout(10D) //in seconds
                .build();

        final Scheduler scheduler = new KibanaScheduler(configuration);
        final MesosSchedulerDriver schedulerDriver = new MesosSchedulerDriver(scheduler, framework, configuration.getZookeeperUrl());

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("server.port", configuration.getApiPort());
        new SpringApplicationBuilder(Application.class)
                .properties(properties)
                .initializers(new ApplicationContextInitializer<ConfigurableApplicationContext>() {
                    @Override
                    public void initialize(ConfigurableApplicationContext context) {
                        context.getBeanFactory().registerSingleton("configuration", configuration);
                    }
                })
                .initializers(new ApplicationContextInitializer<ConfigurableApplicationContext>() {
                    @Override
                    public void initialize(ConfigurableApplicationContext context) {
                        context.getBeanFactory().registerSingleton("schedulerDriver", schedulerDriver);
                    }
                })
                .run();
        
        int status = schedulerDriver.run() == Protos.Status.DRIVER_STOPPED ? 0 : 1;
        schedulerDriver.stop();
        System.exit(status);
    }
}