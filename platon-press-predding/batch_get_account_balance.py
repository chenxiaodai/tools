import datetime
import json
import sys

import click
from client_sdk_python import Web3
from client_sdk_python.eth import Eth
from client_sdk_python.providers import HTTPProvider
from platon_utility import read_csv, transaction_str_to_int

@click.command()
@click.option('-f', '--accountsfile', default='', help='Node account list file.')
@click.option('-c', '--config', default='', help='Send the node configuration information for the transaction.')
def batch_send_transfer(accountsfile, config):
    # 交易文件列表
    if accountsfile == "":
        print("-f/--accountsfile is null, please input.")
        exit(1)

    # 发送交易的节点配置信息
    if config == "":
        print("-c/--config is null, please input.")
        exit(1)

    try:
        # 获取节点 url
        with open(config, 'r') as load_f:
            config_info = json.load(load_f)
            url = config_info['nodeAddress'] + ":" + config_info['nodeRpcPort']

        w3 = Web3(HTTPProvider(url))
        platon = Eth(w3)
        # 获取交易列表
        rows = read_csv(accountsfile)
        for row in rows:
            if 'address' in row:
                address = row["address"]
            else:
                raise Exception("The address field does not exist in the address file!")

            amount = platon.getBalance(Web3.toChecksumAddress(address))
            amount = w3.fromWei(amount, "ether")

            print("address:{}, amount:{}LAT".format(address, amount))

    except Exception as e:
        print('{} {}'.format('exception: ', e))
        print('batch get account balance failure!!!')
        sys.exit(1)
    else:
        print('batch get account balance  SUCCESS.')
        end = datetime.datetime.now()
        print("end：{}".format(end))
        print("总共消耗时间：{}s".format((end - start).seconds))
        sys.exit(0)

if __name__ == "__main__":
    start = datetime.datetime.now()
    print("start：{}".format(start))
    batch_send_transfer()
