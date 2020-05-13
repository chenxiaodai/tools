#!/bin/bash

if [ -z $1 ]; then
	echo "please input node number"
	exit 1
fi

for ((i=1;i<=$1;i++)); do
    {
    num=$(python3 batch_get_account_balance.py -f ./node/node_$i.csv -c ./config.json | grep amount:0LAT | wc -l)
    if [[ $num -eq 0 ]]; then
        echo "node_$i.csv Transfer wallets succeed"
    else
        echo "node_$i.csv $num wallets failed"
    fi
    }&
done
wait
