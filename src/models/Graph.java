package models;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Classe do Grafo.
 *
 * @author Allan Capistrano
 * @author João Erick Barbosa
 */
public class Graph {

    private List<Vertex> vertices;
    private List<Edge> edges;
    private List<Edge> edgesMultiGraph;
    private List<Travel> travels;

    /**
     * Método construtor.
     */
    public Graph() {
        this.vertices = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.edgesMultiGraph = new ArrayList<>();
        this.travels = new ArrayList<>();
    }

    /**
     * Adiciona uma nova cidade ao grafo.
     *
     * @param cityName String - Nome da cidade.
     */
    public void addVertex(String cityName) {
        if (!this.vertices.contains(new Vertex(cityName))) {
            this.vertices.add(new Vertex(cityName));
        }
    }

    /**
     * Adiciona uma nova ligação entre cidades ao grafo.
     *
     * @param firstCity String - Primeira cidade.
     * @param secondCity String - Segunda cidade.
     * @param time double - Tempo de voo entre as duas cidades.
     * @param companyName String - Nome da companhia aérea.
     * @param amountSeat int - Quantidade de acentos.
     */
    public void addEdge(
            String firstCity,
            String secondCity,
            double time,
            String companyName,
            int amountSeat
    ) {
        
        if (!edgeExists(firstCity, secondCity)) {
            this.addVertex(firstCity);
            this.addVertex(secondCity);
            this.edges.add(
                    new Edge(
                            new Vertex(firstCity),
                            new Vertex(secondCity),
                            time,
                            companyName,
                            amountSeat
                    )
            );
            this.edgesMultiGraph.add(
                    new Edge(
                            new Vertex(firstCity),
                            new Vertex(secondCity),
                            time,
                            companyName,
                            amountSeat
                    )
            );
        } else {
            this.edgesMultiGraph.add(
                    new Edge(
                            new Vertex(firstCity),
                            new Vertex(secondCity),
                            time,
                            companyName,
                            amountSeat
                    )
            );
        }

    }

    /**
     * Encontra uma cidade no grafo.
     *
     * @param cityName String - Nome da cidade que se deseja encontrar.
     * @return Vertex || null.
     */
    public Vertex findVertex(String cityName) {
        for (Vertex vertex : this.vertices) {
            if (vertex.equals(new Vertex(cityName))) {
                return vertex;
            }
        }

        return null;
    }

    /**
     * Encontra as ligações entre as cidades.
     *
     * @param firstCity String - Nome da primeira cidade.
     * @param secondCity String - Nome da segunda cidade.
     * @return List<Edge>.
     */
    public List<Edge> findEdge(String firstCity, String secondCity) {
        List<Edge> temp = new ArrayList<>();

        for (Edge edge : this.edgesMultiGraph) {
            if (edge.equals(new Edge(firstCity, secondCity))) {
                temp.add(edge);
                /* TEMPORÁRIO */
                System.out.print(
                        edge.getFirstCity().getCityName() + " -> "
                        + edge.getSecondCity().getCityName()
                        + " | [" + edge.getCompanyName()
                        + ", " + edge.getTimeTravel() + "] "
                );
            }
        }
        /* TEMPORÁRIO */
        System.out.println("");

        return temp;
    }

    /**
     * Verifica se já existe uma ligação entre duas cidades.
     *
     * @param firstCity String - Nome da primeira cidade.
     * @param secondCity String - Nome da segunda cidade.
     * @return boolean.
     */
    public boolean edgeExists(String firstCity, String secondCity) {
        for (Edge edge : this.edges) {
            if (edge.equals(new Edge(firstCity, secondCity))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Econtra todos as cidades adjacentes de uma determinada cidade.
     *
     * @param cityName String - Nome da cidade que deseja encontrar as
     * adjacentes.
     * @return LinkedList<Vertex>.
     */
    public LinkedList<Vertex> adjacentNodes(String cityName) {
        LinkedList<Vertex> temp = new LinkedList<>();

        for (Edge edge : this.edges) {
            if (edge.getFirstCity().getCityName().equals(cityName)) {
                temp.add(edge.getSecondCity());
            }
        }

        return temp;
    }

    /**
     * Busca todas os caminhos possíveis entre duas cidades
     *
     * @param startCity String - Nome da cidade incial.
     * @param endCity String - Nome da cidade final.
     * @return List<Travel>.
     */
    public List<Travel> depthFirst(String startCity, String endCity) {
        LinkedList<Vertex> visited = new LinkedList();

        visited.add(new Vertex(startCity));

        this.travels.removeAll(this.travels);

        this.depthFirst(this, visited, new Vertex(endCity));

        return this.travels;
    }

    /**
     * Busca todas os caminhos possíveis entre duas cidades
     *
     * @param graph Graph - Grafo.
     * @param visited LinkedList<Vertex> - Lista das cidade que já foram
     * visitadas.
     * @param endCity String - Nome da cidade final.
     */
    private void depthFirst(Graph graph, LinkedList<Vertex> visited, Vertex endCity) {
        LinkedList<Vertex> vertices
                = graph.adjacentNodes(visited.getLast().getCityName());

        for (Vertex vertex : vertices) {
            if (visited.contains(vertex)) {
                continue;
            }

            if (vertex.getCityName().equals(endCity.getCityName())) {
                visited.add(vertex);
                /* Chama o método de exibição dos caminhos. */
                this.travels.add(generateTravel(visited));
                visited.removeLast();

                break;
            }
        }

        for (Vertex vertex : vertices) {
            if (visited.contains(vertex) || vertex.equals(endCity)) {
                continue;
            }

            visited.addLast(vertex);
            depthFirst(graph, visited, endCity);
            visited.removeLast();
        }
    }

    /**
     * Retorna um caminho completo entre duas cidades.
     *
     * @param visited LinkedList<Vertex> - Cidades que compõem o caminho.
     * @return Travel.
     */
    private Travel generateTravel(LinkedList<Vertex> cities) {
        Travel travel = new Travel();

        for (int i = 0; i < cities.size() - 1; i++) {
            travel.getRoute().add(findEdge(
                    cities.get(i).getCityName(),
                    cities.get(i + 1).getCityName()
            ));
        }
        /* TEMPORÁRIO */
        System.out.println("");

        return travel;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public int verticesSize() {
        return this.vertices.size();
    }

    public int edgesSize() {
        return this.edges.size();
    }
}
