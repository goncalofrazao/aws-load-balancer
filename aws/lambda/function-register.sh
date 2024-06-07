#!/bin/bash

source ~/config.sh

aws iam create-role \
	--role-name lambda-role \
	--assume-role-policy-document '{"Version": "2012-10-17","Statement": [{ "Effect": "Allow", "Principal": {"Service": "lambda.amazonaws.com"}, "Action": "sts:AssumeRole"}]}'

sleep 5

aws iam attach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

sleep 5

aws lambda create-function \
	--function-name blurimage \
	--zip-file fileb:///Users/goncalofrazao/cloud-computing/project/cnv-24-gxx/webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler \
	--runtime java11 \
	--timeout 60 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role

aws lambda create-function \
	--function-name enhanceimage \
	--zip-file fileb:///Users/goncalofrazao/cloud-computing/project/cnv-24-gxx/webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler \
	--runtime java11 \
	--timeout 60 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role

aws lambda create-function \
	--function-name raytracer \
	--zip-file fileb:///Users/goncalofrazao/cloud-computing/project/cnv-24-gxx/webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler \
	--runtime java11 \
	--timeout 60 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role
