pragma solidity ^0.4.25;

contract PressureContract {

    mapping(string => uint256) nodeidmap;
    string[] public nodeids; //所有节点公钥
    uint256 public beginBlock;//开始区块
    uint256 public endBlock;//结束区块
    address public contractCreater;//合约创建者

    event SetSuccess(string nodeid,bool flag);

    constructor (uint256 _beginBlock,uint256 _endBlock) public {
        contractCreater = msg.sender;
        beginBlock = _beginBlock;
        endBlock = _endBlock;
    }


    /**
     * 在指定块高内才能发交易
     */
    modifier checkBlock() {
        if (block.number >= beginBlock && block.number <= endBlock ) _;
    }

    /**
     * 合约创建者校验
     */
    modifier checkCreater() {
        if (contractCreater == msg.sender) _;
    }

    /**
     * 合约调用次数统计
     */
    function record(string memory nodeid) public checkBlock{
        if(nodeidmap[nodeid] ==0){
            nodeidmap[nodeid]++;
            nodeids.push(nodeid);
        }else{
            nodeidmap[nodeid]++;
        }

        SetSuccess(nodeid,true);
    }

    /**
     * 根据nodeid查找对应调用次数
     */
    function getValue(string memory nodeid) public constant returns(uint){
        return nodeidmap[nodeid];
    }

    /**
     *  设置开始区块 结束区块
     */
    function setBeginAndEndBlock(uint256 _beginBlock,uint256 _endBlock) public checkCreater {
        beginBlock = _beginBlock;
        endBlock = _endBlock;
    }

    /**
     *  清空map方法
     */
    function clearMap() public checkCreater{
        for(uint i =0;i<nodeids.length;i++){
            nodeidmap[nodeids[i]] = 0;
        }
    }

}