# mesos-kibana

[Kibana](https://www.elastic.co/products/kibana) framework for Mesos.

This uses the [Mesos-Framework](ttps://github.com/ContainerSolutions/mesosframework) project. The framework is generic and only becomes a Kibana framework with the correct configuration.

# Features
(Features come from the [Mesos-Starter](ttps://github.com/ContainerSolutions/mesos-starter) project)

- [x] State stored in ZooKeeper
- [x] Mesos and ZooKeeper Authorisation
- [ ] Live horizontal scaling
- [x] Jar mode (no docker)
- [x] Resource specification (including port)
- [x] Import Kibana.yml settings file
- [x] "Spread" orchestration strategy (Spreads instances across distinct hosts)
- [x] Decoupled from Kibana. Use any version.
- [ ] Decoupled from Mesos. Use any version 0.25+.
- [x] Single endpoint to check health of all instances

# Usage
Because this project uses [Mesos-Framework](ttps://github.com/ContainerSolutions/mesosframework) there is no Kibana-specific code to compile or download. To run, simply pass a configuration file or options. Example marathon files can be found in the [manual-tests](./manual-tests) directory.

All options can be specified as either:
- A cli parameter: `--mesos.command=pwd`
- A properties file: `mesos.command=pwd`
- Java options: `-Dmesos.command=pwd`
- Environmental variables: `MESOS_COMMAND=pwd`
In that order of preference.

To pass a configuration file, the following property must be set:
- `--spring.config.location=my.properties` (Or the env var `SPRING_CONFIG_LOCATION`, etc.)

## Useful Kibana related settings
### Jar mode
See the [jar mode json file](./manual-tests/marathon-jar.json) and [jar properties](./docs/examples/jar.properties) files for examples.
### Docker mode
See the [docker mode json file](./manual-tests/marathon-docker.json) and [docker proerties](./docs/examples/docker.properties) files for examples.
### Passing Kibana settings
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
## Health checks
[Mesos-Framework](ttps://github.com/ContainerSolutions/mesosframework) uses Spring Actuator to provide health and metrics endpoints. To access the health endpoint visit: `http://${SCHEDULER_IP_ADDRESS}:${server.port}/health`. Acuator defaults the `server.port` to 8080, although it is recommended to reserve ports in the marathon command and set this port explicitly. E.g. [jar mode json file](./manual-tests/marathon-jar.json)

See the [Spring documentation](http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#production-ready-endpoints) for more information.


# Sponsors
This project is sponsored by Cisco Cloud Services.

# License
Apache License 2.0
