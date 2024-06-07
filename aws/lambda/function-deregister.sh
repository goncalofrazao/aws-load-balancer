#!/bin/bash

source ~/config.sh

aws lambda delete-function --function-name blurimage
aws lambda delete-function --function-name enhanceimage
aws lambda delete-function --function-name raytracer

aws iam detach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

aws iam delete-role --role-name lambda-role
