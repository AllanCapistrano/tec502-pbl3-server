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
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import models.Edge;
import models.Graph;
import models.Reader;
import models.ServerAddress;
import models.Ticket;
import utils.RandomUtil;

/**
 * Classe do Servidor.
 *
 * @author Allan Capistrano
 * @author João Erick Barbosa
 */
public class Server {

    /*-------------------------- Constantes ----------------------------------*/
    private static final int AMOUNT_OF_PARTS = 18;
    private static final int UNIFY_TIME = 30000;
    private static final int ELECTION_TIME = 10000;
    private static final int BUY_TIME = 10000;
    public static final int MAX_NUMBER_TIMES_COORDINATOR = 3;
    /*------------------------------------------------------------------------*/

    public static Graph graph = new Graph();
    public static Graph unifiedGraph = new Graph();
    public static String companyName;

    public static volatile boolean purchaseFlag = true;

    public static List<ServerAddress> serverAddress
            = new ArrayList<ServerAddress>();
    public static List<Edge> routes = new ArrayList<>();
    public static List<Ticket> tickets
            = Collections.synchronizedList(new ArrayList());
    public static List<Socket> interfaceClients
            = Collections.synchronizedList(new ArrayList());

    public static Stack<Edge> purchasesAccepted = new Stack<>();
    public static Stack<Edge> purchasesDenied = new Stack<>();

    private static String ipAddress;
    private static int port;
    private static ServerSocket server;
    private static ArrayList<ConnectionHandler> connHandler = new ArrayList<>();
    private static ExecutorService pool = Executors.newCachedThreadPool();
    private static boolean isThreadCreated = false;

    /*------------------------ Bully Algorithm -------------------------------*/
    public static volatile ServerAddress coordinator;
    public static volatile boolean electionActive = false;
    public static volatile boolean serverStarted = true;
    public static int numberTimesCoordinator = 0;
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
            routes.addAll(graph.getEdges());

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
                                            + "online: "
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

            /* Thread para lidar com a compra das passagens. */
            Thread threadBuy = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        if (tickets.size() > 0) {
                            int index = 0;

                            for (Ticket ticket : tickets) {
                                for (int i = 0; i < ticket.getListRoutes().size(); i++) {
                                    if (coordinator != null && coordinator.getCompanyName().equals(companyName)) {
                                        if (ticket.getListCompanyNames().get(i).equals(companyName)) {
                                            for (Edge route : routes) {
                                                if (route.equals(ticket.getListRoutes().get(i)) && route.getAmountSeat() > 0) {
                                                    System.out.println("> Compra do trecho " + route.getFirstCity().getCityName() + " -> " + route.getSecondCity().getCityName() + " realziada com sucesso!");
                                                    System.out.println("Qtd assentos antes: " + route.getAmountSeat());
                                                    route.setAmountSeat(route.getAmountSeat() - 1);
                                                    System.out.println("Qtd assentos depois: " + route.getAmountSeat());

                                                    /* Adicionando a compra na pilha de compras realizadas com sucesso. */
                                                    purchasesAccepted.push(route);

                                                } else if (route.equals(ticket.getListRoutes().get(i)) && route.getAmountSeat() == 0) {
                                                    System.out.println("> Não foi possível comprar o trecho " + route.getFirstCity().getCityName() + " -> " + route.getSecondCity().getCityName());
                                                    System.out.println("Qtd assentos: " + route.getAmountSeat());

                                                    /* Adicionando a compra na pilha de compras que não foram realizadas. */
                                                    purchasesDenied.push(route);
                                                    purchaseFlag = false;

                                                }
                                            }
                                        } else {
                                            for (ServerAddress server : serverAddress) {
                                                if (ticket.getListCompanyNames().get(i).equals(server.getCompanyName())) {
                                                    try {
                                                        Socket socket
                                                                = new Socket(
                                                                        server.getIpAddress(),
                                                                        server.getPort()
                                                                );

                                                        if (socket.isConnected()) {
                                                            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());

                                                            output.flush();
                                                            output.writeObject("POST /buy/authorization");
                                                            output.flush();

                                                            ObjectOutputStream outputBody = new ObjectOutputStream(socket.getOutputStream());

                                                            outputBody.flush();
                                                            outputBody.writeObject(ticket.getListRoutes().get(i));
                                                            outputBody.flush();

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
                                            }
                                        }
                                    }
                                }

                                if (coordinator != null && coordinator.getCompanyName().equals(companyName)) {
                                    while ((purchasesAccepted.size() + purchasesDenied.size()) < ticket.getListRoutes().size()) {
                                        /* Laço para espera da autorização da compra de trechos de outras companhias */
                                    }

                                    try {
                                        ObjectOutputStream outputInterface = new ObjectOutputStream(interfaceClients.get(index).getOutputStream());

                                        outputInterface.flush();
                                        outputInterface.writeObject(purchaseFlag);

                                        if (!purchaseFlag) {
                                            for (int i = 0; i < purchasesAccepted.size(); i++) {
                                                if (purchasesAccepted.get(i).getCompanyName().equals(companyName)) {
                                                    for (int j = 0; j < routes.size(); j++) {
                                                        if (purchasesAccepted.get(i).equals(routes.get(j))) {
                                                            routes.get(j).setAmountSeat(routes.get(j).getAmountSeat() + 1);
                                                        }
                                                    }
                                                } else {
                                                    for (ServerAddress server : serverAddress) {
                                                        if (purchasesAccepted.get(i).getCompanyName().equals(server.getCompanyName())) {
                                                            try {
                                                                Socket socket
                                                                        = new Socket(
                                                                                server.getIpAddress(),
                                                                                server.getPort()
                                                                        );

                                                                if (socket.isConnected()) {
                                                                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());

                                                                    output.flush();
                                                                    output.writeObject("POST /buy/cancel");
                                                                    output.flush();

                                                                    ObjectOutputStream outputBody = new ObjectOutputStream(socket.getOutputStream());

                                                                    outputBody.flush();
                                                                    outputBody.writeObject(purchasesAccepted.get(i));
                                                                    outputBody.flush();

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
                                                    }
                                                }
                                            }
                                            purchaseFlag = true;
                                        }

                                        outputInterface.close();
                                    } catch (IOException ioe) {
                                        System.err.println("Não foi possível se comunicar de volta com a interface.");
                                        System.out.println(ioe);
                                    }

                                    purchasesAccepted.removeAll(purchasesAccepted);
                                    purchasesDenied.removeAll(purchasesDenied);
                                } else {
                                    break;
                                }

                                index++;
                            }

                            if (coordinator != null && coordinator.getCompanyName().equals(companyName)) {
                                tickets.removeAll(tickets);
                                interfaceClients.removeAll(interfaceClients);
                            }
                        }

                        try {
                            Thread.sleep(BUY_TIME);
                        } catch (InterruptedException ie) {
                            System.err.println("Thread de compra das passagens "
                                    + "finalizada de maneira inesperada.");
                            System.out.println(ie);
                        }
                    }
                }
            });

            /* Finalizar a thread de eleição quando fechar o programa. */
            threadBuy.setDaemon(true);
            /* Iniciar a thread de eleição. */
            threadBuy.start();

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
        amountRequests.add(tickets.size());

        System.out.println("Quantidade de requisições da " + companyName + ": "
                + tickets.size());

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
