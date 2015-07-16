package org.apache.mesos.kibana.scheduler;

import org.apache.mesos.Protos.*;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class KibanaScheduler implements Scheduler {

    private static final Logger logger = LoggerFactory.getLogger(KibanaScheduler.class);

    private final AtomicInteger taskIDGenerator = new AtomicInteger();
    private Configuration configuration;

    public KibanaScheduler(Configuration configuration) {
        logger.info("constructing " + KibanaScheduler.class.getName());
        this.configuration = configuration;
    }


    private void launchTask(String elasticSearchUrl, Offer offer, SchedulerDriver driver) {
        TaskID taskId = generateTaskId();
        long port = ResourceHelper.pickPort(offer);
        ContainerInfo.Builder containerInfo = buildContainerInfo(port);
        Environment environment = buildEnvironment(elasticSearchUrl);
        CommandInfo.Builder commandInfoBuilder = buildCommandInfo(environment);
        TaskInfo task = buildTask(taskId, offer, containerInfo, commandInfoBuilder, port);
        Filters filters = Filters.newBuilder().setRefuseSeconds(1).build();
        logger.info("Launching task {}", taskId.getValue());
        configuration.putRunningInstances(elasticSearchUrl, taskId);
        driver.launchTasks(Arrays.asList(offer.getId()), Arrays.asList(task), filters);
    }

    private boolean offerIsAcceptable(Offer offer) {
        boolean hasCpus = false;
        double offerCpus = 0;
        boolean hasMem = false;
        double offerMem = 0;
        boolean hasPorts = false;
        int offerPorts = 0;

        for (Resource resource : offer.getResourcesList()) {
            switch (resource.getName()) {
                case "cpus":
                    offerCpus = resource.getScalar().getValue();
                    hasCpus = true;
                    break;
                case "mem":
                    offerMem = resource.getScalar().getValue();
                    hasMem = true;
                    break;
                case "ports":
                    offerPorts = resource.getRanges().getRangeCount();
                    hasPorts = true;
            }
        }
        if (!hasCpus) {
            logger.info("Rejecting offer {} due to lack of cpus (required {}, got {})", offer.getId().getValue(), Configuration.getCPU(), offerCpus);
            return false;
        }
        if (!hasMem) {
            logger.info("Rejecting offer {} due to lack of mem (required {}, got {})", offer.getId().getValue(), Configuration.getMEM(), offerMem);
            return false;
        }
        if (!hasPorts) {
            logger.info("Rejecting offer {} due to lack of ports (required {}, got {})", offer.getId().getValue(), Configuration.getPORTS(), offerPorts);
            return false;
        }
        return true;
    }

    private TaskID generateTaskId() {
        return TaskID.newBuilder().setValue("Kibana-" + Integer.toString(taskIDGenerator.incrementAndGet())).build();
    }

    private CommandInfo.Builder buildCommandInfo(Environment environment) {
        CommandInfo.Builder commandInfoBuilder = CommandInfo.newBuilder();
        commandInfoBuilder.setEnvironment(environment);
        commandInfoBuilder.setShell(false);
        return commandInfoBuilder;
    }

    private Environment buildEnvironment(String elasticSearchUrl) {
        Environment.Variable elasticSearchUrlVar = Environment.Variable.newBuilder()
                .setName("ELASTICSEARCH_URL")
                .setValue(elasticSearchUrl)
                .build();

        List<Environment.Variable> variables = Arrays.asList(elasticSearchUrlVar);

        return Environment.newBuilder()
                .addAllVariables(variables)
                .build();
    }

    private ContainerInfo.Builder buildContainerInfo(long port) {
        ContainerInfo.DockerInfo.Builder dockerInfo = ContainerInfo.DockerInfo.newBuilder();
        dockerInfo.setImage(Configuration.getDockerImageName());
        dockerInfo.addPortMappings(ContainerInfo.DockerInfo.PortMapping.newBuilder().setHostPort((int) port).setContainerPort(5601).setProtocol("tcp"));
        dockerInfo.setNetwork(ContainerInfo.DockerInfo.Network.BRIDGE);
        dockerInfo.build();

        ContainerInfo.Builder containerInfo = ContainerInfo.newBuilder();
        containerInfo.setType(ContainerInfo.Type.DOCKER);
        containerInfo.setDocker(dockerInfo).build();
        return containerInfo;
    }

    private TaskInfo buildTask(TaskID taskId, Offer offer, ContainerInfo.Builder containerInfo, CommandInfo.Builder commandInfoBuilder, long port) {
        TaskInfo task = TaskInfo.newBuilder()
                .setName(taskId.getValue())
                .setTaskId(taskId)
                .setSlaveId(offer.getSlaveId())
                .addResources(ResourceHelper.cpus(Configuration.getCPU()))
                .addResources(ResourceHelper.mem(Configuration.getMEM()))
                .addResources(ResourceHelper.ports(port, port))
                .setContainer(containerInfo)
                .setCommand(commandInfoBuilder)
                .build();
        return task;
    }

    @Override
    public void registered(SchedulerDriver schedulerDriver, FrameworkID frameworkID, MasterInfo masterInfo) {
        logger.info("registered() master={}:{}, framework={}", masterInfo.getIp(), masterInfo.getPort(), frameworkID);
    }

    @Override
    public void reregistered(SchedulerDriver schedulerDriver, MasterInfo masterInfo) {
        logger.info("reregistered()");
    }

    @Override
    public void resourceOffers(SchedulerDriver schedulerDriver, List<Offer> offers) {
        logger.info("resourceOffers() with {} offers", offers.size());

        List<Offer> acceptableOffers = new ArrayList<>();
        for (Offer offer : offers) {
            if (offerIsAcceptable(offer))
                acceptableOffers.add(offer);
        }
        if (acceptableOffers.isEmpty()) return;

        for (Map.Entry<String, Integer> requirement : configuration.getRequirementDeltaMap().entrySet()) {
            int delta = requirement.getValue();
            if (delta > 0) {
                while (delta > 0) {
                    if (acceptableOffers.isEmpty()) return;
                    Offer pickedOffer = acceptableOffers.get(0);
                    launchTask(requirement.getKey(), pickedOffer, schedulerDriver);
                    acceptableOffers.remove(pickedOffer);
                    delta--;
                }
            } else if (delta < 0) {
                //TODO too many instances running. kill tasks. Do we do this here?
            }
        }
    }

    @Override
    public void offerRescinded(SchedulerDriver schedulerDriver, OfferID offerID) {
        logger.info("offerRescinded()");
    }

    @Override
    public void statusUpdate(SchedulerDriver driver, TaskStatus taskStatus) {
        TaskID taskId = taskStatus.getTaskId();
        logger.info("statusUpdate() task {} is in state {}", taskId.getValue(), taskStatus.getState());

        switch (taskStatus.getState()) {
            case TASK_FAILED:
            case TASK_FINISHED:
                logger.info("removing task {} due to state: {}", taskId.getValue(), taskStatus.getState());
                configuration.removeRunningTask(taskId);
                break;
        }
    }

    @Override
    public void frameworkMessage(SchedulerDriver schedulerDriver, ExecutorID executorID, SlaveID slaveID, byte[] bytes) {
        logger.info("frameworkMessage()");
    }

    @Override
    public void disconnected(SchedulerDriver schedulerDriver) {
        logger.info("disconnected()");
    }

    @Override
    public void slaveLost(SchedulerDriver schedulerDriver, SlaveID slaveID) {
        logger.info("slaveLost()");
    }

    @Override
    public void executorLost(SchedulerDriver schedulerDriver, ExecutorID executorID, SlaveID slaveID, int i) {
        logger.info("executorLost()");
    }

    @Override
    public void error(SchedulerDriver schedulerDriver, String s) {
        logger.error("error() {}", s);
    }
}