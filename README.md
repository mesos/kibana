# mesos-kibana
Coming soon!
Kibana Framework for Mesos.

## Pre-releases
During development the latest pre-release can be downloaded [here](http://code.praqma.net/ci/view/Mesos_Kibana/job/mesos-kibana_release/lastSuccessfulBuild/artifact/build/libs/).

## Versions
### Snapshot
The next version of the Kibana, currently under developent.
#### Deployment
Copy the kibana jar file onto your Mesos master and run it.
```
java -jar /path-to/kibana.jar
```
##### Launch options
```q
-m      -master          Mesos Master URI.
-zk     -zookeeper       Mesos Zookeeper URL.
-p      -port            The TCP port for the webservice. Defaults to 9001.
-es     -elasticsearch   URLs of ElasticSearch to start a Kibana for at startup.
```
Note: Either `-m` or `-zk` must be provided at startup.
#### Management
Tasks can be managed through the JSON API.
##### Starting and killing tasks
To spin up new or kill off excess Kibana tasks you can `POST` a `TaskRequest` JSON object to the webservice at `task/request`.
A `TaskRequest` has an `elasticSearchUrl`, which points to the ES you want to spin up Kibana instances for, and a `delta`, which is the amount of Kibana tasks you want to start.

`POST` the following to `task/request` to spin up one new instance of Kibana, pointing to the given ES:
```json
{
    "elasticSearchUrl": "http://db.fancytech.com:9200",
    "delta": 1
}
```
The procedure is the same for killing off excess tasks, except a negative delta should be given. Instances are killed off in LIFO fashion.

### 0.0.1
The first version of the Kibana Framework for Mesos with limited functionality.
#### Deployment
Copy the kibana-0.0.1.jar file onto your Mesos master and execute:
```
java -jar /path-to/kibana-0.0.1.jar <mesos-master-uri>|<zookeeper-url/mesos> <elasticsearch-url>...
```
# Sponsors
This project is sponsored by Cisco Cloud Services

# License
Apache License 2.0
