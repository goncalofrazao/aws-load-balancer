#!/bin/bash

source ~/config.sh

# Install java.
cmd="sudo yum update -y; sudo yum install java-11-amazon-corretto.x86_64 -y;"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# Install web server.
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH /Users/goncalofrazao/cloud-computing/project/server/webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar ec2-user@$(cat instance.dns):
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH /Users/goncalofrazao/cloud-computing/project/javassist/target/JavassistWrapper-1.0-jar-with-dependencies.jar ec2-user@$(cat instance.dns):
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH /Users/goncalofrazao/config2.sh ec2-user@$(cat instance.dns):
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH /Users/goncalofrazao/mykeypair.pem ec2-user@$(cat instance.dns):

# Build web server.
# cmd="cd cnv-24-g01 && mvn clean package"
# ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd 

# Setup web server to start on instance launch.
# cmd="echo \"java -cp /home/ec2-user/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.webserver.WebServer\" | sudo tee -a /etc/rc.local; sudo chmod +x /etc/rc.local"
cmd="echo \"source /home/ec2-user/config2.sh && java -cp /home/ec2-user/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar -javaagent:/home/ec2-user/JavassistWrapper-1.0-jar-with-dependencies.jar=ICount:pt:output pt.ulisboa.tecnico.cnv.webserver.WebServer\" | sudo tee -a /etc/rc.local; sudo chmod +x /etc/rc.local"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd
