/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.braully.bolsa;

import io.github.braully.util.logutil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 *
 * @author Braully Rocha da Silva
 *
 * Referências: - https://stackabuse.com/web-scraping-the-java-way/ -
 *
 */
public class CeiB3 {

    public static final String dateFormat = "dd/MM/yyyy";
    public static final SimpleDateFormat dateFormater = new SimpleDateFormat(dateFormat);

    public static final Map<String, String> cabecalhoToColuna = Map.of(
            "Data do Negócio", "data",
            "Compra/Venda", "operacao",
            "Mercado", "mercado",
            "Prazo/Vencimento", "vencimento",
            "Código Negociação", "codigo",
            "Especificação do Ativo", "especificacao",
            "Quantidade", "quantidade",
            "Preço (R$)", "valor",
            "Valor Total(R$)", "total",
            "Fator de Cotação", "fator"
    );

    //Configuration
    @Command(name = "ceibolsa", mixinStandardHelpOptions = true, version = "1.0")
    class Configuration {

        @CommandLine.Option(names = {"-u", "--user"}, description = "CPF")
        String username;
        @CommandLine.Option(names = {"-p", "--password"}, description = "senha")
        String password;
        @CommandLine.Option(names = {"-l", "--login-url"}, description = "Url de Login")
        String loginUrl = "https://ceiapp.b3.com.br/cei_responsivo/login.aspx";
        @CommandLine.Option(names = {"-i", "--home-url"}, description = "Url do home")
        String homeUrl = "https://ceiapp.b3.com.br/CEI_Responsivo/home.aspx";
        @CommandLine.Option(names = {"-n", "--negociacao-url"}, description = "Url da tela de negociacao")
        String negociacaoUrl = "https://ceiapp.b3.com.br/CEI_Responsivo/negociacao-de-ativos.aspx";
        @CommandLine.Option(names = {"-d", "--csv-database"}, description = "Arquivo que guarda o CSV Database")
        String fileCsvDatabase = "negociacao_ceib3.csv";
        @CommandLine.Option(names = {"-c", "--config"}, description = "Arquivo de configuração com os mesmos parametros da linha de comando")
        String fileConfig = "configuration.properties";

        void loadPropertiesFile() throws FileNotFoundException, IOException {
            Properties properties = new Properties();
            properties.load(new FileReader(configuration.fileConfig));
            username = properties.getProperty("username");
            password = properties.getProperty("password");
            fileCsvDatabase = properties.getProperty("fileCsvDatabase", fileCsvDatabase);
            loginUrl = properties.getProperty("loginUrl", loginUrl);
            homeUrl = properties.getProperty("homeUrl", homeUrl);
            negociacaoUrl = properties.getProperty("negociacaoUrl", negociacaoUrl);
        }

        boolean isValid() {
            return !(configuration.username == null || configuration.username.isBlank()
                    || configuration.password == null || configuration.password.isBlank());
        }
    }

    //Configuration
    Configuration configuration = new Configuration();
    Connection connectionWeb;
    String strDataInicioDisponivel, strDataFimDisponivel, strConta;
    Map<String, String> agenteUltimoData = new HashMap<>();
    Map<String, String> agenteMap = new LinkedHashMap<>();

    public static void main(String... args) throws IOException, SQLException, ParseException {
        CeiB3 cei = new CeiB3();

        CommandLine commandLine = new CommandLine(cei.configuration);
        commandLine.parseArgs(args);
        if (commandLine.isUsageHelpRequested()) {
            commandLine.usage(System.out);
            return;
        } else if (commandLine.isVersionHelpRequested()) {
            commandLine.printVersionHelp(commandLine.getOut());
            return;
        }

        //Username and password are required
        if (!cei.configuration.isValid()) {
            logutil.debug("Usuario e senha não estão na linha de comando");
            try {
                logutil.debug("tentando arquivo de configuração: " + cei.configuration.fileConfig);
                cei.configuration.loadPropertiesFile();
            } catch (Exception e) {
                logutil.debug("no propertie file");
            }
            if (!cei.configuration.isValid()) {
                commandLine.usage(System.out);
                return;
            }
        }

        cei.init();
        cei.login();
        cei.extractDataAgentes();
        cei.extractNegociacoes();
        cei.end();
    }

