package xyz.chendai.tools.platonrecover.cmd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class CmdRunner implements CommandLineRunner {

    @Override
    public void run(String... line)  {
        CommandLineParser parser = new DefaultParser();
        Options options = buildOptions();
        try {
            CommandLine cl = parser.parse(options, line, true);

            //帮助
            if(cl.hasOption("h")) {
                printHelp(options);
                return;
            }

            //加载签名
            String defaultPassword = cl.getOptionValue("p","pwd123456");
            String defaultFile = cl.getOptionValue("f",System.getProperty("user.dir")+ File.separator+"wallet.json");
        } catch (Exception e) {
            System.err.println(e);
            printHelp(options);
        }
    }

    private static final Options buildOptions(){

        OptionGroup optionGroup = new OptionGroup();
        optionGroup.setRequired(true);
        optionGroup.addOption(Option.builder("l").longOpt("loadWallet").desc("加载钱包").build());
        optionGroup.addOption(Option.builder("c").longOpt("createWallet").desc("创建钱包").build());

        Options options = new Options();
        options.addOption("h", "help", false, "显示帮助");

        options.addOptionGroup(optionGroup);
        options.addOption("f","walletPath",true, "创建或加载钱包时的路径，默认为当前目录下名称为wallet.json的钱包");
        options.addOption("p","walletPassword",true, "创建或加载钱包时的密码，默认pwd123456");

        options.addOption("s","txSize",true, "产生交易的数量，默认100");
        options.addOption("v","txValue",true, "产生交易的转账金额，单位VON，默认1000000000000000000");
        options.addOption("i","chainId",true, "链id，默认101");
        options.addOption("u","nodeUrl",true, "链地址，默认: https://aton.test.platon.network/rpc");
        options.addOption("t","to",true, "转账的接收地址，默认：0x912eea1aa4ad08ddf8e5d794d93e5294abcc2256");

        return options;
    }

    private static void printHelp(Options options) {
        String cmdLineSyntax = "java -jar pos-tools.jar";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(cmdLineSyntax,  options, true);
    }
}
