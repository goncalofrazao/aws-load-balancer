#!/bin/bash

source ~/config.sh

snapshot_id=$(aws ec2 describe-images --image-ids $(cat image.id) | jq -r '.Images[0].BlockDeviceMappings[0].Ebs.SnapshotId')

aws ec2 deregister-image --image-id $(cat image.id)
aws ec2 delete-snapshot --snapshot-id $snapshot_id
