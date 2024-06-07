#!/bin/bash

source ~/config.sh

# Variable
AUTO_SCALING_GROUP_NAME="CNV-AutoScalingGroup"

# List all scaling policies and extract the policy names
POLICY_NAMES=$(aws autoscaling describe-policies --auto-scaling-group-name "$AUTO_SCALING_GROUP_NAME" | jq -r '.ScalingPolicies[].PolicyName')

# Iterate over the policy names and delete each policy
for POLICY_NAME in $POLICY_NAMES; do
    aws autoscaling delete-policy --auto-scaling-group-name "$AUTO_SCALING_GROUP_NAME" --policy-name "$POLICY_NAME"
    echo "Deleted policy: $POLICY_NAME"
done
