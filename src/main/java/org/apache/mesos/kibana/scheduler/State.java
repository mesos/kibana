package org.apache.mesos.kibana.scheduler;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.mesos.Protos;
import org.apache.mesos.state.Variable;
import org.apache.mesos.state.ZooKeeperState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class in charge of reading/manipulating the Zookeeper state.
 * Used for keeping the Framework ID persistent
 */
public class State {
    private static final Logger LOGGER = LoggerFactory.getLogger(State.class);

    //Regex used for parsing the zookeeper url
    private static final String userAndPass = "[^/@]+";
    private static final String hostAndPort = "[A-z0-9-.]+(?::\\d+)?";
    private static final String zookeeperNode = "[^/]+";
    private static final String zookeeperUrlRegex = "^zk://((?:" + userAndPass + "@)?(?:" + hostAndPort + "(?:," + hostAndPort + ")*))(/" + zookeeperNode + "(?:/" + zookeeperNode + ")*)$";
    private static final String validZookeeperUrl = "zk://host1:port1,host2:port2,.../path";
    private static final Pattern zookeeperUrlPattern = Pattern.compile(zookeeperUrlRegex);

    private final ZooKeeperState state; // the zookeeper state

    /**
     * Constructor for State
     *
     * @param zookeeperUrl the URL of the Zookeeper
     */
    public State(String zookeeperUrl) {
        Matcher matcher = validateZookeeperUrl(zookeeperUrl);
        state = new ZooKeeperState(
                matcher.group(1),
                30000,
                TimeUnit.MILLISECONDS,
                "/" + SchedulerConfiguration.getFrameworkName() + matcher.group(2));
    }

    /**
     * Determines whether the passed in string is a valid zookeeper url or not
     *
     * @param zookeeperUrl the zookeeper url string
     * @return the Matcher used for validating the string
     * @throws IllegalArgumentException when the String is an invalid zookeeper url.
     */
    private Matcher validateZookeeperUrl(String zookeeperUrl) throws IllegalArgumentException {
        Matcher matcher = zookeeperUrlPattern.matcher(zookeeperUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Invalid zk url format: '%s' expected '%s'", zookeeperUrl, validZookeeperUrl));
        }
        return matcher;
    }

    /**
     * Returns the Framework ID stored in the Zookeeper state.
     *
     * @return the Framework ID stored in the Zookeeper state. Null if no Framework ID was found.
     */
    public Protos.FrameworkID getFrameworkId() {
        try {
            byte[] existingFrameworkId = state.fetch("frameworkId").get().value();
            if (existingFrameworkId.length > 0) {
                Protos.FrameworkID frameworkId = Protos.FrameworkID.parseFrom(existingFrameworkId);
                LOGGER.info("Found FrameworkID " + frameworkId.getValue());
                return frameworkId;
            } else {
                LOGGER.info("No existing FrameworkID found");
                return null;
            }
        } catch (InterruptedException | ExecutionException | InvalidProtocolBufferException e) {
            LOGGER.error("Failed to get framework ID from Zookeeper state", e);
            throw new RuntimeException("Failed to get framework ID from Zookeeper state", e);
        }
    }

    /**
     * Sets the Framework ID in the Zookeeper state
     *
     * @param frameworkId the Framework ID to set
     */
    public void setFrameworkId(Protos.FrameworkID frameworkId) {
        Variable value;
        try {
            value = state.fetch("frameworkId").get();
            value = value.mutate(frameworkId.toByteArray());
            state.store(value).get();
            LOGGER.info("Set framework ID in Zookeeper state to {}", frameworkId.getValue());
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Failed to set framework ID in Zookeeper state", e);
            throw new RuntimeException("Failed to set framework ID in Zookeeper state", e);
        }
    }

    /**
     * Removes the Framework ID from the Zookeeper.
     */
    public void removeFrameworkId() {
        //TODO: Call this when gracefully shutting down the scheduler.
        Variable value;
        try {
            value = state.fetch("frameworkId").get();
            state.expunge(value);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Failed to remove framework ID from Zookeeper state", e);
            throw new RuntimeException("Failed to remove framework ID from Zookeeper state", e);
        }
    }
}