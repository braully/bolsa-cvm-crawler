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

import io.github.braully.util.ioutil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Realiza scrap do saldo da conta na clear corretora, 
 * utilizando a biblioteca de automação Selenium.
 * Atenção todo o código desse projeto é meramente
 * acadêmcio, não deve ser utilizado para outro fim,
 * sob pena e responsabilildade do usuário.
 * @author Braully Rocha da Silva
 */
public class Clear extends Scrapper {

    private static boolean DEBUB_DUMP_PAGES = true;

    private static final List<String> colunas = List.of("id", "banco", "agencia", "conta", "saldo", "data");

    {
        configuration.fileCsvDatabase = "saldo_conta.csv";
    }

    public static void main(String... args) throws IOException, InterruptedException {

        Clear clear = new Clear();
        clear.run(args);
    }

    @Override
    protected void init() {
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.setHeadless(true);
        driver = new FirefoxDriver(firefoxOptions);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        WebDriverWait waitDriver = new WebDriverWait(driver, 30);

    }

    @Override
    protected void login() {
        //        WebDriver driver = new JBrowserDriver();
        //
        driver.get("https://login.clear.com.br/pit/login/");
        driver.findElement(By.id("identificationNumber")).sendKeys(configuration.username);
        waitRandomTimeUntil(5);
        driver.findElement(By.id("dob")).sendKeys(configuration.dob);
        waitRandomTimeUntil(5);
        driver.findElement(By.id("password")).sendKeys(configuration.password);
        waitRandomTimeUntil(5);
        driver.findElement(By.xpath("//button[@value=\'login\']")).click();
        waitTime(12);

    }

    @Override
    protected void execute() {
        //Trying close promotion
        try {
            driver.findElement(By.id("btnPromoFechar")).click();
        } catch (Exception e) {

        }
        String pageSource = null;
        if (DEBUB_DUMP_PAGES) {
            pageSource = driver.getPageSource();
            ioutil.saveToFile(pageSource, "tmp/home.html");
        }

        driver.get("https://pro.clear.com.br/#minha-conta/extrato");
        waitTime(3);

        if (DEBUB_DUMP_PAGES) {
            pageSource = driver.getPageSource();
            ioutil.saveToFile(pageSource, "tmp/extrato.html");
        }
        try {
//colunas = List.of("id", "banco", "agencia", "conta", "saldo", "data");

            WebElement bank = driver.findElement(By.cssSelector(".bank_number"));
            WebElement agency = driver.findElement(By.cssSelector(".bank_agency"));
            WebElement account = driver.findElement(By.cssSelector(".bank_account"));
            //For future use:
            /*
            WebElement saldo = driver.findElement(By.cssSelector(".d0_val"));
            WebElement saldoD1 = driver.findElement(By.cssSelector(".d1_val"));
            WebElement saldoD2 = driver.findElement(By.cssSelector(".d2_val"));
             */
            WebElement saldoTotal = driver.findElement(By.cssSelector(".total_val"));
            List<String> linha = new ArrayList<>();
            //TODO: Generate id on database csv, snowflake strategy
            linha.add(currentTimeString());
            linha.addAll(getValuesText(bank, agency, account));
            String saldoStr = getValueText(saldoTotal);
            //Tratar valor numerico, melhorar isso
            linha.add(saldoStr.replaceAll("R$ ", "").replaceAll(",", "."));
            linha.add(currentDateString());
            appendRawCsv(colunas, List.of(linha));
        } catch (Exception e) {

        }
        driver.get("https://pro.clear.com.br/#minha-conta/meus-ativos");
        if (DEBUB_DUMP_PAGES) {
            pageSource = driver.getPageSource();
            ioutil.saveToFile(pageSource, "tmp/meus-ativos.html");
        }
        waitTime(3);

        driver.get("https://pro.clear.com.br/#minha-conta/historico-ordens");
        if (DEBUB_DUMP_PAGES) {
            pageSource = driver.getPageSource();
            ioutil.saveToFile(pageSource, "tmp/historico-ordens.html");
//        driver.get("https://pro.clear.com.br/#renda-fixa/tesouro-direto");
//        driver.get("https://pro.clear.com.br/#renda-variavel/ofertas-publicas");
        }
        waitTime(2);

    }

    @Override
    protected void end() {
        //Trying logout
        try {
            driver.findElement(By.cssSelector(".account-title")).click();
            driver.findElement(By.cssSelector(".icon-signout")).click();
        } catch (Exception e) {

        }
        driver.quit();
    }
}
