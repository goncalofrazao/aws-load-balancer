#!/bin/bash

source /Users/goncalofrazao/config.sh

TABLE_NAME="RequestMetrics"

aws dynamodb scan --table-name $TABLE_NAME --attributes-to-get "RequestType" "RequestId" --query "Items[*]" --output json | jq -r ".[] | tojson" | while read i; do
    aws dynamodb delete-item --table-name $TABLE_NAME --key "$i"
done