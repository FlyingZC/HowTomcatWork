package ex02.pyrmont;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * @author flyingzc
 * 可处理静态资源  和  请求servlet资源,如http://localhost:9080/servletClass.
 * 比如请求servlet资源http://localhost:9080/servlet/PrimitiveServlet
 */
public class HttpServer1
{

    /** WEB_ROOT is the directory where our HTML and other files reside.
     *  For this package, WEB_ROOT is the "webroot" directory under the working
     *  directory.
     *  The working directory is the location in the file system
     *  from where the java command was invoked.
     */
    // shutdown command
    private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";

    // the shutdown command received
    private boolean shutdown = false;

    public static void main(String[] args)
    {
        HttpServer1 server = new HttpServer1();
        server.await();
    }

    public void await()
    {
        ServerSocket serverSocket = null;
        int port = Constants.PORT;
        try
        {
            serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
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
                socket = serverSocket.accept();
                input = socket.getInputStream();
                output = socket.getOutputStream();

                // create Request object and parse
                Request request = new Request(input);
                request.parse();

                // create Response object
                Response response = new Response(output);
                response.setRequest(request);

                // check if this is a request for a servlet or a static resource
                // a request for a servlet begins with "/servlet/"
                //处理对Servlet类 资源的请求
                if(request.getUri() == null)
                {
                    return;
                }
                if (request.getUri().startsWith("/servlet/"))
                {
                    ServletProcessor1 processor = new ServletProcessor1();
                    processor.process(request, response);
                }
                else
                {//处理对静态资源的请求
                    StaticResourceProcessor processor = new StaticResourceProcessor();
                    processor.process(request, response);
                }

                // Close the socket
                socket.close();
                //check if the previous URI is a shutdown command
                shutdown = request.getUri().equals(SHUTDOWN_COMMAND);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
