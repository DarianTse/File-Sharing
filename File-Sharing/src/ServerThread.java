import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;

public class ServerThread implements Runnable {
    private Socket socket;
    private BufferedReader requestInput;
    private DataOutputStream responseOutput;

    ServerThread(Socket socket) throws IOException{
        this.socket = socket;
        requestInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        responseOutput = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        String line = null;
        try{
            line = requestInput.readLine();
            handleRequest(line);
        } catch(IOException e){
            e.printStackTrace();
        }finally {
            try{
                requestInput.close();
                responseOutput.close();
                socket.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    //Parses the request code and calls function for it
    public void handleRequest(String request) throws IOException{
        try{
            StringTokenizer tokenizer = new StringTokenizer(request);
            String command = tokenizer.nextToken();
            String uri = tokenizer.nextToken();

            File baseDir = Server.getInstance().getBaseDir();

            //Download a file from the server
            if(command.equalsIgnoreCase("DOWNLOAD")){
                //Get database path
                System.out.println("Download file request");
                sendFile(baseDir, uri);
            }
            if(command.equalsIgnoreCase("DIR"))
            {
                System.out.println("Sending Directory");
                //Send all filenames.extension to client
                sendFileList(baseDir);
            }
            if(command.equalsIgnoreCase("UPLOAD"))
            {
                System.out.println("Saving File to database");
                //String content = "";
                /*while(tokenizer.hasMoreTokens()){
                    content = content + tokenizer.nextToken() + " ";
                }*/
                saveFile(uri);
            }
        }catch (NoSuchElementException e){
            e.printStackTrace();
        }
    }

    public void saveFile(String uri) throws IOException {
        File newFile = new File(Server.getInstance().getBaseDir() + "/" + uri);
        //if it doesnt exist
        if (!newFile.exists()) {
            newFile.createNewFile();

            String line;
            PrintWriter fileOut = new PrintWriter(newFile);
            while ((line = requestInput.readLine()) != null) {
                fileOut.println(line);
            }
            fileOut.close();

        }
    }

    //Send all file names
    public void sendFileList(File file) throws IOException{
        String result = "";

        if(file.isDirectory()){ //If its a directory
            File[] contents = file.listFiles();
            for(File current: contents){
                sendResponse((current.getName() + "\n").getBytes());
            }
        }
    }

    //Send contents in file
    public void sendFile(File baseDir, String uri) throws IOException{
        File file = new File(baseDir, uri);

        if(!file.exists()){
            System.out.println("Could not find file in server");
        } else{
            byte[] content = new byte[(int)file.length()];
            FileInputStream fileIn = new FileInputStream(file);
            fileIn.read(content);
            fileIn.close();;
            sendResponse(content);
        }
    }

    private String getContentType(String filename) {
        if (filename.endsWith(".html") || filename.endsWith(".htm")) {
            return "text/html";
        } else if (filename.endsWith(".css")) {
            return "text/css";
        } else if (filename.endsWith(".js")) {
            return "text/javascript";
        } else if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".gif")) {
            return "image/gif";
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else {
            return "unknown";
        }
    }

    //send response with a byte[]
    private void sendResponse(byte[] content) throws IOException {
        responseOutput.write(content);
        responseOutput.flush();
    }




}