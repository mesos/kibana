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

/**
 * The Scheduler for the KibanaFramework, in charge of starting tasks on the Mesos Slaves to fulfill requirements.
 */
public class KibanaScheduler implements Scheduler {
    private static final Logger logger = LoggerFactory.getLogger(KibanaScheduler.class);
    private final AtomicInteger taskIDGenerator = new AtomicInteger();  // used to generate task numbers
    private Configuration configuration; // contains the scheduler's settings and tasks

    /**
     * Constructor for KibanaScheduler
     *
     * @param configuration the Configuration to use
     */
    public KibanaScheduler(Configuration configuration) {
        logger.info("constructing " + KibanaScheduler.class.getName());
        this.configuration = configuration;
    }

    /**
     * Launches a new Kibana task for the given elasticSearchUrl, using the offer
     *
     * @param elasticSearchUrl the elasticSearchUrl Kibana will use
     * @param offer            the offer to create the task for
     * @param driver           the driver to launch the task with
     */
    private void launchTask(String elasticSearchUrl, Offer offer, SchedulerDriver driver) {
        TaskID taskId = generateTaskId();
        long port = configuration.pickPort(taskId, offer);
        ContainerInfo.Builder containerInfo = buildContainerInfo(port);
        Environment environment = buildEnvironment(elasticSearchUrl);
        CommandInfo.Builder commandInfoBuilder = buildCommandInfo(environment);
        TaskInfo task = buildTaskInfo(taskId, offer, containerInfo, commandInfoBuilder, port);
        Filters filters = Filters.newBuilder().setRefuseSeconds(1).build();
        configuration.putRunningInstances(elasticSearchUrl, taskId);
        driver.launchTasks(Arrays.asList(offer.getId()), Arrays.asList(task), filters);
    }

    /**
     * Checks whether the given offer has the tasks' required resources
     *
     * @param offer the offer whose resources to check
     * @return a boolean representing whether or not the offer has all the required resources
     */
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

        logger.info("Accepting offer {} with {} cpus, {} memory and {} ports", offer.getId().getValue(), offerCpus, offerMem, offerPorts);
        return true;
    }

    /**
     * Generates a new TaskID
     *
     * @return a new TaskID
     */
    private TaskID generateTaskId() {
        return TaskID.newBuilder().setValue("Kibana-" + Integer.toString(taskIDGenerator.incrementAndGet())).build();
    }

    /**
     * Prepares a CommandInfoBuilder, including the given Environment
     *
     * @param environment the Environment to include
     * @return the CommandInfoBuilder
     */
    private CommandInfo.Builder buildCommandInfo(Environment environment) {
        CommandInfo.Builder commandInfoBuilder = CommandInfo.newBuilder();
        commandInfoBuilder.setEnvironment(environment);
        commandInfoBuilder.setShell(false);
        return commandInfoBuilder;
    }

    /**
     * Prepares the Environment, setting the given elasticSearchUrl as an Environment Variable
     *
     * @param elasticSearchUrl the elasticSearchUrl to set
     * @return the Environment
     */
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

    /**
     * Prepares the Docker ContainerInfo, adding a PortMapping for the given host port
     *
     * @param port the host port to direct to Kibana
     * @return the Docker ContainerInfo
     */
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

    /**
     * Prepares the TaskInfo for the Kibana task
     *
     * @param taskId        the tasks' ID
     * @param offer         the offer with which to run the task
     * @param containerInfo the tasks' ContainerInfo
     * @param commandInfo   the tasks' CommandInfo
     * @param port          the host port which will be mapped
     * @return the TaskInfo
     */
    private TaskInfo buildTaskInfo(TaskID taskId, Offer offer, ContainerInfo.Builder containerInfo, CommandInfo.Builder commandInfo, long port) {
        TaskInfo task = TaskInfo.newBuilder()
                .setName(taskId.getValue())
                .setTaskId(taskId)
                .setSlaveId(offer.getSlaveId())
                .addResources(Resources.cpus(Configuration.getCPU()))
                .addResources(Resources.mem(Configuration.getMEM()))
                .addResources(Resources.ports(port, port))
                .setContainer(containerInfo)
                .setCommand(commandInfo)
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
                logger.info("ElasticSearch {} is missing {} tasks. Attempting to start tasks.", requirement.getKey(), delta);
                while (delta > 0) {
                    if (acceptableOffers.isEmpty()) return;
                    Offer pickedOffer = acceptableOffers.get(0);
                    launchTask(requirement.getKey(), pickedOffer, schedulerDriver);
                    acceptableOffers.remove(pickedOffer);
                    delta--;
                }
            } else if (delta < 0) {
                logger.info("ElasticSearch {} has an excess of {} tasks. Removal of tasks not implemented yet.", requirement.getKey(), delta);
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
                logger.info("Removing task {} due to state: {}", taskId.getValue(), taskStatus.getState());
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