package org.apache.mesos.kibana;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Protos.Credential;
import org.apache.mesos.Scheduler;
import org.apache.mesos.kibana.scheduler.KibanaScheduler;
import org.apache.mesos.kibana.scheduler.SchedulerConfiguration;
import org.apache.mesos.kibana.scheduler.State;
import org.apache.mesos.kibana.web.KibanaFrameworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import com.google.protobuf.ByteString;

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
        LOGGER.info("Entering KibanaFramework main().");

        LOGGER.info("Setting up the scheduler configuration.");
        final SchedulerConfiguration configuration = new SchedulerConfiguration();
        try {
            configuration.parseLaunchArguments(args); //DCOS-10 Configuration MUST be via CLI parameters or environment variables.
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(configuration.getFrameworkName(), configuration.getOptions());
            System.exit(1);
        }

        LOGGER.info("Setting up the Framework.");
        FrameworkInfo.Builder framework = FrameworkInfo.newBuilder()
                .setName(configuration.getFrameworkName())
                .setUser("")
                .setCheckpoint(true) //DCOS-04 Scheduler MUST enable checkpointing.
                .setFailoverTimeout(ONE_DAY_IN_SECONDS); //DCOS-01 Scheduler MUST register with a failover timeout.

        LOGGER.info("Setting up the State.");
        State state = new State(configuration.getZookeeper());
        configuration.setState(state);
        FrameworkID frameworkId = state.getFrameworkId();
        if(frameworkId != null){
            framework.setId(frameworkId); //DCOS-02 Scheduler MUST persist their FrameworkID for failover.
        }

        final Scheduler scheduler;
        final MesosSchedulerDriver schedulerDriver;

        if (configuration.getPrincipal() != null) {
            LOGGER.info("Setting up mesos authentication.");
            Credential.Builder credentialBuilder = Credential.newBuilder();
            framework.setPrincipal(configuration.getPrincipal());
            credentialBuilder.setPrincipal(configuration.getPrincipal());
            credentialBuilder.setSecret(ByteString.copyFromUtf8(configuration.getSecret()));

            LOGGER.info("Setting up the Scheduler");
            scheduler = new KibanaScheduler(configuration);
            schedulerDriver = new MesosSchedulerDriver(scheduler, framework.build(), configuration.getZookeeper(),credentialBuilder.build());
        }
        else {
            LOGGER.info("Setting up the Scheduler");
            scheduler = new KibanaScheduler(configuration);
            schedulerDriver = new MesosSchedulerDriver(scheduler, framework.build(), configuration.getZookeeper());
        }


        LOGGER.info("Setting up the Spring Web API");
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

        int status = schedulerDriver.run() == Status.DRIVER_STOPPED ? 0 : 1;
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