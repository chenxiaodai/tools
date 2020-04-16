package xyz.chendai.tools.platonrecover.cmd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import xyz.chendai.tools.platonrecover.service.CollectService;

@Slf4j
@Component
public class CmdRunner implements CommandLineRunner {

    @Autowired
    private CollectService collectService;

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

            //从链上采集数据
            if(cl.hasOption("c")) {
                collectService.collectAddress();
                collectService.collectBalance();
            }

        } catch (Exception e) {
            System.err.println(e);
            printHelp(options);
        }
    }

    private static final Options buildOptions(){
        Options options = new Options();
        options.addOption("h", "help", false, "显示帮助");
        options.addOption("c","collect",false, "采集数据");
        return options;
    }

    private static void printHelp(Options options) {
        String cmdLineSyntax = "java -jar platon-recover.jar";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(cmdLineSyntax,  options, true);
    }
}
