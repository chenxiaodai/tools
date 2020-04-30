import csv
import os
import time

from eth_keys import keys
from eth_utils import (
    keccak,
)
from eth_utils.curried import (
    text_if_str,
    to_bytes,
)


def generate_key():
    """
    生成节点公私钥
    :return:
        私钥
        公钥
    """
    extra_entropy = ''
    extra_key_bytes = text_if_str(to_bytes, extra_entropy)
    key_bytes = keccak(os.urandom(32) + extra_key_bytes)
    privatekey = keys.PrivateKey(key_bytes)
    pubKey = keys.private_key_to_public_key(privatekey)
    address = pubKey.to_address()
    # return address, privatekey.to_hex()[2:], keys.private_key_to_public_key(privatekey).to_hex()[2:]
    return address, privatekey.to_hex()[2:]


def get_time_stamp():
    '''
    获取时间戳
    :return:
    时间戳字符串：如：20200428092745170
    '''
    ct = time.time()
    local_time = time.localtime(ct)
    data_head = time.strftime("%Y-%m-%d %H:%M:%S", local_time)
    data_secs = (ct - int(ct)) * 1000
    time_stamp = "%s.%03d" % (data_head, data_secs)
    print(time_stamp)
    stamp = ("".join(time_stamp.split()[0].split("-")) + "".join(time_stamp.split()[1].split(":"))).replace('.', '')
    return stamp

def write_csv(file_name: str, dict_list: list):
    """将字典列表数据写进csv文件

    Args:
        file_name:  要写入的文件名称
        dict_list： 字典列表

    Raises:
        Exception： 写入文件不是以.csv为后缀，抛出异常
        :param file_name:
        :param dict_list:
    """
    if not file_name.endswith(".csv"):
        raise Exception("File format error")
    with open(file_name, "w", encoding="utf-8", newline='') as f:
        csv_write = csv.writer(f)
        csv_head = list(dict_list[0].keys())
        csv_write.writerow(csv_head)
        for one_dict in dict_list:
            csv_value = list(one_dict.values())
            csv_write.writerow(csv_value)


def read_csv(file_name) -> list:
    """从csv文件中获取字典列表数据

    Args:
        file_name:  csv文件名称

    Returns:
        list：字典列表数据

    Raises:
        Exception： 写入文件不是以.csv为后缀，抛出异常
    """
    if not file_name.endswith(".csv"):
        raise Exception("File format error")
    transaction_list = []
    with open(file_name) as csvfile:
        csv_reader = csv.reader(csvfile)
        header = next(csv_reader)
        for row in csv_reader:
            transaction_dict = dict(zip(header, row))
            transaction_list.append(transaction_dict)
    return transaction_list


def transaction_str_to_int(transaction_dict: dict):
    """转换交易格式

    Args:
        transaction_dict:  交易字典

    Returns:
        dict：转换后的交易字典
    """
    for key, value in transaction_dict.items():
        if key in ["value", "gasPrice", "gas", "nonce", "chainId"]:
            transaction_dict[key] = int(value)
    return transaction_dict
