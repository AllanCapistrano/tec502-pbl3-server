# tec502-pbl3-server

<p align="center">
  <img src="https://png.pngtree.com/png-vector/20190521/ourlarge/pngtree-databasedistributedconnectionnetworkcomputer-line-icon-png-image_1058621.jpg" alt="Server icon" width="300px" height="300px">
</p>

------------

## 📚 Descrição ##
**Resolução do problema 3 do MI - Concorrência e Conectividade (TEC 502) - [Universidade Estadual de Feira de Santana (UEFS)](https://www.uefs.br/).**<br/><br/>
O projeto trata-se de um servidor (utilizando [ServerSocket](https://docs.oracle.com/javase/7/docs/api/java/net/ServerSocket.html)) que recebe requisições [HTTP](https://developer.mozilla.org/pt-BR/docs/Web/HTTP/Methods). <br/>
Este servidor permite múltiplas conexões com os *clients* ao mesmo tempo, em que cada conexão é processada por uma *thread* diferente.
Além disso, ele implementa o [Bully Algorithm](https://www.geeksforgeeks.org/election-algorithm-and-distributed-processing/), que é utilizado para decidir quais requisições serão processadas, com o intuito de evitar problemas de concorrência, pois o mesmo faz parte de um sistema distribuído.

### ⛵ Navegação pelos projetos: ###
- \>Servidor
- [Interface da Companhia Aérea](https://github.com/JoaoErick/tec502-pbl3-interface)

### 🔗 Tecnologias utilizadas: ### 
- [Java JDK 8](https://www.oracle.com/br/java/technologies/javase/javase-jdk8-downloads.html)

------------

## 🖥️ Como utilizar ##
Para o utilizar este projeto é necessário ter instalado o JDK 8u111.

- [JDK 8u111 com Netbeans 8.2](https://www.oracle.com/technetwork/java/javase/downloads/jdk-netbeans-jsp-3413139-esa.html)
- [JDK 8u111](https://www.oracle.com/br/java/technologies/javase/javase8-archive-downloads.html)

### Através de uma IDE ###
Caso esteja utilizando alguma IDE, basta **executar o projeto**, por exemplo, utilizando o *NetBeans IDE 8.2* aperte o botão `F6`; <br/>

------------

## 📌 Autores ##
- Allan Capistrano: [Github](https://github.com/AllanCapistrano) - [Linkedin](https://www.linkedin.com/in/allancapistrano/) - [E-mail](https://mail.google.com/mail/u/0/?view=cm&fs=1&tf=1&source=mailto&to=asantos@ecomp.uefs.br)
- João Erick Barbosa: [Github](https://github.com/JoaoErick) - [Linkedin](https://www.linkedin.com/in/joão-erick-barbosa-9050801b0/) - [E-mail](https://mail.google.com/mail/u/0/?view=cm&fs=1&tf=1&source=mailto&to=jsilva@ecomp.uefs.br)

------------

## ⚖️ Licença ##
[MIT License (MIT)](./LICENSE)
