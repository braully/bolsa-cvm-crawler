# Crawler para CVM e Bolsa Brasileira

Esse projeto visa realizar scrap de dados da bolsa brasileira, e algumas informações de algumas corretoras, para criação de um banco de dados local,
com informações finaceiras.

Atenção todo o código desse projeto é meramente
acadêmcio, não deve ser utilizado para outro fim,
sob pena e responsabilildade do usuário.

## Implementações:
 - [Cei.java](https://github.com/braully/bolsa-cvm-crawler/blob/main/src/main/java/io/github/braully/bolsa/Cei.java): Implementação de um scrap das informações de negociação de ativos da bolsa brasileira, utiliza JSOUP e salva as informações em um arquivo csv.
 - [Clear.java](https://github.com/braully/bolsa-cvm-crawler/blob/main/src/main/java/io/github/braully/bolsa/Clear.java): Implementação de um scrap das informações de negociação de ativos da bolsa brasileira, utiliza SELENIUM e salva as informações em um arquivo csv.


## Principais bibliotecas
- PicoCLI
- JSOUP
- Selenium

## Compilação

```
$ ./mvnw -Pfatjar clean install
```

## Execução

```
java -jar target/bolsa-cvm-crawler.jar
```

## Parametros obrigatorios

- username
- password

## Arquivo de configuração [opcional]

Arquivo padrão na pasta corrente.

Exemplo: 'configuration.properties'
```
#CEI APP
cei.username=012.345.678-90
cei.password=P@ssW0d

#Clear 
clear.username=012.345.678-90
clear.dob=01/01/1981
clear.password=P@ssW0d
```

# Saída

Todas as negociações seram salvas em um arquivo csv,
por padrão negociacao_ceib3.csv.

Colunas do arquivo:
 - id
 - data
 - operacao
 - mercado
 - vencimento
 - codigo
 - especificacao
 - quantidade
 - valor
 - total
 - fator
 - agente
 - conta


# Exemplo de uso

## Com arquivo de configuração

```
$ java -jar bolsa-cvm-crawler.jar -c configuration.properties 
2021-12-10 11:07:08,841 DEBUG io.github.braully [main] Usuario e senha não estão na linha de comando
2021-12-10 11:07:08,847 DEBUG io.github.braully [main] tentando arquivo de configuração: configuration.properties
ultimas negociacoes carregadas: {4570=14/09/2021, 1099=15/10/2021, 308=06/01/2021}
2021-12-10 11:07:15,064 INFO io.github.braully [main] buscando dados agente: 820-xxxxx        de até 18/06/2020 
2021-12-10 11:07:15,315 INFO io.github.braully [main] buscando dados agente: 1570-xxxx        de até 18/06/2020 
2021-12-10 11:07:15,538 INFO io.github.braully [main] buscando dados agente: 308-xxxxx        de até 07/01/2021 
2021-12-10 11:07:15,769 INFO io.github.braully [main] buscando dados agente: 1982-xxxx        de até 18/06/2020 
2021-12-10 11:07:15,997 INFO io.github.braully [main] buscando dados agente: 90-xxxxxx        de até 18/06/2020 
2021-12-10 11:07:16,231 INFO io.github.braully [main] buscando dados agente: 1099-xxxx        de até 16/10/2021 
2021-12-10 11:07:16,488 INFO io.github.braully [main] buscando dados agente: 4570-xxxx        de até 15/09/2021 
```
