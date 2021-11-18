package main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import org.json.JSONArray;
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
        
        if(httpRequest.equals("GET /routes")) {
            
        } else if(httpRequest.equals("POST /buy")) {
            
        } else if(httpRequest.equals("POST /buy/authorization")) {
            
        } else if(httpRequest.equals("GET /graph")) {
            
        }
        
        System.out.println("");
    }

    private void sendRoutes() {
        try {
            String cities[] = ((String) this.input.readObject()).split(",");
            
            Server.unifiedGraph.depthFirst(cities[0], cities[1]);
            
        } catch (IOException ioe) {
            System.err.println("Erro de Entrada/Saída.");
            System.out.println(ioe);
        } catch (ClassNotFoundException cnfe) {
            System.err.println("Classe String não foi encontrada.");
            System.out.println(cnfe);
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
