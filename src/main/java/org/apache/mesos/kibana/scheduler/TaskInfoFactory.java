package org.apache.mesos.kibana.scheduler;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.ContainerInfo.DockerInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A factory class for TaskInfo
 */
public class TaskInfoFactory {

    private static final AtomicInteger taskIDGenerator = new AtomicInteger();  // used to generate task numbers

    /**
     * Creates a new task and registers it with the configuration, ready for launch.
     *
     * @param requirement   the task requirement
     * @param offer         the offer the task will use to run
     * @param configuration the scheduler's configuration, used to register the task with
     * @return a new task, ready for launch
     */
    public static Protos.TaskInfo buildTask(Map.Entry<String, Integer> requirement, Protos.Offer offer, Configuration configuration) {
        Protos.TaskID taskId = generateTaskId();
        long port = configuration.pickAndRegisterPort(taskId, offer);
        Protos.ContainerInfo container = buildContainerInfo(port);
        Protos.Environment environment = buildEnvironment(requirement.getKey());
        Protos.CommandInfo command = buildCommandInfo(environment);
        List<Protos.Resource> resources = buildResources(port);
        return buildTaskInfo(taskId, offer, container, command, resources);
    }

    /**
     * Generates a new TaskID
     *
     * @return a new TaskID
     */
    private static Protos.TaskID generateTaskId() {
        return Protos.TaskID.newBuilder().setValue("Kibana-" + taskIDGenerator.getAndIncrement()).build();
    }

    /**
     * Prepares a CommandInfoBuilder, including the given Environment
     *
     * @param environment the Environment to include
     * @return the CommandInfoBuilder
     */
    private static Protos.CommandInfo buildCommandInfo(Protos.Environment environment) {
        Protos.CommandInfo.Builder commandInfoBuilder = Protos.CommandInfo.newBuilder();
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
    private static Protos.Environment buildEnvironment(String elasticSearchUrl) {
        Protos.Environment.Variable.Builder esUrlVariableBuilder = Protos.Environment.Variable.newBuilder();
        esUrlVariableBuilder.setName("ELASTICSEARCH_URL");
        esUrlVariableBuilder.setValue(elasticSearchUrl);

        List<Protos.Environment.Variable> variables = Arrays.asList(esUrlVariableBuilder.build());

        Protos.Environment.Builder environmentBuilder = Protos.Environment.newBuilder();
        environmentBuilder.addAllVariables(variables);
        return environmentBuilder.build();
    }

    /**
     * Prepares the Docker ContainerInfo, adding a PortMapping for the given host port
     *
     * @param port the host port to direct to Kibana
     * @return the Docker ContainerInfo
     */
    private static Protos.ContainerInfo buildContainerInfo(long port) {
        DockerInfo.PortMapping.Builder portMappingBuilder = DockerInfo.PortMapping.newBuilder();
        portMappingBuilder.setHostPort((int) port);
        portMappingBuilder.setContainerPort(5601);
        portMappingBuilder.setProtocol("tcp");

        DockerInfo.Builder dockerInfo = DockerInfo.newBuilder();
        dockerInfo.setImage(Configuration.getDockerImageName());
        dockerInfo.addPortMappings(portMappingBuilder.build());
        dockerInfo.setNetwork(Protos.ContainerInfo.DockerInfo.Network.BRIDGE);
        dockerInfo.build();

        Protos.ContainerInfo.Builder containerInfo = Protos.ContainerInfo.newBuilder();
        containerInfo.setType(Protos.ContainerInfo.Type.DOCKER);
        containerInfo.setDocker(dockerInfo);
        return containerInfo.build();
    }

    /**
     * Prepares the tasks' resources
     *
     * @param port the hosts' port that will be mapped to Kibana
     * @return a list of the tasks' resources
     */
    private static List<Protos.Resource> buildResources(long port) {
        Protos.Resource cpu = Resources.cpus(Configuration.getCPU());
        Protos.Resource mem = Resources.mem(Configuration.getMEM());
        Protos.Resource ports = Resources.ports(port, port);
        return Arrays.asList(cpu, mem, ports);
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
    private static Protos.TaskInfo buildTaskInfo(Protos.TaskID taskId, Protos.Offer offer, Protos.ContainerInfo containerInfo, Protos.CommandInfo commandInfo, List<Protos.Resource> resources) {
        Protos.TaskInfo.Builder task = Protos.TaskInfo.newBuilder()
                .setName(taskId.getValue())
                .setTaskId(taskId)
                .setSlaveId(offer.getSlaveId())
                .setContainer(containerInfo)
                .setCommand(commandInfo)
                .addAllResources(resources);
        return task.build();
    }
}
