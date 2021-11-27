package main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import models.Edge;
import models.Ticket;
import models.ServerAddress;
import models.Travel;

/**
 * Classe que lida com as requisições enviadas para o servidor.
 *
 * @author Allan Capistrano
 * @author João Erick Barbosa
 */
public class ConnectionHandler implements Runnable {

    private final Socket connection;
    private final ObjectInputStream input;
    private String received;

    /**
     * Método construtor.
     *
     * @param connection Socket - Conexão com o Client.
     * @throws IOException
     */
    public ConnectionHandler(Socket connection) throws IOException {
        this.connection = connection;
        this.input = new ObjectInputStream(connection.getInputStream());
    }

    @Override
    public void run() {
        try {
            /* Requisição recebida. */
            this.received = (String) this.input.readObject();

            /* Processandos a requisição. */
            this.processRequests(this.received);

            /* Finalizando as conexões. */
            input.close();
            connection.close();
        } catch (IOException ioe) {
            System.err.println("Erro de Entrada/Saída.");
            System.out.println(ioe);
        } catch (ClassNotFoundException cnfe) {
            System.err.println("Classe String não foi encontrada.");
            System.out.println(cnfe);
        }
    }

    /**
     * Processa as requisições que são enviados ao servidor.
     *
     * @param httpRequest JSONObject - Requisição HTTP.
     */
    private void processRequests(String httpRequest) {
        System.out.println("> Processando a requisição");

        try {
            if (httpRequest.equals("GET /routes")) {
                System.out.println("> Rota: /routes");
                System.out.println("\t Método: GET");

                ObjectInputStream secondInput
                        = new ObjectInputStream(connection.getInputStream());

                String[] request
                        = ((String) secondInput.readObject()).split(",");

                Server.unifyGraph();

                /* Aguardar o grafo ser unificado. */
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    System.err.println("Thread finalizada de maneira "
                            + "inesperada.");
                    System.out.println(ie);
                }

                this.sendRoutes(request[0], request[1]);

            } else if (httpRequest.equals("POST /buy")) {
                System.out.println("> Rota: /buy");
                System.out.println("\t Método: POST");

                ObjectInputStream secondInput
                        = new ObjectInputStream(connection.getInputStream());

                /* Adicionando a compra da passagem no final da fila de 
                passagens. */
                Server.tickets.add((Ticket) secondInput.readObject());

                /* NÃO VAI USAR */
                Server.interfaceClient.add(connection.getOutputStream());

            } else if (httpRequest.equals("POST /buy/authorization")) {
                System.out.println("> Rota: /buy/authorization");
                System.out.println("\t Método: POST");

                ObjectInputStream secondInput
                        = new ObjectInputStream(connection.getInputStream());

                Edge e = (Edge) secondInput.readObject();

                /* VER SE PRECISA MUDAR O NOME DO MÉTODO */
                this.buyRoute(e);

            } else if (httpRequest.equals("POST /send/authorization")) {
                System.out.println("> Rota: /send/authorization");
                System.out.println("\t Método: POST");

                ObjectInputStream secondInput
                        = new ObjectInputStream(connection.getInputStream());

                Edge route = (Edge) secondInput.readObject();

                if (route != null) {
                    Server.purchasesAccepted.push(route);

                    System.out.println("> Compra do trecho "
                            + route.getFirstCity().getCityName() + " -> "
                            + route.getSecondCity().getCityName()
                            + " realziada com sucesso!");
                } else {
                    /* TO DO */
                    System.err.println("DEU RUIM!!!");
                }
            } else if (httpRequest.equals("GET /graph")) {
                System.out.println("> Rota: /graph");
                System.out.println("\t Método: GET");

                this.sendGraph();

            } else if (httpRequest.equals("GET /startElection")) {
                System.out.println("> Rota: /startElection");
                System.out.println("\t Método: GET");
                System.out.println("\t Quantidade de requisições: "
                        + Server.tickets.size());
                Server.electionActive = true;

                this.sendAmountRequests();

            } else if (httpRequest.equals("POST /coordinator")) {
                System.out.println("> Rota: /coordinator");
                System.out.println("\t Método: POST");

                ObjectInputStream secondInput
                        = new ObjectInputStream(connection.getInputStream());

                Server.coordinator
                        = (ServerAddress) secondInput.readObject();
                Server.electionActive = false;

                System.out.println("Novo coordenador: "
                        + Server.coordinator.getCompanyName());

            } else if (httpRequest.equals("POST /ping")) {
                System.out.println("> Rota: /ping");
                System.out.println("\t Método: POST");

                ObjectInputStream inputBody
                        = new ObjectInputStream(connection.getInputStream());

                System.out.println("O servidor da companhia "
                        + ((String) inputBody.readObject()) + " vivo!");

            } else if (httpRequest.equals("GET /coordinatorAlive")) {
                System.out.println("> Rota: /coordinatorAlive");
                System.out.println("\t Método: GET");

                if (Server.coordinator.getCompanyName().equals(Server.companyName)) {
                    this.sendCoordinatorCompanyName();
                }
            }
        } catch (IOException ioe) {
            System.err.println("Erro ao receber as requisições.");
            System.out.println(ioe);
        } catch (ClassNotFoundException cnfe) {
            System.err.println("Classe String não foi encontrada.");
            System.out.println(cnfe);
        }

