# mesos-kibana

[Kibana](https://www.elastic.co/products/kibana) framework for Mesos.

The kibana framework is developed with a simple approach: For each instance of elasticsearch known by the framework, one or more instances of kibana can be deployed to serve the users.

Each instance of kibana run as a docker image in the mesos cluster, thus docker containers must be supported by the mesos-slaves.

# Bits and pieces

Beside the Mesos Kibana framework itself, few other scripts, services and small solutions have been created:

* Using Gradle (instead of Maven) to _build, deploy, test etc._
* The _simple JSON service_ that can be used to interact with the Mesos Kibana framework, e.g. start new Kibana instances.
* A [Mesos Kibana demo](https://github.com/Praqma/mesos-kibana-demo) project, that contain examples and the infrastructure used for testing. Use it to deploy a Mesos cluster using docker, to test and demonstrate the Mesos Kibana framework.

_Notice this readme explain current state of this repository (master branch), and may not match releases in all details. Check the readme file for specific releases_.

# Roadmap

## Features
- [ ] **Slave attribute handling**. _Improve attribute handling to support for example locations, so the kibana instance can run on the same network or physical location as the elasticsearch server_
- [ ] **UI status interface**. _A framework webpage with status over running kibana instances and known elasticsearch servers_.
- [ ] **JSON management service interface**. _The simple JSON service to use to interact with the kibana framework could support some kind of more user friendly interface_.

## Testing
- [ ] **Extended unit testing**. _Primarily using the Mesos test framework and best-practices for testing frameworks. Currently tests are very simple_.
- [ ] **Extended functional testing.** _More use-case oriented testing, for example verifying framework behaves as expected on requests to the JSON service_.
- [ ] **System testing, ELK on Mesos.** _An overall demo and test of ELK on Mesos, using the two other frameworks for [LogStash](https://github.com/mesos/logstash) and [Elasticsearch](https://github.com/mesos/elasticsearch) to provide a complete ELK stack running on Mesos_.

## Miscellaneous
- [ ] DCOS CLI support.

## CoDe

_Different continuous delivery improvements, mostly related to gradle tasks_.

- [ ] Deploy over ssh.
- [ ] Publish artifacts.
    - [ ] gradle publish to artifact repository (mesos official?)
- [ ] Extend deploy to use published artifacts.
- [ ] Support deploy on Marathon frameworks.


# Requirements

The `dependencies` section in the `build.gradle` file states the build dependencies, among other the Mesos version.

## Build system and dependencies

[Mesos Kibana demo](https://github.com/Praqma/mesos-kibana-demo) uses Docker version 1.7.1, docker-machine 0.3.0 , docker-compose 1.3.2.

Build are done on Ubuntu Linux 12.04 32-bit, while testing that uses docker are done on Ubuntu 14.04 64-bit.
To our best knowledge there is no specific dependency requirement to those platforms, except from tools used like Gradle (that requires a JVM) and docker tools.


# Pre-releases

During development the latest pre-release can be downloaded from our [mesos-kibana_release](http://code.praqma.net/ci/view/All/job/mesos-kibana_release) Jenkins  job artifact (or this [direct link](http://code.praqma.net/ci/view/All/job/mesos-kibana_release/lastSuccessfulBuild/artifact/build/libs/)).

# Deployment

From version 0.1.0.

## Deploying with gradle

We currently support Gradle deployment for Docker based Mesos clusters.
The Gradle task uses the settings defined in the `deploy.properties` file.
The default values match a Docker cluster created using `docker-compose` in the [kibana-mesos-demo](https://github.com/Praqma/mesos-kibana-demo) repository.

To deploy, simply run:
```
gradle deploy [-PverifyBuild]
```
The `-PverifyBuild` options makes Gradle use the latest build from the [mesos-kibana_verify](http://code.praqma.net/ci/job/mesos-kibana_verify/) job.

Note: Until an artifact management and repository system is in place, the latest release is only stored in the [mesos-kibana_release](http://code.praqma.net/ci/view/All/job/mesos-kibana_release) job on our CI server.

## Deploying manually
For manual deployment, simply copy the kibana jar file onto your Mesos master and run it.
```
java -jar /path-to/kibana.jar
```

### Launch options
```
-zk     -zookeeper       Mesos Zookeeper URL. (Required)
-p      -port            The TCP port for the webservice. (Default: 9001)
-mem	-requiredMem	 The amount of memory (in MB) to allocate to a single Kibana instance. (Default: 128)
-cpu	-requiredCpu	 The amount of CPUs to allocate to a single Kibana instance. (Default: 0.1)
-disk	-requiredDisk	 The amount of disk space (in MB) to allocate to a single Kibana instance. (Default: 25)
-es     -elasticsearch   URLs of ElasticSearch to start a Kibana for at startup.
```
### Example
With the `kibana.jar` in the `tmp` directory on the Mesos master, zookeeper with hostname `zookeeper` and one ElasticSearch instance running with IP `172.17.0.68` and the default port, the launch command would be:
```
java -jar /tmp/kibana.jar -zk zk://zookeeper:2181/mesos -es http://172.17.0.68:9200
```

# Management - interact with framework
Tasks can be managed through the _simple JSON service_ we have created.

## Starting and killing tasks
To spin up new, or kill off excess, Kibana instances you can `POST` a `TaskRequest` JSON object to the webservice at `task/request`.
A `TaskRequest` has an `elasticSearchUrl`, which points to the ES you want to spin up Kibana instances for, and a `delta`, which is the amount of Kibana tasks you want to start.

`POST` the following to `task/request` to spin up one new instance of Kibana, pointing to the given ES:
```json
{
    "elasticSearchUrl": "http://172.17.0.68:9200",
    "delta": 1
}
```

The procedure is the same for killing off excess tasks, except a negative delta should be given. Instances are killed off in LIFO fashion.

## Example
With a mesos-master with hostname mesosmaster and the above json snippet in a file called request.json, the following command:
```
curl -X POST http://mesosmaster:9001/task/request -d @request.json -H "Content-type: application/json"
```
would add an instance of kibana to mesos.


# Changelog


## 0.1.0

The second release of the Kibana framework:

* With gradle as build tool and gradle deploy tasks
* The _simple JSON service_ added to interact with the framework



## 0.0.1

The first version of the Kibana Framework for Mesos with limited functionality.


### Deployment on 0.0.1
Copy the kibana-0.0.1.jar file onto your Mesos master and execute:
```
java -jar /path-to/kibana-0.0.1.jar <mesos-master-uri>|<zookeeper-url/mesos> <elasticsearch-url>...
```

# Sponsors
This project is sponsored by Cisco Cloud Services.

# License
Apache License 2.0
