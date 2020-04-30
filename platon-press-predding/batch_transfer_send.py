import datetime
import json
import sys


import click
from client_sdk_python import Web3
from client_sdk_python.eth import Eth
from client_sdk_python.providers import HTTPProvider
from platon_utility import read_csv, transaction_str_to_int

# 发送交易
def transfer_send(url, signdata) -> tuple:
    w3 = Web3(HTTPProvider(url))
    platon = Eth(w3)
    platon.sendRawTransaction(signdata)
    #tx_hash = HexBytes(platon.sendRawTransaction(signdata)).hex()
    # res = platon.waitForTransactionReceipt(tx_hash)
    #return tx_hash

@click.command()
@click.option('-f', '--txfile', default='', help='Transaction list file.')
@click.option('-c', '--config', default='', help='Send the node configuration information for the transaction.')

def batch_send_transfer(txfile, config):
    # 交易文件列表
    if txfile == "":
        print("-f/--txfile is null, please input.")
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

        # 获取交易列表
        transaction_list_raw = read_csv(txfile)
        transaction_list = []
        for one_transaction in transaction_list_raw:
            transaction_list.append(transaction_str_to_int(one_transaction))

        # 发送交易
        print('\nstart to send transfer transaction, please wait...\n')
        for one_transaction in transaction_list:
             try:
                transfer_send(url, one_transaction["rawTransaction"])
             except Exception as e:
                 continue
    except Exception as e:
        print('{} {}'.format('exception: ', e))
        print('batch send transfer transaction failure!!!')
        sys.exit(1)
    else:
        print('batch send transfer transaction SUCCESS.')
        end = datetime.datetime.now()
        print("end：{}".format(end))
        print("总共消耗时间：{}s".format((end - start).seconds))
        sys.exit(0)

if __name__ == "__main__":
    start = datetime.datetime.now()
    print("start：{}".format(start))
    batch_send_transfer()