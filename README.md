# Crawler para CVM e Bolsa Brasileira

Baixa as informações de negociação de ativos e salva em um arquivo CSV,
execuções sucessivas iram buscar as novas negociações, 
a partir das datas de das ultimas infomrações no arquivo CSV.

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
Arquivo padrão 'configuration.properties' na pasta corrente.

Exemplo:
username=11111111111
password=O1234%a
fileCsvDatabase=dados/bd.csv

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