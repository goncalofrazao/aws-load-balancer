#!/bin/bash

source ~/config.sh

snapshot_ids=$(aws ec2 describe-snapshots --owner-ids self | jq -r '.Snapshots[].SnapshotId')

for snapshot_id in $snapshot_ids
do
    aws ec2 delete-snapshot --snapshot-id $snapshot_id
done