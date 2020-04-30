#!/bin/bash
if [ -z $1 ]; then
	echo "please input start index!!!"
	exit 1
fi

if [ -z $2 ]; then
	echo "please input end index!!!"
	exit 1
fi

if [ $1 -lt 0 ]; then
	echo "The starting index must be greater than or equal to 0!!!"
	exit 1
fi

if [ $1 -gt $2 ]; then
	echo "The starting index cannot be greater than the ending index!!!"
	exit 1
fi

for ((i=$1;i<=$2;i++))
do
{
	cmd="python3 batch_transfer_send.py -f ./signed_batch_transfer_node_$i.csv -c ./config.json"
	echo $cmd

	$cmd
	if [ $? -ne 0 ];then
		echo ""
		echo -e "\033[;31mexec command:【$cmd】failed...\033[0m"
	else
		echo -e "\033[;32mexec command:【$cmd】succeed...\033[0m"
	fi
 }&      
done    
wait 

