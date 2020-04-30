import csv
import datetime
import os
import sys

import click

from platon_utility import generate_key, write_csv

@click.command()
@click.option('-s', '--startindex', default=1, help='Start the subscript.')
@click.option('-m', '--nodenumber', default=110, help='Number of nodes.')
@click.option('-n', '--addressnumber', default=10000, help='The number of addresses generated per node, default is 10000.')
@click.option('-d', '--designation', default="", help='name of the node.')
def batch_generate_prikey(startindex, nodenumber, addressnumber, designation):
    if startindex <= 0:
        print("startindex must be greater than 0.")
        exit(1)

    # 节点个数
    if nodenumber <= 0:
        print("nodenumber must be greater than 0.")
        exit(1)

    # 钱包文件名称
    if addressnumber <= 0:
        print("addressnumber must be greater than 0.")
        exit(1)

    # 钱包文件名称
    if designation == "":
        designation = "node"

    csvPath = os.getcwd()

    # 创建保存私钥文件目录
    csvPath = os.path.join(csvPath, designation)
    if not os.path.exists(csvPath):
        os.makedirs(csvPath)

    # 根据账户名称生成批量生成钱包
    try:
        nIndex = 0
        all_node_prikey = []
        for n in range(0, nodenumber):
            nIndex = nIndex + 1
            node_name = designation + "_%d" % startindex
            startindex = startindex + 1
            # 保存私钥记录（csv）
            all_prikey_per_node = []
            # 轮询生成
            for i in range(0, addressnumber):
                address, prikey = generate_key()
                all_prikey_per_node.append({"address": address.lower(), "private": prikey})

            if addressnumber == 1 and nodenumber != 1:
                all_node_prikey.append(all_prikey_per_node[0])

            # 保存钱包地址记录（csv）
            privatekey_csv_file = os.path.join(csvPath, node_name + ".csv")

            write_csv(privatekey_csv_file, all_prikey_per_node)
            print('The 【{}】 wallet private key file was generated successfully：{}'.format(node_name, privatekey_csv_file))

        if addressnumber == 1 and nodenumber != 1:
            all_privatekey_csv_file = os.path.join(csvPath, designation + ".csv")
            write_csv(all_privatekey_csv_file, all_node_prikey)
            print('The 【{}】 wallet private key file was generated successfully：{}'.format(designation,
                                                                                          all_privatekey_csv_file))
    except Exception as e:
        print('{} {}'.format('exception: ', e))
        print('batch create wallet failure!!!')
        sys.exit(1)

    else:
        print('{}{} {}'.format('SUCCESS\n', "Batch generation of wallet private key files in:", csvPath))
        end = datetime.datetime.now()
        print("end：{}".format(end))
        print("总共消耗时间：{}s".format((end - start).seconds))
        sys.exit(0)

if __name__ == "__main__":
    start = datetime.datetime.now()
    print("start：{}".format(start))
    batch_generate_prikey()
