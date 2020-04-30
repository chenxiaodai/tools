import datetime
import json
import os
import sys

import click
from hexbytes import HexBytes
from platon_utility import write_csv, read_csv, get_time_stamp

from client_sdk_python import Web3
from client_sdk_python.eth import Eth
from client_sdk_python.providers import HTTPProvider


# 生成未签名交易
def transfer_sign(from_address, to_address, value, nonce, gas_price, from_prikey, chain_id) -> dict:
    transaction_dict = {
        "to": Web3.toChecksumAddress(to_address),
        "gasPrice": gas_price,
        "gas": 21000,
        "nonce": nonce,
        "chainId": chain_id,
        "value": value,
        'from': Web3.toChecksumAddress(from_address),
    }

    ret = Eth.account.signTransaction(transaction_dict, from_prikey)
    transaction_dict.update({'rawTransaction': HexBytes(ret.rawTransaction).hex()})
    return transaction_dict


@click.command()
@click.option('-f', '--fromprivate', default='', help='Send the wallet private key file for the transaction.')
@click.option('-t', '--tolist', default='', help='A list of addresses to receive transactions.')
@click.option('-n', '--nonce', default=0, help='To start sending the starting nonce of the trading account, '
                                               'it is not necessary to enter parameters. '
                                               'If you do not enter the default value of 0, it is obtained from the '
                                               'RPC interface.')
@click.option('-a', '--amount', default='', help='transfer amount, The unit is LAT.')
@click.option('-c', '--config', default='', help='Send the node configuration information for the transaction.')
def signed_batch_transfer(fromprivate, tolist, nonce, amount, config):
    # 发交易钱包文件
    if fromprivate == "":
        print("-f/--fromprivate is null, please input.")
        exit(1)

    # 接收交易地址列表文件
    if tolist == "":
        print("-t/--tolist is null, please input.")
        exit(1)
    else:
        if not tolist.endswith(".csv"):
            raise Exception("File format error")

    # 转账金额
    if amount == "":
        print("-a/--amount is null, please input.")
        exit(1)

    # 发送交易的节点配置信息
    if config == "":
        print("-c/--config is null, please input.")
        exit(1)

    try:
        # 获取转出地址
        address_prikey_list = read_csv(fromprivate)
        if len(address_prikey_list) == 0:
            print("The wallet private key file that sent the transaction is empty, please check!")
            exit(1)
        from_address = address_prikey_list[0]["address"]
        from_prikey = address_prikey_list[0]["private"]
        # 获取节点 url
        with open(config, 'r') as load_f:
            config_info = json.load(load_f)
            url = config_info['nodeAddress'] + ":" + config_info['nodeRpcPort']
            chainId = int(config_info['chainId'])
        # 和节点建立连接
        w3 = Web3(HTTPProvider(url))
        platon = Eth(w3)
        gasPrice = platon.gasPrice

        # 转账金额(LAT转换成VON)
        value = w3.toWei(amount, "ether")
        print("transfer amount:{}VON".format(value))

        if nonce == 0:
            nonce = platon.getTransactionCount(Web3.toChecksumAddress(from_address))

        # 获取转账 to 钱包名称
        all_transaction = []
        rows = read_csv(tolist)
        for row in rows:
            if 'address' in row:
                to_address = row["address"]
            else:
                raise Exception("The address field does not exist in the address file!")

            one_transaction_data = transfer_sign(from_address, to_address, value, nonce, gasPrice,
                                                 from_prikey, chainId)
            all_transaction.append(one_transaction_data)
            nonce += 1

        # 生成 csv 文件
        #stamp = get_time_stamp()
        suffix = os.path.basename(tolist)
        transaction_file_name = "signed_batch_transfer_" + suffix
        write_csv(transaction_file_name, all_transaction)

    except Exception as e:
        print('{} {}'.format('exception: ', e))
        print('generate unsigned transaction file failure!!!')
        sys.exit(1)

    else:
        csvPath = os.getcwd() + "\\" + transaction_file_name
        print('{}{} {}'.format('SUCCESS\n', "generate unsigned transaction file:", csvPath))
        end = datetime.datetime.now()
        print("end：{}".format(end))
        print("总共消耗时间：{}s".format((end - start).seconds))
        sys.exit(0)

if __name__ == "__main__":
    start = datetime.datetime.now()
    print("start：{}".format(start))
    signed_batch_transfer()
