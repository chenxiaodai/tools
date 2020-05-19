## 批量生成私钥文件和批量转账

### 环境准备

- Ubuntu 18.04.2 LTS  （建议cpu 8核心+  内存 16G）
- python：3.6+
- pip3

依次执行命令：

> - 安装python3
>
> ```shell
> if ! command -v python3 >/dev/null 2>&1; then  sudo apt-get update && sudo apt-get install python3.6; fi;
> ```
>
> - 安装pip3
>
> ```shell
> if ! command -v pip3 >/dev/null 2>&1; then sudo apt install python3-pip; fi;
> ```
>
> - 安装依赖库
> ```shell
> pip3 install -r requirements.txt
> ```
>
> > `requirements.txt`为需要安装的依赖库列表文件，在当前目录下。


### 1 修改配置文件
a. 修改config.json
```
{
	"chainId":"101",                           //链id
	"nodeAddress":"http://127.0.0.1",          //rpc主机
	"nodeRpcPort":"6789"                       //rpc端口
}

```

### 2 准备中间账户
a.  创建总账户
```shell
python3 batch_generate_privatekey.py -m 1 -n 1 -d "main"

```
> 将在当前目录生成一个main的子目录，此目录下有1个main_1.csv，main_1.csv中的address即为总账户地址，提供给财务。等待财务转账。  申请金额为：130,130,010‬‬ LAT‬ =( 100(每个账户余额) * 10000（每个节点账户数） + 1000（转账手续费）)（每个节点金额） * 130（节点数） + 10（转账手续费）  


b.  创建节点账户
```shell
python3 batch_generate_privatekey.py -m 130 -n 1 -d "multi"

```
> 将在当前目录生成一个transfer的子目录，此目录下有130个`multi_*.csv`和1个`multi.csv`，`multi_*.csv`和`multi.csv`中的address即为各个节点的转账地址。

c. 总账户到节点账户转账签名文件生成
```shell
python3 batch_generate_signed_transfer.py -f ./main/main_1.csv -t ./multi/multi.csv -a 1001000 -c ./config.json
```
> 给每个节点账户转账1001000LAT。执行成功后会生成一个签名后的转账交易列表文件：signed_batch_transfer_multi.csv。

d. 总账户到节点账户转账
```shell
python3 batch_transfer_send.py -f ./signed_batch_transfer_to_1.csv -c ./config.json
```
> 如果中途发生连接节点中断等情况导致发送交易中断，可重复执行上述命令。

e. 总账户到节点账户转账检查
```shell
python3 batch_get_account_balance.py -f ./multi/multi.csv -c ./config.json | grep amount:1001000LAT | wc -l
```
> 匹配记录数为130个代表成功


### 3 生成压测钱包
a. 并行执行生成钱包命令，在4核cpu下面预计50分钟。
```shell
nohup python3 batch_generate_privatekey.py -s 1 -m 10 -n 10000 -d "node"  &
nohup python3 batch_generate_privatekey.py -s 11 -m 10 -n 10000 -d "node" &
nohup python3 batch_generate_privatekey.py -s 21 -m 10 -n 10000 -d "node" &
nohup python3 batch_generate_privatekey.py -s 31 -m 10 -n 10000 -d "node" &
nohup python3 batch_generate_privatekey.py -s 41 -m 10 -n 10000 -d "node" &
nohup python3 batch_generate_privatekey.py -s 51 -m 10 -n 10000 -d "node" &
nohup python3 batch_generate_privatekey.py -s 61 -m 10 -n 10000 -d "node" &
nohup python3 batch_generate_privatekey.py -s 71 -m 10 -n 10000 -d "node" &
nohup python3 batch_generate_privatekey.py -s 81 -m 10 -n 10000 -d "node" &
nohup python3 batch_generate_privatekey.py -s 91 -m 10 -n 10000 -d "node" &
nohup python3 batch_generate_privatekey.py -s 101 -m 10 -n 10000 -d "node" &
nohup python3 batch_generate_privatekey.py -s 111 -m 10 -n 10000 -d "node" &
nohup python3 batch_generate_privatekey.py -s 121 -m 10 -n 10000 -d "node" &
```
> 此过程将在当前目录生成一个node的子目录，在此子目录下生成130个`node_*.csv`文件，每个`node_*.csv`私钥文件下，有10000个账户地址。可以通过命令检查生成文件数， ls -l node/ |grep "^-"|wc -l


### 4 给压测钱包转账
a. 批量生成130个节点的签名文件
```shell
chmod +x batch_signed.sh && ./batch_signed.sh 1 130 100
```
> 此过程会生成`signed_batch_transfer_*.csv`文件，其中`*`表示接收交易账户私钥文件的文件名，如：`signed_batch_transfer_node_1.csv`。其中1为文件开始下标，130为文件结束下标。每个账户转账100LAT

b. 批量发送
```shell
chmod +x batch_send.sh && ./batch_send.sh 1 130
```

c. 批量查询
```shell
chmod +x batch_query.sh && ./batch_query.sh 130

如果输出中存在 wallets failed字段，重新执行批量发送
```
> 

### 参考：命令行介绍
#### 批量生成私钥文件工具说明

命令：

```shell
python3 batch_generate_privatekey.py -s startindex -m nodenumber -n addressnumber -d designation
```

> 说明：
>
> - startindex：私钥文件(csv)的开始后缀，如此值为3，则第一个节点生成的csv为：XXX_3.csv；默认为1。
> - nodenumber：表示节点的数量，默认为110。
> - addressnumber：表示每个节点的地址账户数量，默认为10000。
> - designation：表示节点名描述，执行完成后，会在当前目录创建一个以designation为名的子目录，所有的私钥文件保存在此目录下；默认为“node”。

#### 批量生成转账交易签名文件工具说明

命令：

```shell
python3 batch_generate_signed_transfer.py -f fromprivate -t tolist -n nonce -a amount -c config
```

> 说明：
>
> - fromprivate：发送交易账户对应的私钥文件
>
> - tolist：接收交易账户对应的私钥文件
>
> - nonce：签名批量转账交易的起始nonce，默认为0，如果为0，会通过rpc接口实时查询实际的nonce值；此值主要用于多线程分批跑的情况。
>
> - amount：转账金额，单位：LAT。
>
> - config：配置文件，包括chainid，ip以及rpc端口号，根据实际情况进行修改，模板如下：
>
>   ```json
>   {
>   	"chainId":"101",
>   	"nodeAddress":"http://127.0.0.1",
>   	"nodeRpcPort":"6789"
>   }
>   ```



#### 批量发送转账交易工具说明

命令：
```shell
python3 batch_transfer_send.py -f txfile -c config
```

说明：
> txfile：已签名的交易列表文件。
>
> config：配置文件，包括chainid，ip以及rpc端口号，根据实际情况进行修改。



#### 批量查询转账余额工具说明

命令：

```shell
python3 batch_get_account_balance.py -f accountsfile -c config
```

说明：

> accountsfile：接收交易账户列表对应的私钥文件。同tolist。
>
> config：配置文件，包括chainid，ip以及rpc端口号，根据实际情况进行修改。
