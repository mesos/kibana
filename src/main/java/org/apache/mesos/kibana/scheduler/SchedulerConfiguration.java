package org.apache.mesos.kibana.scheduler;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.Value;

import java.util.*;

/**
 * Contains the configuration for a KibanaScheduler.
 * Used to manage task settings and required/running tasks.
 */
public class SchedulerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerConfiguration.class);

    private static final String FRAMEWORK_NAME = "kibana";         // the name of this Mesos framework
    private static final String DEFAULT_KIBANA_VERSION = "latest"; // version of Kibana (and its image) to use
    private static final String DOCKER_IMAGE_NAME = "kibana";      // the name of Kibana Docker image to use when starting a task
    private static final double REQUIRED_PORT_COUNT = 1D;          // the amount of ports a task needs

    private static final Options OPTIONS = new Options() {{     // launch options for the KibanaFramework
        addOption("zk", "zookeeper", true, "Zookeeper URL (zk://host:port/mesos)");
        addOption("es", "elasticsearch", true, "ElasticSearch URLs (http://host:port;http://host:port)");
        addOption("v", "version", true, "Version of Kibana docker image to use");
        addOption("cpu", "requiredCpu", true, "Amount of CPUs given to a Kibana instance (0.1)");
        addOption("mem", "requiredMem", true, "Amount of memory (MB) given to a Kibana instance (128)");
        addOption("disk", "requiredDisk", true, "Amount of disk space (MB) given to a Kibana instance (20)");
        addOption("p", "port", true, "TCP port for the webservice (9001)");
    }};

    protected Map<String, Integer> requiredTasks = new HashMap<>();             // a map containing the required tasks: <elasticSearchUrl, numberOfInstances>
    protected Map<String, List<TaskID>> runningTasks = new HashMap<>();  // a map containing the currently running tasks: <elasticSearchUrl, listOfTaskIds>
    private Map<TaskID, Long> usedPortNumbers = new HashMap<>();         // a list containing the currently used ports, part of the Docker host ports workaround
    private String zookeeper;   // the zookeeper url
    private State state;        // the state of the zookeeper
    private int apiPort = 9001;     // the port of the JSON API
    private double requiredCpu = 0.1D; // the amount of CPUs a task needs
    private double requiredMem = 128D; // the amount of memory a task needs
    private double requiredDisk = 25D; // the amount of disk space a task needs
    private String kibanaVersion = DEFAULT_KIBANA_VERSION;

    /**
     * Returns the name of the framework
     *
     * @return the name of the framework
     */
    public static String getFrameworkName() {
        return FRAMEWORK_NAME;
    }

    /**
     * Returns the name of the Kibana Docker image
     *
     * @return the name of the Kibana Docker image
     */
    public String getDockerImageName() {
        return String.format("%s:%s", DOCKER_IMAGE_NAME, kibanaVersion);
    }

    /**
     * Returns the amount of ports a task needs
     *
     * @return the amount of ports a task needs
     */
    public static double getRequiredPortCount() {
        return REQUIRED_PORT_COUNT;
    }

    /**
     * Returns the start options of the KibanaFramework
     *
     * @return the start options of the KibanaFramework
     */
    public static Options getOptions() {
        return OPTIONS;
    }

    /**
     * Returns the required disk space in MB a task needs
     *
     * @return the required disk space in MB a task needs
     */
    public double getRequiredDisk() {
        return requiredDisk;
    }

    /**
     * Sets the required disk space in MB a task needs
     *
     * @param requiredDisk the required disk space in MB a task needs
     */
    public void setRequiredDisk(double requiredDisk) {
        LOGGER.info("Setting required disk space to {} MB.", requiredDisk);
        this.requiredDisk = requiredDisk;
    }

    /**
     * Returns the port the JSON API uses
     *
     * @return the port the JSON API uses
     */
    public int getApiPort() {
        return apiPort;
    }

    /**
     * Sets the port the JSON API uses
     *
     * @param apiPort the port number for the JSON API to use
     */
    public void setApiPort(int apiPort) {
        LOGGER.info("Setting api port to {}", apiPort);
        this.apiPort = apiPort;
    }

    /**
     * Returns the address of the zookeeper
     *
     * @return the address of the zookeeper
     */
    public String getZookeeper() {
        return zookeeper;
    }

    /**
     * Sets the zookeeper address
     *
     * @param master the address of the zookeeper
     */
    public void setZookeeper(String master) {
        LOGGER.info("Setting zookeeper address to {}", master);
        this.zookeeper = master;
    }

    /**
     * Gets the state of the zookeeper
     *
     * @return the state of the zookeeper
     */
    public State getState() {
        return state;
    }

    /**
     * Sets the state of the zookeeper
     *
     * @param state the state of the zookeeper
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Returns the amount of CPUs a task needs
     *
     * @return the amount of CPUs a task needs
     */
    public double getRequiredCpu() {
        return requiredCpu;
    }

    /**
     * Sets the amount of CPUs a task needs
     *
     * @param requiredCpu the amount of CPUs a task
     */
    public void setRequiredCpu(double requiredCpu) {
        LOGGER.info("Setting required CPUs to {}.", requiredCpu);
        this.requiredCpu = requiredCpu;
    }

    /**
     * Returns the amount of memory a task needs
     *
     * @return the amount of memory a task needs
     */
    public double getRequiredMem() {
        return requiredMem;
    }

    /**
     * Sets the amount of memory a task needs
     *
     * @param requiredMem the amount of memory a task needs
     */
    public void setRequiredMem(double requiredMem) {
        LOGGER.info("Setting required memory to {} MB.", requiredMem);
        this.requiredMem = requiredMem;
    }

    /**
     * Picks a port number from the given offer's port resources
     *
     * @param taskId the TaskID to register the picked ports with
     * @param offer  the offer from which's resources to pick a port
     * @return a port number
     */
    public long pickAndRegisterPortNumber(TaskID taskId, Offer offer) {
        for (Resource resource : offer.getResourcesList()) {
            if (!resource.getName().equals("ports")) continue;

            List<Value.Range> offeredRanges = resource.getRanges().getRangeList();
            for (Value.Range portRange : offeredRanges) {
                long begin = portRange.getBegin();
                long end = portRange.getEnd();
                for (long port = begin; port < end; port++) {
                    if (!usedPortNumbers.values().contains(port)) {
                        usedPortNumbers.put(taskId, port);
                        LOGGER.info("Task {} received port {}.", taskId, port);
                        return port;
                    }
                }
            }
        }
        LOGGER.warn("Offer {} had no unused port to offer! Task {} received no port!", offer.getId().getValue(), taskId.getValue());
        return -1;
    }

    /**
     * Increases the required number of instances for the given elasticSearchUrl by the given amount.
     * If the resulting amount of required instances is equal to or lower than 0, the elasticSearchUrl entry is removed from the requiredTasks.
     *
     * @param elasticSearchUrl the elasticSearchUrl to change the required amount of instances for
     * @param amount           the amount by which to change the required amount of instances
     */
    public void registerRequirement(String elasticSearchUrl, int amount) {
        if (requiredTasks.containsKey(elasticSearchUrl)) {
            int newAmount = amount + requiredTasks.get(elasticSearchUrl);
            if (newAmount <= 0) {
                requiredTasks.remove(elasticSearchUrl);
                LOGGER.info("No more instances are required for ElasticSearch {}", elasticSearchUrl);
            } else {
                requiredTasks.put(elasticSearchUrl, newAmount);
                LOGGER.info("Now requiring {} instances for ElasticSearch {}", newAmount, elasticSearchUrl);
            }
        } else if (amount > 0) {
            requiredTasks.put(elasticSearchUrl, amount);
            LOGGER.info("Now requiring {} instances for ElasticSearch {}", amount, elasticSearchUrl);
        }
    }

    /**
     * Returns a Map with all known elasticSearchUrls and the delta between the required and running number of instances.
     *
     * @return a Map with all known elasticSearchUrls and the delta between the required and running number of instances
     */
    public Map<String, Integer> getRequirementDeltaMap() {
        Set<String> elasticSearchUrls = new HashSet<>();
        elasticSearchUrls.addAll(requiredTasks.keySet());
        elasticSearchUrls.addAll(runningTasks.keySet());

        Map<String, Integer> requirementDeltaMap = new HashMap<>();
        for (String elasticSearchUrl : elasticSearchUrls) {
            requirementDeltaMap.put(elasticSearchUrl, getRequirementDelta(elasticSearchUrl));
        }
        return requirementDeltaMap;
    }

    /**
     * Calculates the delta between the required amount and the running amount of instances for the given elasticSearchUrl
     *
     * @param elasticSearchUrl the elasticSearchUrl to calculate the delta for
     * @return the delta between the required amount and the running amount of instances for the given elasticSearchUrl
     */
    private int getRequirementDelta(String elasticSearchUrl) {
        if (requiredTasks.containsKey(elasticSearchUrl)) {
            int requiredAmount = requiredTasks.get(elasticSearchUrl);
            if (runningTasks.containsKey(elasticSearchUrl)) {
                int actualAmount = runningTasks.get(elasticSearchUrl).size();
                return requiredAmount - actualAmount;
            }
            return requiredAmount;
        }

        if (runningTasks.containsKey(elasticSearchUrl)) {
            int actualAmount = runningTasks.get(elasticSearchUrl).size();
            return -actualAmount;
        }
        return 0;
    }

    /**
     * Adds the given task to the currently running tasks, under the given elasticSearchUrl
     *
     * @param elasticSearchUrl the elasticSearchUrl under which to add the given task
     * @param taskId           the task to add
     */
    public void registerTask(String elasticSearchUrl, TaskID taskId) {
        if (runningTasks.containsKey(elasticSearchUrl)) {
            runningTasks.get(elasticSearchUrl).add(taskId);
        } else {
            ArrayList<TaskID> instances = new ArrayList<>();
            instances.add(taskId);
            runningTasks.put(elasticSearchUrl, instances);
        }
        LOGGER.info("Now running task {} for ElasticSearch{}", taskId.getValue(), elasticSearchUrl);
    }

    /**
     * Unregisters the given task and its ports
     *
     * @param taskId the task to unregister
     */
    public void unregisterTask(TaskID taskId) {
        for (Map.Entry<String, List<TaskID>> taskEntry : runningTasks.entrySet()) {
            if (taskEntry.getValue().contains(taskId)) {
                taskEntry.getValue().remove(taskId);
                if (taskEntry.getValue().isEmpty())
                    runningTasks.remove(taskEntry.getKey());
                usedPortNumbers.remove(taskId);
                LOGGER.info("Unregistered task {}", taskId.getValue());
                return;
            }
        }
    }

    /**
     * Handles any passed in arguments
     *
     * @param args the passed in arguments
     */
    public void parseLaunchArguments(String[] args) throws ParseException {
        LOGGER.info("Parsing arguments ({}).", Arrays.toString(args));
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(OPTIONS, args);

        String zk = commandLine.getOptionValue("zk");
        if (zk != null) {
            setZookeeper(zk);
        } else {
            throw new MissingArgumentException("Zookeeper url is required.");
        }

        String cpu = commandLine.getOptionValue("cpu", "0.1");
        setRequiredCpu(Double.parseDouble(cpu));

        String mem = commandLine.getOptionValue("mem", "128");
        setRequiredMem(Double.parseDouble(mem));

        String disk = commandLine.getOptionValue("disk", "25");
        setRequiredDisk(Double.parseDouble(disk));

        String port = commandLine.getOptionValue("p", "9001");
        setApiPort(Integer.parseInt(port));

        String esUrls = commandLine.getOptionValue("es");
        if (esUrls != null) {
            for (String esUrl : esUrls.split(";")) {
                registerRequirement(esUrl, 1);
            }
        }

        String version = commandLine.getOptionValue("v");
        if (version != null) {
            setKibanaVersion(version);
        }

    }

    /**
     * Returns the youngest task of the given elasticSearchUrl
     *
     * @param elasticSearchUrl the the elasticSearchUrl of which to return the youngest task
     * @return the youngest task of the given elasticSearchUrl
     */
    public TaskID getYoungestTask(String elasticSearchUrl) {
        if (runningTasks.containsKey(elasticSearchUrl)) {
            List<TaskID> tasks = runningTasks.get(elasticSearchUrl);
            return tasks.get(tasks.size() - 1);
        }
        return null;
    }

    public void setKibanaVersion(String kibanaVersion) {
        this.kibanaVersion = kibanaVersion;
    }

}