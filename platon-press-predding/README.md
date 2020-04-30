## 批量生成私钥文件和批量转账

### 环境准备

- Ubuntu系统
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



### 批量生成私钥文件工具说明

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

### 批量生成转账交易签名文件工具说明

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



### 批量发送转账交易工具说明

命令：
```shell
python3 batch_transfer_send.py -f txfile -c config
```

说明：
> txfile：已签名的交易列表文件。
>
> config：配置文件，包括chainid，ip以及rpc端口号，根据实际情况进行修改。



### 批量查询转账余额工具说明

命令：

```shell
python3 batch_get_account_balance.py -f accountsfile -c config
```

说明：

> accountsfile：接收交易账户列表对应的私钥文件。同tolist。
>
> config：配置文件，包括chainid，ip以及rpc端口号，根据实际情况进行修改。



### 具体操作步骤

#### **背景**
  - 压测活动要求给110个节点转账，每个节点有10000个账户。
  - 考虑到如果只用一个总账户给110个节点转账，会有批量转账时，中途失败，导致nonce不连续，排查难度较大的问题；所以采用一个账户对应一个节点的方式，进行批量转账。
####  **操作**

 - 修改config配置文件

```json
{
	"chainId":"101",
	"nodeAddress":"http://127.0.0.1",
	"nodeRpcPort":"6789"
}
```

> chainid，ip以及rpc端口号，根据实际情况进行修改。

  - 生成一个总账户
      - 生成私钥文件
      ```shell
        python3 batch_generate_privatekey.py -m 1 -n 1 -d "main"
      ```
      > 将在当前目录生成一个main的子目录，此目录下有1个main_1.csv，main_1.csv中的address即为总账户地址，提供给财务。
      - 等待财务转账
      > 总账户的金额到账后，即可进行下面的步骤。

      

 - 将总账户的金额平均分到110个节点的转账账户
    - 生成110个转账账户私钥文件

    ```shell
    python3 batch_generate_privatekey.py -m 110 -n 1 -d "multi"
    ```

    > 将在当前目录生成一个transfer的子目录，此目录下有110个`multi_*.csv`和1个`multi.csv`，`multi_*.csv`和`multi.csv`中的address即为各个节点的转账地址。

    - 生成转账签名文件

    ```shell
    python3 batch_generate_signed_transfer.py -f ./main/main_1.csv -t ./multi/multi.csv -a amount -c ./config.json
    ```

    > 其中`amount`需要改为实际值。执行成功后会生成一个签名后的转账交易列表文件：signed_batch_transfer_multi.csv。

    - 发起转账
    ```shell
    python3 batch_transfer_send.py -f ./signed_batch_transfer_multi.csv -c ./config.json
    ```
    > 如果中途发生连接节点中断等情况导致发送交易中断，可重复执行上述命令。

  - 生成110个节点的10000账户的私钥文件

```shell
python3 batch_generate_privatekey.py -s 1 -m 110 -n 10000 -d "node"
```

> 此过程需要耗费较长时间，可以通过控制`-s`的值，开多个终端并行执行，比如：
>
> python3 batch_generate_privatekey.py -s 1 -m 10 -n 10000 -d "node"
>
> python3 batch_generate_privatekey.py -s 11 -m 10 -n 10000 -d "node"
>
> python3 batch_generate_privatekey.py -s 21 -m 10 -n 10000 -d "node"
>
> ...
>
> python3 batch_generate_privatekey.py -s 101 -m 10 -n 10000 -d "node"
>
> 此过程将在当前目录生成一个node的子目录，在此子目录下生成110个`node_*.csv`文件，每个`node_*.csv`私钥文件下，有10000个账户地址。

  - 生成转账签名文件：单个节点的批量转账签名

```shell
python3 batch_generate_signed_transfer.py -f ./multi/multi_1.csv -t ./node/node_1.csv -a amount -c ./config.json
.
.
.
python3 batch_generate_signed_transfer.py -f ./multi/multi_110.csv -t ./node/node_110.csv -a amount -c ./config.json
```

> - 其中`amount`为转账金额，需要改为实际值。
>
> - 此过程只支持单个节点的批量转账。对应生成`signed_batch_transfer_*.csv`文件，其中`*`表示接收交易账户私钥文件的文件名，如：`signed_batch_transfer_node_1.csv`。
>
> - **如果使用多线程执行所有的命令，可执行：**
>
>   ```shell
>   chmod +x batch_signed.sh && ./batch_signed.sh 1 110 amount
>   ```
>   其中1为文件开始下标，110为文件结束下标。

- 检查转账账户的余额

```
python3 batch_get_account_balance.py -f ./multi/multi.csv -c ./config.json
```

> 如果转账账户的余额已到账，即可进行如下的转账操作。

  - 批量发送转账交易：单个节点的批量转账

```shell
python3 batch_transfer_send.py -f ./signed_batch_transfer_node_1.csv -c ./config.json
.
.
.
python3 batch_transfer_send.py -f ./signed_batch_transfer_node_110.csv -c ./config.json
```

> - 如果中途发生连接节点中断等情况导致发送交易中断，可重复执行上述命令。
>
> - **如果使用多线程执行所有的命令，可执行：**
>   ```shell
>   chmod +x batch_send.sh && ./batch_send.sh 1 110
>   ```
>   其中1为文件开始下标，110为文件结束下标。

  - 批量查询转账余额：单个节点的账户余额

```shell
python3 batch_get_account_balance.py -f ./node/node_1.csv -c ./config.json
.
.
.
python3 batch_get_account_balance.py -f ./node/node_110.csv -c ./config.json
```

