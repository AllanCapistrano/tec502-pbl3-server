package main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
    private static final int AMOUNT_OF_PARTS = 5;
    private static final int SLEEP_TIME = 30000;
    /*------------------------------------------------------------------------*/

    private static String ipAddress;
    private static int port;
    private static String companyName;
    private static ServerSocket server;
    public static List<ServerAddress> serverAddress
            = new ArrayList<ServerAddress>();
    public static Graph graph = new Graph();
    public static Graph unifiedGraph = new Graph();
    private static ArrayList<ConnectionHandler> connHandler = new ArrayList<>();
    private static ExecutorService pool = Executors.newCachedThreadPool();
    private static boolean isThreadCreated = false;

    public static void main(String[] args)
            throws IOException, ClassNotFoundException {
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

            ipAddress = serverAddress.get(regionIndex).getIpAddress();
            port = serverAddress.get(regionIndex).getPort();
            companyName = serverAddress.get(regionIndex).getCompanyName();

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
            InetAddress addr = InetAddress.getByName(ipAddress);
            InetSocketAddress inetSocket = new InetSocketAddress(addr, port);
            server.bind(inetSocket);

            /* Montando o grafo */
            Reader reader = new Reader();
            reader.generateGraph(graph, companyName, AMOUNT_OF_PARTS);

            System.out.println("> Aguardando conexão");

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

    /**
     * Unifica os grafos de todas companhias.
     */
    public static void unifyGraph() {
        if (!isThreadCreated) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        /* Limpando o grafo unificado. */
                        unifiedGraph.cleanUnifiedGraph();
                        /* Adicionando ao unificado o grafo da própria 
                        companhia. */
                        unifiedGraph.unifyGraph(graph);

                        for (ServerAddress server : serverAddress) {
                            try {
                                Socket socket
                                        = new Socket(
                                                server.getIpAddress(),
                                                server.getPort()
                                        );

                                if (socket.isConnected()) {
                                    ObjectOutputStream output
                                            = new ObjectOutputStream(
                                                    socket.getOutputStream()
                                            );

                                    output.flush();
                                    output.writeObject("GET /graph");
                                    output.flush();

                                    ObjectInputStream input
                                            = new ObjectInputStream(
                                                    socket.getInputStream()
                                            );

                                    /* Recebendo o grafo. */
                                    Graph otherGraph
                                            = (Graph) input.readObject();

                                    System.out.println(otherGraph);

                                    /* Unificando o grafo da companhia com as 
                                demais. */
                                    unifiedGraph.unifyGraph(otherGraph);

                                    output.close();
                                    input.close();
                                }

                                socket.close();
                            } catch (IOException ioe) {
                                System.err.println("Erro ao tentar se "
                                        + "comunicar com o servidor: "
                                        + server.getCompanyName()
                                );
                                System.out.println(ioe);
                            } catch (ClassNotFoundException cnfe) {
                                System.err.println("A classe Graph não foi "
                                        + "encontrada.");
                                System.out.println(cnfe);
                            }
                        }

                        try {
                            Thread.sleep(SLEEP_TIME);
                        } catch (InterruptedException ie) {
                            System.err.println("Thread finalizada de maneira "
                                    + "inesperada.");
                            System.out.println(ie);
                        }
                    }
                }
            });

            /* Finalizar a thread de requisição quando fechar o programa. */
            thread.setDaemon(true);
            /* Iniciar a thread de requisições. */
            thread.start();
        }

        isThreadCreated = true;
    }
}
