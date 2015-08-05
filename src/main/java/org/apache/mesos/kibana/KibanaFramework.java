package org.apache.mesos.kibana;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.apache.mesos.kibana.scheduler.KibanaScheduler;
import org.apache.mesos.kibana.scheduler.SchedulerConfiguration;
import org.apache.mesos.kibana.web.KibanaFrameworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.io.PrintStream;
import java.util.HashMap;

public class KibanaFramework {
    private static final Logger LOGGER = LoggerFactory.getLogger(KibanaFramework.class);
    private static double ONE_DAY_IN_SECONDS = 86400D;

    /**
     * KibanaFramework entry point
     *
     * @param args application launch arguments
     */
    public static void main(String[] args) {

        final SchedulerConfiguration configuration = new SchedulerConfiguration();
        try {
            configuration.parseLaunchArguments(args); //DCOS-10 Configuration MUST be via CLI parameters or environment variables.
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("kibana", configuration.getOptions());
            System.exit(1);
        }

        Protos.FrameworkInfo.Builder framework = Protos.FrameworkInfo.newBuilder()
                .setId(Protos.FrameworkID.newBuilder().setValue("kibana"))//TODO: DCOS-02 Scheduler MUST persist their FrameworkID for failover.
                .setName("kibana")
                .setUser("")
                .setCheckpoint(true) //DCOS-04 Scheduler MUST enable checkpointing.
                .setFailoverTimeout(ONE_DAY_IN_SECONDS); //DCOS-01 Scheduler MUST register with a failover timeout.

        final Scheduler scheduler = new KibanaScheduler(configuration);
        final MesosSchedulerDriver schedulerDriver = new MesosSchedulerDriver(scheduler, framework.build(), configuration.getMaster());

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("server.port", configuration.getApiPort());
        new SpringApplicationBuilder(KibanaFrameworkService.class)
                .banner(getBanner())
                .properties(properties)
                .initializers(new ApplicationContextInitializer<ConfigurableApplicationContext>() {
                    @Override
                    public void initialize(ConfigurableApplicationContext context) {
                        context.getBeanFactory().registerSingleton("configuration", configuration);
                    }
                })
                .run();

        int status = schedulerDriver.run() == Protos.Status.DRIVER_STOPPED ? 0 : 1;
        schedulerDriver.stop();
        System.exit(status);
    }

    /**
     * Returns our cool custom banner
     *
     * @return our cool custom banner
     */
    private static Banner getBanner() {
        return new Banner() {
            @Override
            public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
                out.print(  "  _  __ _  _                          \n" +
                            " | |/ /(_)| |__    __ _  _ __    __ _ \n" +
                            " | ' / | || '_ \\  / _` || '_ \\  / _` |\n" +
                            " | . \\ | || |_) || (_| || | | || (_| |\n" +
                            " |_|\\_\\|_||_.__/  \\__,_||_| |_| \\__,_|\n");
            }
        };
    }
}