package final_project_1;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class httpServer {
    public static void main(String[] argv) throws Exception{
        ServerSocket welcomeSocket = new ServerSocket(8081);
        while(true){
            // 为每一个新连接新建一个线程并运行
            Socket connectionSocket = welcomeSocket.accept();
            httpRequest request = new httpRequest(connectionSocket);
            Thread thread = new Thread(request);
            thread.start();
        }
    }

    public static class httpRequest implements Runnable{
        public static String CRLF = "\r\n";  // 换行符
        public Socket socket;

        public httpRequest(Socket socket) throws Exception{
            this.socket = socket;
        }

        public void run(){
            try{
                processRequest();
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        public void processRequest() throws Exception{
            InputStream is = socket.getInputStream();  // 从客户端读
            DataOutputStream os = new DataOutputStream(socket.getOutputStream());  // 向客户端写，字节输出流
            BufferedReader br = new BufferedReader(new InputStreamReader(is));  // 读入客户端字符流
            String requestLine = br.readLine();  // 读入用户的请求行（第一行）
            System.out.println(requestLine);
            String headerLine = null;
            while((headerLine = br.readLine()).length() != 0){
                System.out.println(headerLine);  // 读入用户的请求报文的头部（单独一行回车符处结束），后面为正文部分
            }

            // 从请求行中提取文件名
            StringTokenizer tokens = new StringTokenizer(requestLine);  // 分割
            tokens.nextToken();  // 跳过第一个词（方法）
            String fileName = tokens.nextToken();
            // 判断请求内容
            if(fileName.equals("/shutdown")){
                System.exit(0);
            }
            // 开启代理时会自动加http://
            else if(fileName.startsWith("http://")){
                // http://后面的内容
                String line = fileName.substring(7);
                if(line.startsWith("localhost")){
                    String subline = line.substring(14);
                    if(subline.equals("/shutdown")){
                        System.exit(0);
                    }
                    else{
                        fileName = "D:/Java/Computer_network/webroot" + subline;
                    }
                }
            }
            // localhost:8081后面的内容
            else{
                fileName = "D:/Java/Computer_network/webroot" + fileName;
            }

            FileInputStream fis = null;
            boolean fileExists = true;  // 判断请求对象是否存在
            // 打开读该文件的流，若能打开，则文件存在
            try{
                fis = new FileInputStream(fileName);
            } catch (FileNotFoundException e){
                fileExists = false;
            }
            // 响应报文
            String statusLine = null;
            String contentTypeLine = null;
            if(fileExists){
                // 请求文件存在
                statusLine = "HTTP/1.1 200 OK" + CRLF;
                contentTypeLine = "Content-type: text/html" + CRLF;
            }
            else{
                // 请求文件不存在
                statusLine = "HTTP/1.1 404 File Not Found" + CRLF;
                contentTypeLine = "Content-type: text/html" + CRLF;
            }
            // 写回到客户端
            os.writeBytes(statusLine);
            os.writeBytes(contentTypeLine);
            os.writeBytes(CRLF);
            if (!fileExists) {
                fis = new FileInputStream("D:/Java/Computer_network/webroot/404.html");
            }
            sendBytes(fis, os);  // 写入请求的文件内容
            fis.close();
            os.close();
            br.close();
            socket.close();
        }

        private void sendBytes(FileInputStream fis, OutputStream os) throws Exception{
            byte[] buffer = new byte[1024];
            int bytes = 0;
            // 读到buffer中写入
            while((bytes = fis.read(buffer)) != -1){
                os.write(buffer, 0, bytes);
            }
        }
    }
}
