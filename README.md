# mesos-kibana

[Kibana](https://www.elastic.co/products/kibana) framework for Mesos.

This uses the [Mesos-Framework](https://github.com/ContainerSolutions/mesosframework) project. The framework is generic and only becomes a Kibana framework with the correct configuration.

# Features
(Features come from the upstream [Mesos-Framework](https://github.com/ContainerSolutions/mesosframework) and [Mesos-Starter](https://github.com/ContainerSolutions/mesos-starter) projects)

- [x] State stored in ZooKeeper
- [x] Mesos Authorisation
- [ ] ZooKeeper Authorisation (should work, requires testing)
- [x] Live horizontal scaling via REST endpoint
- [x] Jar mode (no docker)
- [x] Resource specification (including port)
- [x] Import Kibana.yml settings file
- [x] "Spread" orchestration strategy (Spreads instances across distinct hosts)
- [x] Decoupled from Kibana. Use any version.
- [x] Decoupled from Mesos. Use any version 0.25+.
- [x] Single endpoint to check health of all instances

# Quick start
To start the kibana framework, simply start the mesosframework binary or docker container with a properties file containing your settings.

## On minimesos
First install [minimesos](http://minimesos.org), then run:

```
minimesos up --num-agents 3
export MINIMESOS_MARATHON=http://192.168.99.100:8080
docker run -d elasticsearch:latest
curl -sS -o marathon.json https://raw.githubusercontent.com/mesos/kibana/master/manual-tests/marathon-minimesos-docker.json ; curl -XPOST -H 'Content-Type:application/json' -d @marathon.json $MINIMESOS_MARATHON/v2/apps
```

This will start a cluster of Kibana instances running on your local machine connecting to a static elasticsearch instance. Please check out the [Elasticsearch framework](http://www.github.com/mesos/elasticsearch) if you are interested in running Elasticsearch.

## On a real Mesos cluster (docker mode)
If you want to run on your own cluster, you will probably want to customize the installation using a properties file, rather than in the marathon file itself. This marathon file will ask Mesos to download the properties file from a url. First export the locations of your master and first slave, then run these commands:

```
export MASTER=${master ip address}
export SLAVE0=${elasticsearch ip address - see json file}
curl -sS -o marathon.json https://raw.githubusercontent.com/mesos/kibana/master/manual-tests/marathon-docker.json
cat marathon.json | sed -e 's/$MASTER/'"$MASTER"'/' | sed -e 's/$SLAVE0/'"$SLAVE0"'/' | curl -XPOST -H 'Content-Type:application/json' -d @- http://$MASTER:8080/v2/apps
```

See the [marathon file](./manual-tests/marathon-docker.json) and the [properties file](./docs/examples/docker.properties) for further ways to customize the installation.

## On a real Mesos cluster (jar mode)
Sometimes people don't want to run docker. This example will install a Kibana cluster using the java binaries. This marathon file will ask Mesos to download the properties file from a url. First export the locations of your master and first slave, then run these commands:

```
export MASTER=${master ip address}
export SLAVE0=${elasticsearch ip address - see json file}
curl -sS -o marathon.json https://raw.githubusercontent.com/mesos/kibana/master/manual-tests/marathon-jar.json
cat marathon.json | sed -e 's/$MASTER/'"$MASTER"'/' | sed -e 's/$SLAVE0/'"$SLAVE0"'/' | curl -XPOST -H 'Content-Type:application/json' -d @- http://$MASTER:8080/v2/apps
```

See the [marathon file](./manual-tests/marathon-jar.json) and the [properties file](./docs/examples/jar.properties) for further ways to customize the installation.

# Passing options
Because this project uses [Mesos-Framework](https://github.com/ContainerSolutions/mesosframework) there is no Kibana-specific code to compile or download. To run, simply pass a configuration file or options. Example marathon files can be found in the [manual-tests](./manual-tests) directory.

All options can be specified as either:
- A cli parameter: `--mesos.command=pwd`
- A properties file: `mesos.command=pwd`
- Java options: `-Dmesos.command=pwd`
- Environmental variables: `MESOS_COMMAND=pwd`
 
In that order of preference.

To pass a configuration file, the following property must be set:
- `--spring.config.location=my.properties` (Or the env var `SPRING_CONFIG_LOCATION`, etc.)

# Full list of Kibana related settings
All settings are written in properties or argument format. Remember that these can also be specified in environment or yml format.

| Command | Description |
| --- | --- |
| `spring.application.name` | Required application name for Spring |
| `mesos.framework.name` | Framework name used in Mesos and ZooKeeper |
| `mesos.master` | URL of the master (usually provided by ZooKeeper) |
| `mesos.zookeeper.server` | IP:PORT of the zookeeper server |
| `mesos.resources.cpus` | CPUs allocated to the task |
| `mesos.resources.mem` | RAM allocated to the task |
| `mesos.resources.count` | Number of task instances |
| `mesos.resources.ports.${VAR}.host` | A requested port, where VAR is the name of the port. |
| `mesos.resources.ports.${VAR}.container` | When in bridge mode, the container port to map the host port to. |
| `mesos.docker.image` | Docker image to use |
| `mesos.docker.network` | Type of docker network |
| `mesos.command` | The command to run |
| `mesos.uri[0..]` | Files to download into the Mesos sandbox |
| `logging.level.com.containersolutions.mesos` | Logging level |
| `mesos.principal` | The principal for Mesos Auth. |
| `mesos.secret`  | The secret for Mesos Auth. |
| `mesos.healthCheck.command` | The command to run as the Mesos healthcheck |

Note that there are more parameters. See [Mesos-Starter](https://github.com/ContainerSolutions/mesos-starter).

# Passing Kibana settings
First upload the settings file into the task sandbox with the `mesos.uri` property:
```
mesos.uri[0]=https://gist.githubusercontent.com/philwinder/592a1ab2db40431c1b08/raw/kibana.yml
```
The file can be a local file (local to the host that the task is to run on) or a url.

Then copy that file into the kibana config directory, overwriting the default. Kibana does not expose a config location parameter so overwriting is the only way to pass the settings into Kibana. For example, in jar mode:
```
mesos.command= cp $MESOS_SANDBOX/kibana.yml $MESOS_SANDBOX/kibana-*/config/kibana.yml ; cd kibana-* ; bin/kibana --port=$UI_5061 --elasticsearch ${elasticsearch.http}
```
Or when in docker mode:
```
mesos.command=mv $MESOS_SANDBOX/kibana.yml /opt/kibana/config/kibana.yml ; kibana --port=$UI_5061 --elasticsearch ${elasticsearch.http}
```
# Port allocation
Ports are allocated by Mesos and provided to the application as an environmental variable. For example:
```
mesos.resources.ports.UI_5061.host=ANY
mesos.resources.ports.UI_5061.container=5601
```
Assigns an unprivileged port to the environmental variable `UI_5061`. This environmental variable can now be use in the `mesos.command`, if required.

The value can be one of the following types:

| Command | Description |
| --- | --- |
| `ANY` | The next available unprivileged port (>1024) |
| `UNPRIVILEGED` | The next available unprivileged port (>1024) |
| `PRIVILEGED` | The next available privileged port (<=1024) |
| `1234` | A specific port (e.g. 1234) |

# Health checks
[MesosFramework](https://github.com/ContainerSolutions/mesosframework) uses Spring Actuator to provide health and metrics endpoints. To access the health endpoint visit: `http://${SCHEDULER_IP_ADDRESS}:${server.port}/health`. Acuator defaults the `server.port` to 8080, although it is recommended to reserve ports in the marathon command and set this port explicitly. E.g. [jar mode json file](./manual-tests/marathon-jar.json)

See the [Spring documentation](http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#production-ready-endpoints) for more information.

# Horizontal scaling
This adds an endpoint at the following location to control the number of instances in the cluster. The endpoint matches the properties file definition of the same name:

`GET /mesos/resources/count` Returns the current number of requested instances. For example to get the current number of instances:

```
$ curl -s http://${SCHEDULER_IP_ADDRESS}:${server.port}/mesos/resources/count
3
```

`POST /mesos/resources/count` with a body of type `Integer` will set the number of requested instances. For example, to set the number of instances to 1:

```
$ curl -XPOST -H 'Content-Type: text/plain' http://${SCHEDULER_IP_ADDRESS}:${server.port}/mesos/resources/count -d 1
```

# Sponsors
This project is sponsored by Cisco Cloud Services.

# License
Apache License 2.0
