package models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import utils.RandomUtil;

/**
 * Classe para a leitura do arquivo.
 * 
 * @author Allan Capistrano
 * @author João Erick Barbosa
 */
public class Reader {
    /*-------------------------- Constantes ----------------------------------*/
    private static final String FILE_NAME = "route-network.txt";
    /*------------------------------------------------------------------------*/
    
    private File file;

    /**
     * Método construtor.
     */
    public Reader(){
        try {
            this.file = new File(FILE_NAME);
            file.createNewFile();
        } catch (IOException ioe) {
            System.err.println("Erro ao tentar criar o objeto \"file\".");
            System.out.println(ioe);
        }
    }

    /**
     * Monta um grafo a partir de um arquivo.
     * 
     * @param graph Graph - Grafo.
     * @param companyName String - Nome da companhia.
     * @param amountParts int - Quantidade de trechos para a companhia.
     * @return boolean
     */
    public boolean generateGraph(Graph graph, String companyName, int amountParts) {
        if(file.getName().compareTo(FILE_NAME) != 0) {
            return false;
        }
        
        List<String> completeFile  = new ArrayList<>();
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                completeFile.add(line);
            }
            br.close();
            
            /* Adicionando trechos aleatórios no grafo da companhia. */
            for (int i = 0; i < amountParts; i++) {
                int index = RandomUtil.generateInt(0, completeFile.size());
                String[] temp = completeFile.get(index).split(",");
                
                graph.addEdge(temp[0], 
                        temp[1], 
                        Float.parseFloat(temp[2]), 
                        companyName, 
                        Integer.parseInt(temp[3])
                );
                
                completeFile.remove(index);
            }
            
            return true;
        } catch (IOException ioe) {
            System.err.println("Erro ao tentar ler o arquivo e/ou montar o grafo.");
            System.out.println(ioe);
            
            return false;
        }
    }
    
    public File getFile() {
        return file;
    }
}
