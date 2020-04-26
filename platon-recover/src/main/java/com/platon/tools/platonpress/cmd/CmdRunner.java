package com.platon.tools.platonpress.cmd;

import com.platon.tools.platonpress.service.CollectService;
import com.platon.tools.platonpress.service.RecoverService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CmdRunner implements CommandLineRunner {

    @Autowired
    private CollectService collectService;

    @Autowired
    private RecoverService recoverService;

    @Override
    public void run(String... line)  {
        CommandLineParser parser = new DefaultParser();
        Options options = buildOptions();
        try {
            CommandLine cl = parser.parse(options, line, true);
            //帮助
            if(cl.hasOption("h")) {
                printHelp(options);
            }

            //采集数据
            if(cl.hasOption("c")) {
                collectService.collectAddress();
                collectService.collectBalance();
            }

            //余额恢复
            if(cl.hasOption("r")) {
                recoverService.initCheck();
                recoverService.recover();
                recoverService.check();
            }

            //工作账户返还
            if(cl.hasOption("b")) {
                recoverService.initCheck();
                recoverService.recover();
                recoverService.check();
            }

        } catch (Exception e) {
            log.error("处理过程异常！",e);
            printHelp(options);
        }
    }

    private static final Options buildOptions(){
        Options options = new Options();
        options.addOption("h","help", false, "显示帮助");
        options.addOption("c","collect",false, "采集数据");
        options.addOption("r","recover",false, "余额恢复");
        options.addOption("b","back",false, "账户返还");
        return options;
    }

    private static void printHelp(Options options) {
        String cmdLineSyntax = "java -jar platon-recover.jar";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(cmdLineSyntax,  options, true);
    }
}
