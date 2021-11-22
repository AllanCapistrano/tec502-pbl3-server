package main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import models.Travel;
import org.json.JSONObject;

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

            } else if (httpRequest.equals("POST /buy/authorization")) {

            } else if (httpRequest.equals("GET /graph")) {
                System.out.println("> Rota: /graph");
                System.out.println("\t Método: GET");
                this.sendGraph();
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

//            List<Travel> routes
//                    = Server.graph.depthFirst(firstCity, secondCity);
            List<Travel> routes
                    = Server.unifiedGraph.depthFirst(firstCity, secondCity);

            System.out.println("> Enviando as possíveis rotas (Qtd: " + routes.size() + ")...");

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
     * Requisita para as Fogs um certo número de pacientes, e salva os mesmos na
     * lista.
     *
     * @param address String - Endereço da Fog.
     * @param port int - Porta da Fog.
     * @param amount int - Quantidade de dispositivos de pacientes.
     */
    private void requestPatientsDeviceListToFog(String address, int port, int amount) {
        try {
            Socket connFog = new Socket(address, port);

            JSONObject json = new JSONObject();

            /* Definindo os dados que serão enviadas para o Fog Server. */
            json.put("method", "GET"); // Método HTTP
            json.put("route", "/patients/" + amount); // Rota

            ObjectOutputStream output
                    = new ObjectOutputStream(connFog.getOutputStream());

            /* Enviando a requisição para o Fog Server. */
            output.flush();
            output.writeObject(json);

            /* Recebendo a resposta do Fog Server. */
            ObjectInputStream response
                    = new ObjectInputStream(connFog.getInputStream());

            JSONObject jsonResponse = (JSONObject) response.readObject();

            /* Somente se a resposta possuir o método e a resposta certa, que os
            os dispositivos enviados serão adicionados na lista do servidor. */
            if (jsonResponse.getString("method").equals("POST")
                    && jsonResponse.getString("route").equals("/patients")) {
                System.out.println("\n> Processando a requisição");
                System.out.println("\tMétodo POST");
                System.out.println("\t\tRota: " + jsonResponse.getString("route"));
                System.out.println("> Recebendo os dispositivos enviados pelas "
                        + "Fogs.");

//                this.addPatientDevicesToServer(jsonResponse.getJSONArray("body"));
            }

            output.close();
            response.close();
            connFog.close();
        } catch (IOException ioe) {
            System.err.println("Erro ao requisitar uma certa quantidade de "
                    + "pacientes para a Fog.");
            System.out.println(ioe);
        } catch (ClassNotFoundException cnfe) {
            System.err.println("Classe JSONObject não foi encontrada");
            System.out.println(cnfe);
        }
    }
}