    public void carregarDataUltimasNegociacoesExtaidas() throws SQLException {
        File file = new File(configuration.fileCsvDatabase);
        org.h2.Driver.load();
        if (file.exists()) {
            java.sql.Connection conn = DriverManager.getConnection("jdbc:h2:mem:.", "sa", "");
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("select agente, "
                    + " FORMATDATETIME(max(PARSEDATETIME(data,'dd/MM/yyyy')), 'dd/MM/yyyy') as max_data "
                    + " from csvread('" + file.getAbsolutePath() + "') "
                    + " group by agente");
            ResultSetMetaData metaData = rs.getMetaData();
            while (rs.next()) {
                agenteUltimoData.put(rs.getString(1), rs.getString(2));
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    logutil.debug(metaData.getColumnLabel(i + 1) + ": "
                            + rs.getString(i + 1));
                }
            }
            //logutil.info
            System.out.println("ultimas negociacoes carregadas: " + agenteUltimoData);
        } else {
            logutil.debug("database not found: it will be created");
        }
    }

    public void extractDataAgentes() throws IOException {
        //Se sucesso entrar na tela de negociação de ativos
        System.out.println("Tela de negociacao");
        Document telaNegociacao = connectionWeb.newRequest()
                .referrer(configuration.homeUrl)
                .url(configuration.negociacaoUrl).get();
//        saveToFile(telaNegociacao, "negociacao.html");
        strDataInicioDisponivel = telaNegociacao.getElementById("ctl00_ContentPlaceHolder1_txtDataDeBolsa").val();
        strDataFimDisponivel = telaNegociacao.getElementById("ctl00_ContentPlaceHolder1_txtDataAteBolsa").val();
        strConta = "0";
        Elements agentesOption = telaNegociacao.getElementById("ctl00_ContentPlaceHolder1_ddlAgentes").getElementsByTag("option");
        agenteMap.clear();
        for (Element el : agentesOption) {
            String agente = el.val();
            if (agente.equals("-1")) {
                continue;
            }
            agenteMap.put(agente, el.text());
        }
    }

    public void init() throws SQLException {
        connectionWeb = Jsoup.connect(configuration.loginUrl);
        carregarDataUltimasNegociacoesExtaidas();
//        initDBCSV();
    }

    public void login() throws IOException {
        System.out.println("Login");
        Document paginaInicial = connectionWeb.get();
        //saveToFile(paginaInicial, "login.html");
        var payload = new HashMap<String, String>();
        extrairValoresEstadoPagina(paginaInicial, payload);
        payload.putAll(Map.of("__EVENTTARGET", "",
                "ctl00$ContentPlaceHolder1$txtLogin", configuration.username,
                "ctl00$ContentPlaceHolder1$txtSenha", configuration.password,
                "ctl00$ContentPlaceHolder1$btnLogar", "Entrar"
        ));
        Document resultadoLogin = connectionWeb.newRequest()
                .data(payload)
                .referrer(configuration.loginUrl)
                .url(configuration.loginUrl).post();
//        saveToFile(resultadoLogin, "home.html");
    }

    protected void extrairValoresEstadoPagina(Document pagina, Map<String, String> estado) {
        estado.put("__VIEWSTATE", pagina.getElementById("__VIEWSTATE").val());
        estado.put("__VIEWSTATEGENERATOR", pagina.getElementById("__VIEWSTATEGENERATOR").val());
        estado.put("__EVENTVALIDATION", pagina.getElementById("__EVENTVALIDATION").val());
    }

    protected void saveToFile(Document pagina, String arquivo) throws IOException {
        System.out.println("save file: " + arquivo);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(arquivo));
        bufferedWriter.write(pagina.toString());
        bufferedWriter.close();
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
            linha.replaceAll(c -> c.replaceAll(",", ""));
            writer.append(linha.stream().collect(Collectors.joining(",")));
            writer.append("\n"); // newline
        }
        writer.close();
    }

    protected void extractNegociacoes() throws IOException, ParseException {
        for (String agente : agenteMap.keySet()) {
            var telaNegociacao = connectionWeb.newRequest()
                    .referrer(configuration.negociacaoUrl)
                    .url(configuration.negociacaoUrl).get();

            strConta = "0";
            var payload = new HashMap<String, String>();
            extrairValoresEstadoPagina(telaNegociacao, payload);
            String dataInicio = maxDataInicio(strDataInicioDisponivel, agente);
            payload.putAll(Map.of("ctl00$ContentPlaceHolder1$ToolkitScriptManager1",
                    "ctl00$ContentPlaceHolder1$updFiltro|ctl00$ContentPlaceHolder1$ddlAgentes",
                    "ctl00_ContentPlaceHolder1_ToolkitScriptManager1_HiddenField", "",
                    "__EVENTTARGET", "ctl00$ContentPlaceHolder1$ddlAgentes",
                    "__EVENTARGUMENT", "", "__LASTFOCUS", "",
                    "ctl00$ContentPlaceHolder1$hdnPDF_EXCEL", "",
                    "ctl00$ContentPlaceHolder1$txtDataDeBolsa", dataInicio,
                    "ctl00$ContentPlaceHolder1$txtDataAteBolsa", strDataFimDisponivel,
                    "ctl00$ContentPlaceHolder1$ddlAgentes", agente,
                    "ctl00$ContentPlaceHolder1$ddlContas", strConta)
            );

            telaNegociacao = connectionWeb.newRequest()
                    .referrer(configuration.negociacaoUrl)
                    .url(configuration.negociacaoUrl)
                    .data(payload).post();
//            saveToFile(telaNegociacao, "negociacao-" + agente + ".html");

            Elements contasOptions = telaNegociacao
                    .getElementById("ctl00_ContentPlaceHolder1_ddlContas")
                    .getElementsByTag("option");

            for (Element contaElement : contasOptions) {
                strConta = contaElement.val();
                logutil.info(String.format("buscando dados agente: %-18s de até %-10s ", (agente + "-" + strConta), dataInicio, strDataFimDisponivel));
                extrairValoresEstadoPagina(telaNegociacao, payload);
                payload.put("ctl00$ContentPlaceHolder1$ddlContas", strConta);
                payload.put("ctl00$ContentPlaceHolder1$btnConsultar", "Consultar");

                telaNegociacao = connectionWeb.newRequest()
                        .referrer(configuration.negociacaoUrl)
                        .url(configuration.negociacaoUrl)
                        .data(payload).post();

                //saveToFile(telaNegociacao, "negociacao-" + agente + "-" + strConta + ".html");
                Element tabelaResultado = telaNegociacao.getElementById("ctl00_ContentPlaceHolder1_rptAgenteBolsa_ctl00_rptContaBolsa_ctl00_pnAtivosNegociados");
                if (tabelaResultado != null) {
                    Elements cabecalhoTabela = tabelaResultado.getElementsByTag("th");
                    List<String> cabecalho = new ArrayList<>();
                    cabecalho.add("id");
                    for (Element cab : cabecalhoTabela) {
                        cabecalho.add(cab.text());
                    }
                    //Adicionar o agente e conta nas colunas
                    cabecalho.add("agente");
                    cabecalho.add("conta");
//                    System.out.println("Cabecalho: " + cabecalho);

                    Elements linhasTabela = tabelaResultado.getElementsByTag("tbody").get(0).getElementsByTag("tr");
                    List<List<String>> linhasRaw = new ArrayList<>();
                    for (Element lin : linhasTabela) {
                        List<String> linha = new ArrayList<>();
                        //Id=CurrentTimestamp+LinhasCount
                        linha.add("" + System.currentTimeMillis() + linhasRaw.size());
                        //Dados da Tela
                        for (Element cel : lin.getElementsByTag("td")) {
                            linha.add(cel.text());
                        }
                        if (linha.size() > 1) {
                            //Acrescentar agente e conta
                            linha.add(agente);
                            linha.add(strConta);
                            linhasRaw.add(linha);
                            logutil.info("" + linha);
                        }
                    }
                    if (!linhasRaw.isEmpty()) {
                        cabecalho.replaceAll(c -> toColuna(c));
                        appendRawCsv(cabecalho, linhasRaw);
//                    insertCsvDatabase(cabecalho, linhasRaw);
                    }
                }
                //Se tiver mais de um agente de custodia, resetar a consulta entrando novamente na pagina
                if (contasOptions.size() > 1) {
                    telaNegociacao = connectionWeb.newRequest()
                            .referrer(configuration.negociacaoUrl)
                            .url(configuration.negociacaoUrl).get();
                }

            }
        }
    }

    protected void end() {

    }

    protected String toColuna(String c) {
        String ret = cabecalhoToColuna.get(c);
        if (ret == null) {
            ret = c.toLowerCase();
        }
        return ret;
    }

    protected String maxDataInicio(String strDataDisponivel, String agente) throws ParseException {
        if (!agenteUltimoData.containsKey(agente)) {
            return strDataDisponivel;
        }
        Date dataDisponivel = dateFormater.parse(strDataDisponivel);
        Date dataUltimaExtracao = dateFormater.parse(agenteUltimoData.get(agente));
        //Adicionar um dia na data da ultima extração
        Calendar cal = Calendar.getInstance();
        cal.setTime(dataUltimaExtracao);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        dataUltimaExtracao = cal.getTime();

        Date max = dataDisponivel;
        if (dataUltimaExtracao.after(dataDisponivel)) {
            max = dataUltimaExtracao;
        }
        String format = dateFormater.format(max);
        return format;
    }
}
