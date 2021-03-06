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
import java.io.File;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Braully Rocha da Silva
 *
 * Referências: - https://stackabuse.com/web-scraping-the-java-way/ -
 *
 */
public class Cei extends Scrapper {

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

    String loginUrl = "https://ceiapp.b3.com.br/cei_responsivo/login.aspx";
    String homeUrl = "https://ceiapp.b3.com.br/CEI_Responsivo/home.aspx";
    String negociacaoUrl = "https://ceiapp.b3.com.br/CEI_Responsivo/negociacao-de-ativos.aspx";

    //Default value
    {
        configuration.fileCsvDatabase = "negociacao_ceib3.csv";
    }

    String strDataInicioDisponivel, strDataFimDisponivel, strConta;
    Map<String, String> agenteUltimoData = new HashMap<>();
    Map<String, String> agenteMap = new LinkedHashMap<>();

    public static void main(String... args) throws IOException, SQLException, ParseException {
        Cei cei = new Cei();
        cei.run(args);
    }

    public void execute() {
        try {
            extractDataAgentes();
            extractNegociacoes();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void carregarDataUltimasNegociacoesExtaidas() {
        File file = new File(configuration.fileCsvDatabase);
        org.h2.Driver.load();
        try {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void extractDataAgentes() throws IOException {
        //Se sucesso entrar na tela de negociação de ativos
        System.out.println("Tela de negociacao");
        Document telaNegociacao;
        telaNegociacao = connectionWeb.newRequest()
                .referrer(homeUrl)
                .url(negociacaoUrl).get();
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

    public void init() {
        connectionWeb = Jsoup.connect(loginUrl);
        carregarDataUltimasNegociacoesExtaidas();
//        initDBCSV();
    }

    public void login() {
        System.out.println("Login");
        Document paginaInicial;
        try {
            paginaInicial = connectionWeb.get();
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
                    .referrer(loginUrl)
                    .url(loginUrl).post();
//        saveToFile(resultadoLogin, "home.html");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void extrairValoresEstadoPagina(Document pagina, Map<String, String> estado) {
        estado.put("__VIEWSTATE", pagina.getElementById("__VIEWSTATE").val());
        estado.put("__VIEWSTATEGENERATOR", pagina.getElementById("__VIEWSTATEGENERATOR").val());
        estado.put("__EVENTVALIDATION", pagina.getElementById("__EVENTVALIDATION").val());
    }

    protected void extractNegociacoes() throws IOException, ParseException {
        for (String agente : agenteMap.keySet()) {
            var telaNegociacao = connectionWeb.newRequest()
                    .referrer(negociacaoUrl)
                    .url(negociacaoUrl).get();

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
                    .referrer(negociacaoUrl)
                    .url(negociacaoUrl)
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
                        .referrer(negociacaoUrl)
                        .url(negociacaoUrl)
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
                            .referrer(negociacaoUrl)
                            .url(negociacaoUrl).get();
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
