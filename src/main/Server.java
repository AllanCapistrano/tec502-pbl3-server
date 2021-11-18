package main;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import models.Graph;
import models.Reader;
import models.ServerAddress;

/**
 * Classe do Servidor.
 *
 * @author Allan Capistrano
 * @author João Erick Barbosa
 */
public class Server {

    /*-------------------------- Constantes ----------------------------------*/
    private static String IP_ADDRESS;
    private static int PORT;
    private static String COMPANY_NAME;
    private static final int AMOUNT_OF_PARTS = 5;
    /*------------------------------------------------------------------------*/

    private static ServerSocket server;
    public static List<ServerAddress> serverAddress = new ArrayList<ServerAddress>();
    public static Graph graph = new Graph();
    public static Graph unifiedGraph = new Graph();
    private static ArrayList<ConnectionHandler> connHandler = new ArrayList<>();
    private static ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Scanner keyboardInput = new Scanner(System.in);
        int regionIndex = 0;

        /* Adicionando os servidores. */
        serverAddress.add(new ServerAddress("localhost", 12240, "Azul"));
        serverAddress.add(new ServerAddress("localhost", 12241, "GOL"));
        serverAddress.add(new ServerAddress("localhost", 12242, "TAM"));

        System.out.println("Selecione de qual companhia pertence este servidor: ");
        System.out.println("1 - Azul");
        System.out.println("2 - GOL");
        System.out.println("3 - TAM");
        System.out.print("> ");

        try {
            regionIndex = keyboardInput.nextInt() - 1;

            IP_ADDRESS = serverAddress.get(regionIndex).getIpAddress();
            PORT = serverAddress.get(regionIndex).getPort();
            COMPANY_NAME = serverAddress.get(regionIndex).getCompanyName();

            /* Removendo o endereço deste servidor. */
            serverAddress.remove(regionIndex);
        } catch (Exception e) {
            System.err.println("Erro ao dar entrada nas opções.");
            System.out.println(e);
            System.exit(0);
        }

        System.out.println("> Iniciando o servidor");

        try {
            /* Definindo o endereço e a porta do servidor. */
            Server.server = new ServerSocket();
            InetAddress addr = InetAddress.getByName(IP_ADDRESS);
            InetSocketAddress inetSocket = new InetSocketAddress(addr, PORT);
            server.bind(inetSocket);

            /* Montando o grafo */
            Reader reader = new Reader();
            reader.generateGraph(graph, COMPANY_NAME, AMOUNT_OF_PARTS);

            System.out.println("> Aguardando conexão");

            System.out.println(IP_ADDRESS);
            System.out.println(PORT);
            System.out.println(COMPANY_NAME);
            System.out.println(graph.verticesSize());
            System.out.println(graph.edgesSize());

            while (true) {
                /* Serviço que lida com as requisições utilizando threads. */
                ConnectionHandler connectionThread
                        = new ConnectionHandler(server.accept());
                connHandler.add(connectionThread);

                /* Executando as threads. */
                pool.execute(connectionThread);
            }
        } catch (BindException be) {
            System.err.println("A porta já está em uso.");
            System.out.println(be);
        } catch (IOException ioe) {
            System.err.println("Erro de Entrada/Saída.");
            System.out.println(ioe);
        }
    }
}
