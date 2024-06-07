#!/bin/bash

source ~/config.sh

# List all CloudWatch alarms and extract the alarm names
ALARM_NAMES=$(aws cloudwatch describe-alarms | jq -r '.MetricAlarms[].AlarmName')

# Iterate over the alarm names and delete each alarm
for ALARM_NAME in $ALARM_NAMES; do
    aws cloudwatch delete-alarms --alarm-names "$ALARM_NAME"
    echo "Deleted alarm: $ALARM_NAME"
done
