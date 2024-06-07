# Checkpoint

## Warning

All the commands assume you are in the project directory unziped

## Javassist tool to use

The code will generate a log file called ```log``` in the directory where invoked.
- The tool to be used is ```ICount```

## How to launch the webserver in aws

- must add the keypair and a config file with credentials of aws (with names ```mykeypair.pem``` and ```config.sh``` respectively) to the aws directory.
- ```cd aws && ./create.sh``` to create AMI.
- ```cd aws && ./launch.sh``` to launch the load balancer with auto scaler.
- ```cd aws && ./terminate.sh``` to terminate the deployment and clean all resources in usage.

## How to run the webserver locally

### Compile

- To compile the javassist: ```cd javassist && mvn clean package```.
- To compile the webserver: ```cd server && mvn clean package```.
- The jar files will be created in ```javassist/target/``` and ```server/webserver/target/```.

### Run instructions

- You must run the following commands by order. If you want to run the server again, you only need to run the last command.
- You must adapt the jar paths in the following commands if you are not in the main directory of the submitted zip 

### Run without javassist

```bash
cp server/webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar .
```
```bash
java -cp webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.webserver.WebServer
```

### Run with javassist

```bash
cp server/webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar .
```
```bash
cp javassist/target/JavassistWrapper-1.0-jar-with-dependencies.jar .
```
```bash
java -cp webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar -javaagent:JavassistWrapper-1.0-jar-with-dependencies.jar=ICount:pt:output pt.ulisboa.tecnico.cnv.webserver.WebServer
```

# Final submission

## Code Organization

- Code is divided in 5 directories
- aws directory has the shell scripts to create the instances image and create lambda functions
- cnv-24-gxx is the default webserver given by teachers that is used for the lambdas
- javassist is the javaagent project with the instrumentation code using the dynamoDB
- loadbalancer is the project with our own loadbalancer and autoscaler implementations
- server is the webserver given by teachers with some modifications to save the instrumented code

## Loadbalancer observations

- To run the loadbalancer you must source the config.sh file
- To run the loadbalancer you must pass as a argument the image id of the instances to balance
