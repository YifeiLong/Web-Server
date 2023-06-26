package final_project_1;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class proxyServer {
    public static void main(String[] argv) throws Exception{
        ServerSocket welcomeSocket = new ServerSocket(8081);
        // 为新连接入客户端新建线程处理
        while(true){
            Socket connectionSocket = welcomeSocket.accept();
            httpRequest request = new httpRequest(connectionSocket);
            Thread thread = new Thread(request);
            thread.start();
        }
    }

    public static class httpRequest implements Runnable{
        static String CRLF = "\r\n";
        Socket socket;

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
            InputStream is = socket.getInputStream();  // 从客户端读入
            DataOutputStream os = new DataOutputStream(socket.getOutputStream());  // 向客户端写
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String requestLine = br.readLine();  // 读入用户的请求行
            System.out.println(requestLine);
            String headerLine = null;
            while((headerLine = br.readLine()).length() != 0){
                System.out.println(headerLine);
            }
            // 从请求行中提取文件名
            StringTokenizer tokens = new StringTokenizer(requestLine);
            tokens.nextToken();  // 跳过第一个词（方法）
            String fileName = tokens.nextToken();

            // 不开代理：直接 GET /index.html...
            // 开代理：http://localhost:8081/index.html or 直接 GET http://index.html
            // 开代理，其他http://后面直接接要请求的外部网页
            if(fileName.equals("/shutdown")){
                System.exit(0);
            }
            else if(fileName.startsWith("http://")){
                String line1 = fileName.substring(7);
                if(line1.startsWith("localhost")){
                    String line2 = line1.substring(14);
                    if(line2.equals("/shutdown")){
                        System.exit(0);
                    }
                    else{
                        fileName = "D:/Java/Computer_network/webroot" + line2;
                        getLocalFile(os, fileName);
                    }
                }
                else if(line1.equals("index.html")){
                    fileName = "D:/Java/Computer_network/webroot/index.html";
                    getLocalFile(os, fileName);
                }
                // 外部网页
                else{
                    proxyExternalUrl(os, fileName);
                    os.close();
                    br.close();
                    socket.close();
                    return;
                }
            }
            // 本地文件
            else{
                fileName = "D:/Java/Computer_network/webroot" + fileName;
                getLocalFile(os, fileName);
            }

            os.close();
            br.close();
            socket.close();
        }

        private void getLocalFile(DataOutputStream os, String fileName) throws Exception {
            FileInputStream fis = null;
            boolean fileExists = true;  // 判断请求对象是否存在
            try{
                fis = new FileInputStream(fileName);
            } catch (FileNotFoundException e){
                fileExists = false;
            }
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
            os.writeBytes(statusLine);
            os.writeBytes(contentTypeLine);
            os.writeBytes(CRLF);
            if (!fileExists) {
                fis = new FileInputStream("D:/Java/Computer_network/webroot/404.html");
            }
            sendBytes(fis, os);
            fis.close();
        }

        private void sendBytes(FileInputStream fis, OutputStream os) throws Exception{
            byte[] buffer = new byte[1024];
            int bytes = 0;
            while((bytes = fis.read(buffer)) != -1){
                os.write(buffer, 0, bytes);
            }
        }

        private void proxyExternalUrl(DataOutputStream clientWriter, String requestUrl) throws IOException{
            URL url = new URL(requestUrl);  // 根据字符串新建url
            // HttpURLConnection类可以向指定网站发送GET或者POST请求
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();  // 建立连接
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();  // 响应状态码
            String statusLine = "HTTP/1.1 " + responseCode + " " + connection.getResponseMessage() + CRLF;  // 向目标服务器请求的结果
            clientWriter.writeBytes(statusLine);  // 向客户端写入

            // 获取目标服务器的响应头部信息
            Map<String, List<String>> headers = connection.getHeaderFields();  // 获取header中每一项内容及其对应的字符串
            // 比如Date:..., Content-Type:...
            // 向客户端不断写入，写入整个响应报文的header
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String headerKey = entry.getKey();
                if (headerKey != null) {
                    for (String headerValue : entry.getValue()) {
                        clientWriter.writeBytes(headerKey + ": " + headerValue + CRLF);
                    }
                }
            }
            clientWriter.writeBytes(CRLF);  // header后空行，接下来是正文
            clientWriter.flush();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读入目标服务器的响应内容，写入buffer
                InputStream inputStream = connection.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    // 向客户端写入正文
                    clientWriter.write(buffer, 0, bytesRead);
                }
                clientWriter.flush();
                inputStream.close();
            }
            connection.disconnect();  // 关闭连接
        }
    }
}
