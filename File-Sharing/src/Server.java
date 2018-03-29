import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    //singleton
    private static Server instance;
    public static Server getInstance(){return instance;}

    private ServerSocket serverSocket = null;
    private static int port = 20000;

    //server folder
    File baseDir = new File("ServerDatabase");
    public File getBaseDir(){return baseDir;}

    public Server(int port) throws IOException{
        if(instance == null){
            instance = this;
        }

        //Create server folder if it doesnt exist
        if(!baseDir.exists()){
            System.out.println("creating directory at: " + baseDir.getAbsolutePath());
            baseDir.mkdir();
        }

        serverSocket = new ServerSocket(port);
    }

    public void handleRequests() throws IOException{
        System.out.println("Waiting for connections...");

        while(true){
            Socket clientSocket = serverSocket.accept();
            Runnable r = new ServerThread(clientSocket);
            Thread thread = new Thread(r);
            thread.start();
        }
    }

    public static void main(String[] args) throws IOException {
        try{
            Server server = new Server(port);
            server.handleRequests();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
