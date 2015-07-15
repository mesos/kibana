package org.apache.mesos.kibana.scheduler;

import org.apache.mesos.*;
import org.apache.mesos.Protos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class KibanaScheduler implements Scheduler {

    private static final Logger logger = LoggerFactory.getLogger(KibanaScheduler.class);

    private final String dockerImageName = "kibana";
    private final int instancesToRun = 1;
    private final String elasticSearchUrl;

    protected final List<String> pendingInstances = new ArrayList<>();
    protected final List<String> runningInstances = new ArrayList<>();
    private final AtomicInteger taskIDGenerator = new AtomicInteger();

    public KibanaScheduler(String elasticSearchUrl) {
        logger.info("constructing " + KibanaScheduler.class.getName());
        this.elasticSearchUrl = elasticSearchUrl;
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
        for (Offer offer : offers) {
            if (instancesToRun <= runningInstances.size() + pendingInstances.size()) {
                break;
            }

            TaskID taskId = generateTaskId();
            ContainerInfo.Builder containerInfo = buildContainerInfo();
            Environment environment = buildEnvironment();
            CommandInfo.Builder commandInfoBuilder = buildCommandInfo(environment);
            TaskInfo task = buildTask(taskId, offer, containerInfo, commandInfoBuilder);
            Filters filters = Filters.newBuilder().setRefuseSeconds(1).build();
            logger.info("Launching task {}", taskId.getValue());
            pendingInstances.add(taskId.getValue());
            schedulerDriver.launchTasks(Arrays.asList(offer.getId()), Arrays.asList(task), filters);
        }
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

    private Environment buildEnvironment() {
        Environment.Variable elasticSearchUrlVar = Environment.Variable.newBuilder()
                .setName("ELASTICSEARCH_URL")
                .setValue(elasticSearchUrl)
                .build();

        List<Environment.Variable> variables = Arrays.asList(elasticSearchUrlVar);

        return Environment.newBuilder()
                .addAllVariables(variables)
                .build();
    }

    private ContainerInfo.Builder buildContainerInfo() {
        ContainerInfo.DockerInfo.Builder dockerInfo = ContainerInfo.DockerInfo.newBuilder();
        dockerInfo.setImage(dockerImageName);
        dockerInfo.addPortMappings(ContainerInfo.DockerInfo.PortMapping.newBuilder().setHostPort(31001).setContainerPort(5601).setProtocol("tcp"));
        dockerInfo.setNetwork(ContainerInfo.DockerInfo.Network.BRIDGE);
        dockerInfo.build();

        ContainerInfo.Builder containerInfo = ContainerInfo.newBuilder();
        containerInfo.setType(ContainerInfo.Type.DOCKER);
        containerInfo.setDocker(dockerInfo).build();
        return containerInfo;
    }

    private TaskInfo buildTask(TaskID taskId, Offer offer, ContainerInfo.Builder containerInfo, CommandInfo.Builder commandInfoBuilder) {
        TaskInfo task = TaskInfo.newBuilder()
                .setName(taskId.getValue())
                .setTaskId(taskId)
                .setSlaveId(offer.getSlaveId())
                .addResources(ResourceHelper.cpus(0.1D))
                .addResources(ResourceHelper.mem(512D))
                .addResources(ResourceHelper.ports(31000L, 32000L)) //TODO you don't need all of them, just pick the necessary amount from the range...
                .setContainer(containerInfo)
                .setCommand(commandInfoBuilder)
                .build();
        return task;
    }

    @Override
    public void offerRescinded(SchedulerDriver schedulerDriver, OfferID offerID) {
        logger.info("offerRescinded()");
    }

    @Override
    public void statusUpdate(SchedulerDriver driver, TaskStatus taskStatus) {
        final String taskId = taskStatus.getTaskId().getValue();
        logger.info("statusUpdate() task {} is in state {}", taskId, taskStatus.getState());

        switch (taskStatus.getState()) {
            case TASK_RUNNING:
                pendingInstances.remove(taskId);
                runningInstances.add(taskId);
                break;
            case TASK_FAILED:
            case TASK_FINISHED:
                pendingInstances.remove(taskId);
                runningInstances.remove(taskId);
                break;
        }
        logger.info("Number of instances: pending={}, running={}", pendingInstances.size(), runningInstances.size());
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