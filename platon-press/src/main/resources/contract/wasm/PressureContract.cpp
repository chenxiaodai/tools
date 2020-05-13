#include <platon/platon.hpp>
#include <string>
using namespace std;
using namespace platon;


CONTRACT PressureContract : public platon::Contract{
	public:
    ACTION void init(const uint64_t &BeginBlock, const uint64_t &EndBlock)
    {
        begin_block.self() = BeginBlock;
        end_block.self() = EndBlock;
        owner.self() = platon_caller();
    }

    ACTION void setBeginAndEndBlock(const uint64_t &BeginBlock, const uint64_t &EndBlock)
    {
        if(owner.self() == platon_caller())
        {
            begin_block.self() = BeginBlock;
            end_block.self() = EndBlock;
        }
    }

    ACTION void record(std::string &nodeID)
    {
        uint64_t currentblcok = platon_block_number();
        if(currentblcok >= begin_block.self() && currentblcok <= end_block.self())
        {
            auto iter = map_nodeid.self().find(nodeID);
            if(iter != map_nodeid.self().end()){
                map_nodeid.self()[nodeID] = ++map_nodeid.self()[nodeID];
            } else
            {
                map_nodeid.self()[nodeID] = 1;
            }
        }
    }

    ACTION void clearMap()
    {
        if(owner.self() == platon_caller())
        {
            map_nodeid.self().clear();
        }
    }
  }

    CONST std::string getAll()
    {
        std::string strTmp = "";
        for(auto iter = map_nodeid.self().begin(); iter != map_nodeid.self().end(); iter++)
        {
            strTmp += "\"";
            strTmp += iter->first;
            strTmp += "\"";
            strTmp += ":";
            strTmp += "\"";
            strTmp += std::to_string(map_nodeid.self()[iter->first]);
            strTmp += "\"";
            strTmp += ";";
        }
        strTmp.pop_back();
        char* buf = (char*)malloc(strTmp.size() + 1);
        memset(buf, 0, strTmp.size()+1);
        strcpy(buf, strTmp.c_str());
        return buf;
    }

    private:
        platon::StorageType<"owner"_n, Address> owner;
        platon::StorageType<"initmap"_n, std::map<std::string,u128>> map_nodeid;
        platon::StorageType<"beginblock"_n,uint64_t> begin_block;
        platon::StorageType<"endblock"_n,uint64_t> end_block;
};

PLATON_DISPATCH(PressureContract, (init)(setBeginAndEndBlock)(record)(clearMap)(getValue)(getAll))