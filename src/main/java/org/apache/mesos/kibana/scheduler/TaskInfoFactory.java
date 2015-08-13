package org.apache.mesos.kibana.scheduler;

import org.apache.mesos.Protos.CommandInfo;
import org.apache.mesos.Protos.ContainerInfo;
import org.apache.mesos.Protos.Environment;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.ContainerInfo.DockerInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A factory class for TaskInfo
 */
public class TaskInfoFactory {
    private static final AtomicInteger TASK_ID_GENERATOR = new AtomicInteger();  // used to generate task numbers

    /**
     * Creates a new task and registers it with the configuration, ready for launch.
     *
     * @param requirement   the task requirement
     * @param offer         the offer the task will use to run
     * @param configuration the scheduler's configuration, used to register the task with
     * @return a new task, ready for launch
     */
    public static TaskInfo buildTask(Map.Entry<String, Integer> requirement, Offer offer, SchedulerConfiguration configuration) {
        TaskID taskId = generateTaskId();
        long port = configuration.pickAndRegisterPortNumber(taskId, offer);
        ContainerInfo container = buildContainerInfo(port);
        Environment environment = buildEnvironment(requirement.getKey());
        CommandInfo command = buildCommandInfo(environment);
        List<Resource> resources = buildResources(configuration, port); //DCOS-06 Scheduler MUST only use the necessary fraction of an offer.
        return buildTaskInfo(taskId, offer, container, command, resources);
    }

    /**
     * Generates a new TaskID
     *
     * @return a new TaskID
     */
    private static TaskID generateTaskId() {
        return TaskID.newBuilder().setValue("kibana-" + TASK_ID_GENERATOR.getAndIncrement()).build();
    }

    /**
     * Prepares a CommandInfoBuilder, including the given Environment
     *
     * @param environment the Environment to include
     * @return the CommandInfoBuilder
     */
    private static CommandInfo buildCommandInfo(Environment environment) {
        CommandInfo.Builder commandInfoBuilder = CommandInfo.newBuilder();
        commandInfoBuilder.setEnvironment(environment);
        commandInfoBuilder.setShell(false);
        return commandInfoBuilder.build();
    }

    /**
     * Prepares the Environment, setting the given elasticSearchUrl as an Environment Variable
     *
     * @param elasticSearchUrl the elasticSearchUrl to set
     * @return the Environment
     */
    private static Environment buildEnvironment(String elasticSearchUrl) {
        Environment.Variable.Builder esUrlVariableBuilder = Environment.Variable.newBuilder();
        esUrlVariableBuilder.setName("ELASTICSEARCH_URL");
        esUrlVariableBuilder.setValue(elasticSearchUrl);

        List<Environment.Variable> variables = Arrays.asList(esUrlVariableBuilder.build());

        Environment.Builder environmentBuilder = Environment.newBuilder();
        environmentBuilder.addAllVariables(variables);
        return environmentBuilder.build();
    }

    /**
     * Prepares the Docker ContainerInfo, adding a PortMapping to Kibana for the given host port
     *
     * @param port the host port to direct to Kibana
     * @return the Docker ContainerInfo
     */
    private static ContainerInfo buildContainerInfo(long port) {
        DockerInfo.PortMapping.Builder portMappingBuilder = DockerInfo.PortMapping.newBuilder();
        portMappingBuilder.setHostPort((int) port);
        portMappingBuilder.setContainerPort(5601);
        portMappingBuilder.setProtocol("tcp");

        DockerInfo.Builder dockerInfo = DockerInfo.newBuilder();
        dockerInfo.setImage(SchedulerConfiguration.getDockerImageName());
        dockerInfo.addPortMappings(portMappingBuilder.build());
        dockerInfo.setNetwork(ContainerInfo.DockerInfo.Network.BRIDGE);
        dockerInfo.build();

        ContainerInfo.Builder containerInfo = ContainerInfo.newBuilder();
        containerInfo.setType(ContainerInfo.Type.DOCKER);
        containerInfo.setDocker(dockerInfo);
        return containerInfo.build();
    }

    /**
     * Prepares the tasks' resources
     *
     *
     * @param configuration
     * @param port the hosts' port that will be mapped to Kibana
     * @return a list of the tasks' resources
     */
    private static List<Resource> buildResources(SchedulerConfiguration configuration, long port) {
        Resource cpu = Resources.buildCpuResource(configuration.getRequiredCpu());
        Resource mem = Resources.buildMemResource(configuration.getRequiredMem());
        Resource disk = Resources.buildDiskResource(configuration.getRequiredDisk());
        Resource ports = Resources.buildPortResource(port, port);
        return Arrays.asList(cpu, mem, disk, ports);
    }

    /**
     * Prepares the TaskInfo for the Kibana task
     *
     * @param taskId        the tasks' ID
     * @param offer         the offer with which to run the task
     * @param containerInfo the tasks' ContainerInfo
     * @param commandInfo   the tasks' CommandInfo
     * @param resources     the tasks' resources
     * @return the TaskInfo
     */
    private static TaskInfo buildTaskInfo(TaskID taskId, Offer offer, ContainerInfo containerInfo, CommandInfo commandInfo, List<Resource> resources) {
        TaskInfo.Builder task = TaskInfo.newBuilder()
                .setName(taskId.getValue())
                .setTaskId(taskId)
                .setSlaveId(offer.getSlaveId())
                .setContainer(containerInfo)
                .setCommand(commandInfo)
                .addAllResources(resources);
        return task.build();
    }
}