package ex03.pyrmont.connector.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author flyingzc
 * 连接器类 HttpConnector等待HTTP请求
 */
public class HttpConnector implements Runnable
{

    boolean stopped;

    private String scheme = "http";

    public String getScheme()
    {
        return scheme;
    }

    public void run()
    {
        ServerSocket serverSocket = null;
        int port = 9080;
        try
        {
            serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        while (!stopped)
        {
            // Accept the next incoming connection from the server socket
            Socket socket = null;
            try
            {
                socket = serverSocket.accept();
            }
            catch (Exception e)
            {
                continue;
            }
            // Hand this socket off to an HttpProcessor
            HttpProcessor processor = new HttpProcessor(this);
            processor.process(socket);
        }
    }

    public void start()
    {
        // 新启线程,传入当前类的实例.当前类实现Runnable接口.则每一次请求都会新启一个线程,则每一次请求全局变量 会是线程安全的???(zc)
        Thread thread = new Thread(this);
        thread.start();//start()后 会 执行Runnable中的run()方法
    }
}
