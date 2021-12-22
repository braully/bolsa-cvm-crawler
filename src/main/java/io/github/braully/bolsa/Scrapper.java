/*
 * The MIT License
 *
 * Copyright 2021 strike.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.github.braully.bolsa;

import io.github.braully.util.logutil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jsoup.Connection;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import picocli.CommandLine;

/**
 *
 * @author strike
 */
public class Scrapper {

    public final String dateFormat = "dd/MM/yyyy";
    public final SimpleDateFormat dateFormater = new SimpleDateFormat(dateFormat);

    //Configuration
    @CommandLine.Command(mixinStandardHelpOptions = true, version = "1.0")
    protected class Configuration {

        @CommandLine.Option(names = {"-u", "--user"}, description = "usuario")
        String username;
        @CommandLine.Option(names = {"-p", "--password"}, description = "senha")
        String password;
        @CommandLine.Option(names = {"-b", "--birth-date"}, description = "data de nascimento")
        String dob;
        @CommandLine.Option(names = {"-d", "--csv-database"}, description = "Arquivo que guarda o CSV Database")
        String fileCsvDatabase;
        @CommandLine.Option(names = {"-c", "--config"}, description = "Arquivo de configuração com os mesmos parametros da linha de comando")
        String fileConfig = "configuration.properties";

        public void loadPropertiesFile() throws FileNotFoundException, IOException {
            loadPropertiesFile("");
        }

        public void loadPropertiesFile(String prefix) throws FileNotFoundException, IOException {
            if (!prefix.isEmpty()) {
                logutil.debug("load propertis with prefix: " + prefix);
            }
            Properties properties = new Properties();
            properties.load(new FileReader(configuration.fileConfig));
            username = properties.getProperty(prefix + "username");
            password = properties.getProperty(prefix + "password");
            dob = properties.getProperty(prefix + "dob");
            fileCsvDatabase = properties.getProperty(prefix + "fileCsvDatabase", fileCsvDatabase);
        }

        public boolean isValid() {
            return !(configuration.username == null || configuration.username.isBlank()
                    || configuration.password == null || configuration.password.isBlank());
        }
    }

    //Configuration
    protected Configuration configuration = new Configuration();
    protected Connection connectionWeb;
    protected WebDriver driver;

    protected void init() {
    }

    protected void login() {
    }

    protected void execute() {
    }

    protected void end() {
    }

    protected String currentTimeString() {
        return "" + System.currentTimeMillis();
    }

    protected String currentDateString() {
        return dateFormater.format(new Date());
    }

    protected List<String> getValuesText(WebElement... els) {
        List<String> vlas = new ArrayList<>();
        if (els != null) {
            for (WebElement e : els) {
                vlas.add(getValueText(e));
            }
        }
        return vlas;
    }

    protected String getValueText(WebElement value) {
        String str = "";
        if (value != null) {
            str = value.getText();
            if (str == null || str.isEmpty()) {
                str = value.getAttribute("innerText");
            }
        }
        return str;
    }

    protected void appendRawCsv(List<String> cabecalho, List<List<String>> linhas) throws IOException {
        File file = new File(configuration.fileCsvDatabase);
        FileWriter writer;
        if (file.exists()) {
            writer = new FileWriter(configuration.fileCsvDatabase, true);
        } else {
            writer = new FileWriter(configuration.fileCsvDatabase);
            writer.write("#"); // newline
            writer.write(cabecalho.stream().collect(Collectors.joining(",")));
            writer.write("\n"); // newline
        }

        for (List<String> linha : linhas) {
            linha.replaceAll(c -> c.replaceAll(",", "."));
            writer.append(linha.stream().collect(Collectors.joining(",")));
            writer.append("\n"); // newline
        }
        writer.close();
    }

    protected void waitTime(long time) {
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException ex) {
            Logger.getLogger(Clear.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void waitRandomTimeUntil(long t) {
        waitTime(Math.round(t * Math.random()));
    }

    protected void run(String... args) {
        CommandLine commandLine = new CommandLine(configuration);
        commandLine.parseArgs(args);
        if (commandLine.isUsageHelpRequested()) {
            commandLine.usage(System.out);
            return;
        } else if (commandLine.isVersionHelpRequested()) {
            commandLine.printVersionHelp(commandLine.getOut());
            return;
        }

        //Username and password are required
        if (!configuration.isValid()) {
            logutil.debug("Usuario e senha não estão na linha de comando");
            try {
                logutil.debug("tentando arquivo de configuração: " + configuration.fileConfig);
                configuration.loadPropertiesFile(getClass().getSimpleName().toLowerCase() + ".");
            } catch (Exception e) {
                logutil.debug("no propertie file");
            }
            if (!configuration.isValid()) {
                commandLine.usage(System.out);
                return;
            }
        }

        init();
        login();
        execute();
        end();
    }
}