        System.out.println("");
    }

    /**
     * Envia todos os possíveis trajetos entre duas cidades
     *
     * @param firstCity String - Primeira cidade.
     * @param secondCity String - Segunda cidade.
     */
    private void sendRoutes(String firstCity, String secondCity) {
        try {
            ObjectOutputStream output
                    = new ObjectOutputStream(connection.getOutputStream());

            List<Travel> routes
                    = Server.unifiedGraph.depthFirst(firstCity, secondCity);

            System.out.println("> Enviando as possíveis rotas (Qtd: "
                    + routes.size() + ")...");

            System.out.println();

            output.flush();
            output.writeObject(routes);
            output.flush();

            output.close();
        } catch (IOException ioe) {
            System.err.println("Erro ao tentar enviar a lista de trajetos.");
            System.out.println(ioe);
        }
    }

    /**
     * Envia o grafo desta companhia.
     */
    private void sendGraph() {
        try {
            ObjectOutputStream output
                    = new ObjectOutputStream(connection.getOutputStream());

            System.out.println("> Enviando o grafo...");

            output.flush();
            output.writeObject(Server.graph);
            output.flush();

            output.close();
        } catch (IOException ioe) {
            System.err.println("Erro ao tentar enviar o grafo.");
            System.out.println(ioe);
        }
    }

    /**
     * Envia a quantidade de requisições deste servidor.
     */
    private void sendAmountRequests() {
        try {
            ObjectOutputStream output
                    = new ObjectOutputStream(connection.getOutputStream());

            System.out.println("> Enviando a quantidade de requisições...");

            output.flush();
            output.writeObject(Server.tickets.size());
            output.flush();

            output.close();
        } catch (IOException ioe) {
            System.err.println("Erro ao tentar enviar a quantidade de "
                    + "requisições.");
            System.out.println(ioe);
        }
    }

    /**
     * Envia o nome da companhia do atual servidor coordenador.
     */
    private void sendCoordinatorCompanyName() {
        try {
            ObjectOutputStream output
                    = new ObjectOutputStream(connection.getOutputStream());

            System.out.println("> Enviando o nome da companhia do "
                    + "coordenador...");

            output.flush();
            output.writeObject(Server.coordinator.getCompanyName());
            output.flush();

            output.close();
        } catch (IOException ioe) {
            System.err.println("Erro ao tentar enviar o nome da companhia do "
                    + "coordenador.");
            System.out.println(ioe);
        }
    }

    /**
     * Responsável pela compra de um trecho.
     * 
     * @param route Edge - 
     */
    private void buyRoute(Edge route) {
        for (Edge r : Server.routes) {
            try {
                if (r.equals(route) && r.getAmountSeat() > 0) {
                    System.out.println("Qtd acentos antes: " 
                            + r.getAmountSeat());
                    route.setAmountSeat(r.getAmountSeat() - 1);
                    System.out.println("Qtd acentos depois: " 
                            + r.getAmountSeat());

                    Socket coordinatorServer
                            = new Socket(
                                    Server.coordinator.getIpAddress(),
                                    Server.coordinator.getPort()
                            );

                    ObjectOutputStream output
                            = new ObjectOutputStream(
                                    coordinatorServer.getOutputStream()
                            );

                    System.out.println("> Enviando a confirmação de compra do "
                            + "trecho.");

                    output.flush();
                    output.writeObject("POST /send/authorization");
                    output.flush();

                    ObjectOutputStream outputBody
                            = new ObjectOutputStream(
                                    coordinatorServer.getOutputStream()
                            );
                    outputBody.flush();
                    outputBody.writeObject(r);
                    outputBody.flush();

                    output.close();
                    outputBody.close();

                    break;
                } else if (r.getAmountSeat() < 0) {
                    System.out.println("> Não foi possível realizar a compra "
                            + "do trecho solicitado.");

                    Socket coordinatorServer
                            = new Socket(
                                    Server.coordinator.getIpAddress(),
                                    Server.coordinator.getPort()
                            );

                    ObjectOutputStream output
                            = new ObjectOutputStream(
                                    coordinatorServer.getOutputStream()
                            );

                    output.flush();
                    output.writeObject("POST /send/authorization");
                    output.flush();

                    ObjectOutputStream outputBody
                            = new ObjectOutputStream(
                                    coordinatorServer.getOutputStream()
                            );

                    outputBody.flush();
                    outputBody.writeObject(null);
                    outputBody.flush();

                    output.close();
                    outputBody.close();

                    break;
                }
            } catch (IOException ioe) {
                System.err.println("Erro ao tentar enviar a confirmação de "
                        + "compra do trecho para o servidor coordenador.");
                System.out.println(ioe);
            }
        }
    }
}
