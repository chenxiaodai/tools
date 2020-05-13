### 自测报告
> 测试结果符合预期
- 压测时间 4月29日晚上，持续12个小时
- 最大tps 200
- 发送交易占比符合预期

#### 测试环境
ip | 配置 | 节点类型 | 是否部署压力端
--- |--- |--- |---
192.168.112.141 | 4核8G | 非共识节点 | 否 |
192.168.120.141 | 4核8G | 共识节点 | 是 |
192.168.120.142 | 4核8G | 共识节点 | 是 |
192.168.120.143 | 4核8G | 共识节点 | 是 |
192.168.120.144 | 4核8G | 共识节点 | 是 |
192.168.120.145 | 4核8G | 非共识节点 | 是 |

#### 账户初始化
> 通过 genesis.json 内置 110万有钱账户

#### 压测端配置
```yaml
cat application.yaml 
spring:
    application:
        name: platon-press
    main:
        banner-mode: off
press:
    #最大tps
    tx-type: TRANFER,WASM,EVM
    tx-rate: 5,3,2
    tps: 100
    tranfer-need-receipt: false
    wasm-need-receipt: false
    evm-need-receipt: false
    consumer-thread-sleep-duration: 3000
    receipt-attempts: 3
    receipt-sleep-duration: 1000
    #节点rpc端口，支持http或websocket两种格式
    node-url: http://192.168.120.141:6789
    #节点公钥
    node-public-key: "0x0abaf3219f454f3d07b6cbcf3c10b6b4ccf605202868e2043b6f5db12b745df0604ef01ef4cb523adc6d9e14b83a76dd09f862e3fe77205d8ac83df707969b47"
    #链id
    chain-id: 103
    #钱包私钥文件
    keys-file: ./platon-press-config/keys/node-1.csv
    #转账交易的gasPrice
    tranfer-gas-price: 500000000000
    #转账交易的gasLimit
    tranfer-gas-limit: 21000
    #转账的接收地址列表
    tranfer-to-addrs:
        - "0xfbdf3c5bf983cdf67685883f8eaabfd4e31249ec"
        - "0xce77845c5cc019ca2965d99bea91c4a1fd854a64"
    #转账的接收文件
    tranfer-to-addrs-file: ./platon-press-config/tranfer-receive-addr-file.address
    #evm交易的gasPrice
    evm-gas-price: 500000000000
    #evm交易的gasLimit
    evm-gas-limit: 41468
    #evm测试合约地址
    evm-addr: "0x3b1546bd4274d170baef32f0fe92bb59783550f3"
    #wasm交易的gasPrice
    wasm-gas-price: 500000000000
    #wasm交易的gasLimit
    wasm-gas-limit: 360000
    #wasm测试合约地址
    wasm-addr: "0xecc096d70f93cad22be05e3a1411830f4e44aa2c"
logging:
    pattern:
        console: "%date %msg%n"
        file: "%date %msg%n"
    level:
        root: error
        spring: error
        org.web3j: error
        com.platon.tools: info
    file:
        name: ./logs/press.log

```

#### 报告人
> 陈岱