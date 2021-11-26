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
import utils.RandomUtil;

/**
 * Classe do Servidor.
 *
 * @author Allan Capistrano
 * @author João Erick Barbosa
 */
public class Server {

    /*-------------------------- Constantes ----------------------------------*/
    private static final int AMOUNT_OF_PARTS = 10;
    private static final int UNIFY_TIME = 30000;
    private static final int ELECTION_TIME = 10000;
    /*------------------------------------------------------------------------*/

    private static String ipAddress;
    private static int port;
    public static String companyName;
    private static ServerSocket server;
    public static List<ServerAddress> serverAddress
            = new ArrayList<ServerAddress>();
    public static Graph graph = new Graph();
    public static Graph unifiedGraph = new Graph();
    private static ArrayList<ConnectionHandler> connHandler = new ArrayList<>();
    private static ExecutorService pool = Executors.newCachedThreadPool();
    private static boolean isThreadCreated = false;

    /*------------------------ Bully Algorithm -------------------------------*/
    public static volatile ServerAddress coordinator;
    public static volatile boolean electionActive = false;
    public static volatile boolean serverStarted = true;
    public static volatile int requestsSize = RandomUtil.generateInt(0, 10);
    private static int amountConnections = 0;

    /*------------------------------------------------------------------------*/
    public static void main(String[] args)
            throws IOException, ClassNotFoundException {
        Scanner keyboardInput = new Scanner(System.in);
        int companyIndex = 0;

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
            companyIndex = keyboardInput.nextInt() - 1;

            ipAddress = serverAddress.get(companyIndex).getIpAddress();
            port = serverAddress.get(companyIndex).getPort();
            companyName = serverAddress.get(companyIndex).getCompanyName();

            /* Removendo o endereço deste servidor. */
            serverAddress.remove(companyIndex);
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

            /* Thread para lidar com a eleição do coordenador. */
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {

                        for (ServerAddress server : serverAddress) {
                            try {
                                Socket socket
                                        = new Socket(
                                                server.getIpAddress(),
                                                server.getPort()
                                        );

                                if (socket.isConnected()) {
                                    amountConnections++;
                                    System.out.println("\n> Qtd. de servidores "
                                            + "vivos: "
                                            + (amountConnections + 1) + "\n"
                                    );
                                    
                                    ObjectOutputStream output
                                            = new ObjectOutputStream(
                                                    socket.getOutputStream()
                                            );
                                    output.flush();
                                    output.writeObject("POST /ping");
                                    output.flush();

                                    ObjectOutputStream outputBody
                                            = new ObjectOutputStream(
                                                    socket.getOutputStream()
                                            );
                                    outputBody.flush();
                                    outputBody.writeObject(companyName);
                                }

                                socket.close();
                            } catch (IOException ioe) {
                                System.err.println("Erro ao tentar se "
                                        + "comunicar com o servidor: "
                                        + server.getCompanyName()
                                );
                            }
                        }

                        /* Caso o servidor coordenador esteja ofline, incia 
                        uma nova eleição. */
                        try {
                            if (coordinator != null
                                    && !coordinator.getCompanyName()
                                            .equals(companyName)) {
                                Socket socketCoordinator
                                        = new Socket(
                                                coordinator.getIpAddress(),
                                                coordinator.getPort()
                                        );
                                if (socketCoordinator.isConnected()) {
                                    ObjectOutputStream output
                                            = new ObjectOutputStream(
                                                    socketCoordinator
                                                            .getOutputStream()
                                            );
                                    output.flush();
                                    output.writeObject("GET /coordinatorAlive");

                                    ObjectInputStream inputBody
                                            = new ObjectInputStream(
                                                    socketCoordinator
                                                            .getInputStream()
                                            );
                                    System.out.println("[Coordenador] O "
                                            + "servidor da companhia "
                                            + ((String) inputBody.readObject())
                                            + " vivo!");
                                }
                            }

                        } catch (IOException ex) {
                            System.err.println("\nCoordenador OFF.\n");
                            startElection();
                        } catch (ClassNotFoundException cnfe) {
                            System.err.println("A classe String não foi "
                                    + "encontrada.");
                            System.out.println(cnfe);
                        }

                        if (serverStarted
                                && amountConnections > 0
                                && !electionActive) {
                            coordinator = null;
                            /* Inicia uma nova eleição quando o servidor é 
                            inciado. */
                            startElection();

                        } else if (coordinator != null
                                && coordinator.getCompanyName()
                                        .equals(companyName)
                                && !electionActive) {
                            /* Somente o servidor coordenador pode inciar uma 
                            nova eleição de tempos em tempos. */
                            startElection();

                        } else if (amountConnections == 0) {
                            /* Caso só tenha um servidor online, o mesmo é o 
                            coordenador. */
                            coordinator = new ServerAddress(
                                    ipAddress,
                                    port,
                                    companyName
                            );
                            
                            System.out.println("Sou o coordenador: "
                                    + coordinator.getCompanyName());
                        }

                        serverStarted = false;
                        amountConnections = 0;

                        try {
                            Thread.sleep(ELECTION_TIME);
                        } catch (InterruptedException ie) {
                            System.err.println("Thread de eleição finalizada "
                                    + "de maneira inesperada.");
                            System.out.println(ie);
                        }
                    }
                }
            });

            /* Finalizar a thread de eleição quando fechar o programa. */
            thread.setDaemon(true);
            /* Iniciar a thread de eleição. */
            thread.start();

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

                                    /* Unificando o grafo da companhia com as 
                                demais. */
                                    unifiedGraph.unifyGraph(otherGraph);

                                    output.close();
                                    input.close();
                                }

                                socket.close();
                            } catch (IOException ioe) {
                                System.err.println("\nErro ao tentar se "
                                        + "comunicar com o servidor: "
                                        + server.getCompanyName() + "\n"
                                );
                            } catch (ClassNotFoundException cnfe) {
                                System.err.println("A classe Graph não foi "
                                        + "encontrada.");
                                System.out.println(cnfe);
                            }
                        }

                        try {
                            Thread.sleep(UNIFY_TIME);
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

    /**
     * Inicia uma nova eleição para definir o servidor coordenador.
     */
    private synchronized static void startElection() {
        electionActive = true;
        int max = 0;
        int coordinatorIndex;
        List<Integer> amountRequests = new ArrayList<>();
        List<Integer> indexList = new ArrayList<>();

        /* Adicionando a quantidade de requisições deste próprio servidor. */
        amountRequests.add(requestsSize);

        System.out.println("Quantidade de requisições da " + companyName + ": "
                + requestsSize);

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
                    output.writeObject("GET /startElection");
                    output.flush();

                    ObjectInputStream input
                            = new ObjectInputStream(
                                    socket.getInputStream()
                            );

                    amountRequests.add((Integer) input.readObject());

                    output.close();
                    input.close();
                }

                socket.close();
            } catch (IOException ioe) {
                System.err.println("Erro ao tentar se "
                        + "comunicar com o servidor: "
                        + server.getCompanyName()
                );
            } catch (ClassNotFoundException cnfe) {
                System.err.println("A classe Graph não foi "
                        + "encontrada.");
                System.out.println(cnfe);
            }
        }

        /* Encontra qual o maior número de requisições. */
        for (int i = 0; i < amountRequests.size(); i++) {
            if (max < amountRequests.get(i)) {
                max = amountRequests.get(i);
            }
        }

        /* Verifica quais servidores possuem a maior quantidade de 
        requisições. */
        for (int i = 0; i < amountRequests.size(); i++) {
            if (max == amountRequests.get(i)) {
                indexList.add(i);
            }
        }

        /* Caso possua mais de um servidor com a maior quantidade de 
        requisições, o coordenador é definido de maneira aleatória entre eles.*/
        if (indexList.size() > 1) {
            coordinatorIndex
                    = indexList.get(RandomUtil.generateInt(0, indexList.size()));
        } else {
            coordinatorIndex = indexList.get(0);
        }

        setCoordinator(coordinatorIndex);
    }

    /**
     * Define o novo servidor coordenador.
     *
     * @param coordinatorIndex int - Índice do coordenador na lista de endereços
     * servidores.
     */
    private static void setCoordinator(int coordinatorIndex) {
        if (coordinatorIndex == 0) {
            coordinator = new ServerAddress(ipAddress, port, companyName);
        } else {
            coordinator = serverAddress.get(coordinatorIndex - 1);
        }

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
                    output.writeObject("POST /coordinator");

                    ObjectOutputStream outputBody
                            = new ObjectOutputStream(
                                    socket.getOutputStream()
                            );
                    outputBody.flush();
                    outputBody.writeObject(coordinator);

                    output.close();
                    outputBody.close();
                }

                socket.close();
            } catch (IOException ioe) {
                System.err.println("Erro ao tentar se "
                        + "comunicar com o servidor: "
                        + server.getCompanyName()
                );
            }
        }

        electionActive = false;
    }
}
