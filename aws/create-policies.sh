#!/bin/bash

source ~/config.sh

AUTO_SCALING_GROUP_NAME="CNV-AutoScalingGroup"
ADJUSTMENT_TYPE="ChangeInCapacity"
METRIC_NAME="CPUUtilization"
NAMESPACE="AWS/EC2"
PERIOD=60
EVALUATION_PERIODS=1

# create decrease policy
INCREASE_POLICY_ARN=$(aws autoscaling put-scaling-policy \
	--auto-scaling-group-name $AUTO_SCALING_GROUP_NAME \
	--policy-name IncreaseInstanceCount \
	--adjustment-type $ADJUSTMENT_TYPE \
	--scaling-adjustment 1 \
	| jq -r .PolicyARN)

# create increase policy
DECREASE_POLICY_ARN=$(aws autoscaling put-scaling-policy \
	--auto-scaling-group-name $AUTO_SCALING_GROUP_NAME \
	--policy-name DecreaseInstanceCount \
	--adjustment-type $ADJUSTMENT_TYPE \
	--scaling-adjustment -1 \
	| jq -r .PolicyARN)

aws cloudwatch put-metric-alarm \
    --alarm-name HighCPUAlarm \
    --metric-name $METRIC_NAME \
    --namespace $NAMESPACE \
    --statistic Average \
    --period $PERIOD \
    --evaluation-periods $EVALUATION_PERIODS \
    --threshold 60 \
    --comparison-operator GreaterThanThreshold \
    --dimensions Name=AutoScalingGroupName,Value=$AUTO_SCALING_GROUP_NAME \
    --alarm-actions $INCREASE_POLICY_ARN

aws cloudwatch put-metric-alarm \
	--alarm-name LowCPUAlarm \
	--metric-name $METRIC_NAME \
	--namespace $NAMESPACE \
	--statistic Average \
	--period $PERIOD \
	--evaluation-periods $EVALUATION_PERIODS \
	--threshold 20 \
	--comparison-operator LessThanThreshold \
	--dimensions Name=AutoScalingGroupName,Value=$AUTO_SCALING_GROUP_NAME \
	--alarm-actions $DECREASE_POLICY_ARN

