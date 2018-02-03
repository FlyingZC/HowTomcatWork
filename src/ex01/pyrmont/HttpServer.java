package ex01.pyrmont;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer
{

    /** WEB_ROOT is the directory where our HTML and other files reside.
     *  For this package, WEB_ROOT is the "webroot" directory under the working
     *  directory.
     *  The working directory is the location in the file system
     *  from where the java command was invoked.
     */
    //指定该web服务器 可以处理指定目录中的静态资源的请求.包含webroot及其子目录.
    //将 静态资源 放在该目录下
    //若请求静态资源,使用http://machineName:port/staticResource形式的URL,D:\workspace-e3\HowTomcatWorks\webroot
    private static final String MAVEN_PREFIX = "/WebContent";
    public static final String WEB_ROOT = System.getProperty("user.dir") + MAVEN_PREFIX + File.separator;

    //指定关闭服务器的命令.若要关闭该服务器,使用如下URL,http://localhost:9080/SHUTDOWN
    // shutdown command
    private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";

    // the shutdown command received
    private boolean shutdown = false;

    private static final int PORT = 9080;

    public static void main(String[] args)
    {
        HttpServer server = new HttpServer();
        server.await();
    }

    /**
    * 监听 客户端请求
    */
    public void await()
    {
        ServerSocket serverSocket = null;
        try
        {
            serverSocket = new ServerSocket(PORT, 1, InetAddress.getByName("127.0.0.1"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        // Loop waiting for a request
        while (!shutdown)
        {
            Socket socket = null;
            InputStream input = null;
            OutputStream output = null;
            try
            {
                //accept()服务端 会一直 等待客户端请求
                socket = serverSocket.accept();
                input = socket.getInputStream();//相对于服务端,serverSocket的input就是从客户端传上来的.用于创建request
                output = socket.getOutputStream();//相对于服务端,serverSocket的output就是从服务端传出去的.用于创建response

                // create Request object and parse,通过输入流创建request对象
                Request request = new Request(input);
                request.parse();

                // create Response object,创建response
                Response response = new Response(output);
                response.setRequest(request);
                response.sendStaticResource();

                // Close the socket,关闭套接字
                socket.close();

                //check if the previous URI is a shutdown command
                shutdown = request.getUri().equals(SHUTDOWN_COMMAND);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                continue;
            }
        }
    }
}
