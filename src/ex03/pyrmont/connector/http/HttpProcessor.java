package ex03.pyrmont.connector.http;

import ex03.pyrmont.ServletProcessor;
import ex03.pyrmont.StaticResourceProcessor;

import java.net.Socket;
import java.io.OutputStream;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.StringManager;

/* this class used to be called HttpServer */
/**
 * @author flyingzc
 * 连接器支持类
 * HttpProcessor负责创建Request和Response对象
 */
public class HttpProcessor
{

    public HttpProcessor(HttpConnector connector)
    {
        this.connector = connector;
    }

    /**
     * The HttpConnector with which this processor is associated.与该 processor 连接的connector
     */
    private HttpConnector connector = null;

    private HttpRequest request;

    private HttpRequestLine requestLine = new HttpRequestLine();

    private HttpResponse response;

    protected String method = null;

    protected String queryString = null;

    /**
     * The string manager for this package.处理错误消息
     */
    protected StringManager sm = StringManager.getManager("ex03.pyrmont.connector.http");

    public void process(Socket socket)
    {
        SocketInputStream input = null;
        OutputStream output = null;
        try
        {
            // 和第二章HttpServer区别是,此处使用SocketInputStream,方便获取更多内容
            input = new SocketInputStream(socket.getInputStream(), 2048);
            output = socket.getOutputStream();

            // create HttpRequest object and parse
            request = new HttpRequest(input);

            // create HttpResponse object
            response = new HttpResponse(output);
            response.setRequest(request);
            
            // 向客户端发送响应头
            response.setHeader("Server", "Pyrmont Servlet Container");
            
            // 调用 本类中的两个私有方法来解析请求.本类创建HttpRequest实例,并解析请求行 和 请求头 来填充request的成员变量
            // !!!但是 本类不会解析 请求体 或 查询字符串(在请求行中的请求方法中) 中的参数.由每个requst自行完成(只有在某个servlet真正用到这些参数时,request才会延迟解析).
            parseRequest(input, output);
            parseHeaders(input);

            //check if this is a request for a servlet or a static resource
            //a request for a servlet begins with "/servlet/"
            if (request.getRequestURI().startsWith("/servlet/"))
            {
                ServletProcessor processor = new ServletProcessor();
                processor.process(request, response);
            }
            else
            {
                StaticResourceProcessor processor = new StaticResourceProcessor();
                processor.process(request, response);
            }

            // Close the socket
            socket.close();
            // no shutdown for this application
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 解析请求头
     * This method is the simplified version of the similar method in
     * org.apache.catalina.connector.http.HttpProcessor.
     * However, this method only parses some "easy" headers, such as
     * "cookie", "content-length", and "content-type", and ignore other headers.
     * @param input The input stream connected to our socket
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a parsing error occurs
     */
    private void parseHeaders(SocketInputStream input) throws IOException, ServletException
    {
        while (true)
        {
            HttpHeader header = new HttpHeader();

            // Read the next header
            // 解析请求头,该方法会填充HttpHeader对象
            input.readHeader(header);
            // 若没有请求头信息可以读取,HttpHeader实例的nameEnd和valueEnd都为0
            // 若为0,则表示已经读取了所有的请求头信息
            if (header.nameEnd == 0)
            {
                if (header.valueEnd == 0)
                {
                    return;
                }
                else
                {
                    throw new ServletException(sm.getString("httpProcessor.parseHeaders.colon"));
                }
            }
            // 获取请求头的 name 和 value
            String name = new String(header.name, 0, header.nameEnd);
            String value = new String(header.value, 0, header.valueEnd);
            request.addHeader(name, value);
            // do something for some headers, ignore others.
            // 处理cookie
            // Cookie请求头示例   Cookie: userName=zc; psw=11;
            if (name.equals("cookie"))
            {
                // 解析cookie
                Cookie cookies[] = RequestUtil.parseCookieHeader(value);
                for (int i = 0; i < cookies.length; i++)
                {
                    if (cookies[i].getName().equals("jsessionid"))
                    {
                        // Override anything requested in the URL
                        if (!request.isRequestedSessionIdFromCookie())
                        {
                            // Accept only the first session id cookie
                            request.setRequestedSessionId(cookies[i].getValue());
                            request.setRequestedSessionCookie(true);
                            request.setRequestedSessionURL(false);
                        }
                    }
                    request.addCookie(cookies[i]);
                }
            }
            // 处理content-length
            else if (name.equals("content-length"))
            {
                int n = -1;
                try
                {
                    n = Integer.parseInt(value);
                }
                catch (Exception e)
                {
                    throw new ServletException(sm.getString("httpProcessor.parseHeaders.contentLength"));
                }
                request.setContentLength(n);
            }
            // 处理content-type
            else if (name.equals("content-type"))
            {
                request.setContentType(value);
            }
        } //end while
    }

    /**
     * 解析 请求行,并将解析的内容赋值给httpRequest对象
     * @param input
     * @param output
     * @throws IOException
     * @throws ServletException
     */
    private void parseRequest(SocketInputStream input, OutputStream output) throws IOException, ServletException
    {

        // Parse the incoming request line, 调用SocketInputStream的readRequestLine方法
        // 此时传入的requstLine参数内容还没有,将会在这个方法中填充
        input.readRequestLine(requestLine);
        // 从 requestLine对象中获取 请求方法
        String method = new String(requestLine.method, 0, requestLine.methodEnd);//new String(byte[] bytes, int offset, int length)
        // 从 requestLine对象中获取 uri
        String uri = null;
        String protocol = new String(requestLine.protocol, 0, requestLine.protocolEnd);//获取请求的协议HTTP/1.1

        // Validate the incoming request line
        if (method.length() < 1)//即method的length == 0,抛异常
        {
            throw new ServletException("Missing HTTP request method");
        }
        else if (requestLine.uriEnd < 1)//请求的uri长度校验
        {
            throw new ServletException("Missing HTTP request URI");
        }
        
        // Parse any query parameters out of the request URI
        // 从requestLine中uri中获取查询参数,/servlet/Modernservlet?userName=zz&password=pwdval
        int question = requestLine.indexOf("?");//获取?号在uri中的位置    
        if (question >= 0)
        {//将queryString赋值给request对象
            request.setQueryString(new String(requestLine.uri, question + 1, requestLine.uriEnd - question - 1));//userName=zz&password=pwdval
            uri = new String(requestLine.uri, 0, question);//uri为/servlet/ModernServlet,不包含queryString
        }
        else
        {
            request.setQueryString(null);
            uri = new String(requestLine.uri, 0, requestLine.uriEnd);
        }
        
        /*
         * 请求行可能出现的两种方式
         * GET /myAPP/ModernServlet?userName=zz&psw=11 HTTP/1.1
         * 或
         * GET http://www.myAPP.com/ModernServlet?userName=zz&psw=11 HTTP/1.1
         * 下面对 其中两种可能的uri进行判断
         */
        // Checking for an absolute URI (with the HTTP protocol)
        if (!uri.startsWith("/"))
        {
            int pos = uri.indexOf("://");
            // Parsing out protocol and host name
            if (pos != -1)
            {
                pos = uri.indexOf('/', pos + 3);
                if (pos == -1)
                {
                    uri = "";
                }
                else
                {
                    uri = uri.substring(pos);
                }
            }
        }

        // Parse any requested session ID out of the request URI
        // 解析是否有jsessionid查询字符串(queryString)
        // 若有jsessionid,则表明会话标识符在queryString中,而不在cookie中
        String match = ";jsessionid=";
        int semicolon = uri.indexOf(match);
        if (semicolon >= 0)
        {
            String rest = uri.substring(semicolon + match.length());
            int semicolon2 = rest.indexOf(';');
            if (semicolon2 >= 0)
            {
                request.setRequestedSessionId(rest.substring(0, semicolon2));
                rest = rest.substring(semicolon2);
            }
            else
            {
                request.setRequestedSessionId(rest);
                rest = "";
            }
            request.setRequestedSessionURL(true);
            uri = uri.substring(0, semicolon) + rest;
        }
        else
        {
            request.setRequestedSessionId(null);
            request.setRequestedSessionURL(false);
        }

        // Normalize URI (using String operations at the moment)
        String normalizedUri = normalize(uri);

        // Set the corresponding request properties,给request对象赋值method,如GET
        ((HttpRequest) request).setMethod(method);
        request.setProtocol(protocol);//给request对象赋值protocol,如HTTP/1.1
        if (normalizedUri != null)
        {
            ((HttpRequest) request).setRequestURI(normalizedUri);//给request对象赋值uri,如/servlet/ModernServlet
        }
        else
        {
            ((HttpRequest) request).setRequestURI(uri);
        }

        if (normalizedUri == null)
        {// 若无法修正抛异常
            throw new ServletException("Invalid URI: " + uri + "'");
        }
    }

    /**
     * 对非正常的uri进行修正,若无法修正,返回null
     * Return a context-relative path, beginning with a "/", that represents
     * the canonical version of the specified path after ".." and "." elements
     * are resolved out.  If the specified path attempts to go outside the
     * boundaries of the current context (i.e. too many ".." path elements
     * are present), return <code>null</code> instead.
     * 
     * @param path Path to be normalized
     */
    protected String normalize(String path)
    {
        if (path == null)
            return null;
        // Create a place for the normalized path
        String normalized = path;

        // Normalize "/%7E" and "/%7e" at the beginning to "/~"
        if (normalized.startsWith("/%7E") || normalized.startsWith("/%7e"))
            normalized = "/~" + normalized.substring(4);

        // Prevent encoding '%', '/', '.' and '\', which are special reserved
        // characters
        if ((normalized.indexOf("%25") >= 0) || (normalized.indexOf("%2F") >= 0) || (normalized.indexOf("%2E") >= 0)
                || (normalized.indexOf("%5C") >= 0) || (normalized.indexOf("%2f") >= 0)
                || (normalized.indexOf("%2e") >= 0) || (normalized.indexOf("%5c") >= 0))
        {
            return null;
        }

        if (normalized.equals("/."))
            return "/";

        // Normalize the slashes and add leading slash if necessary
        if (normalized.indexOf('\\') >= 0)
            normalized = normalized.replace('\\', '/');
        if (!normalized.startsWith("/"))
            normalized = "/" + normalized;

        // Resolve occurrences of "//" in the normalized path
        while (true)
        {
            int index = normalized.indexOf("//");
            if (index < 0)
                break;
            normalized = normalized.substring(0, index) + normalized.substring(index + 1);
        }

        // Resolve occurrences of "/./" in the normalized path
        while (true)
        {
            int index = normalized.indexOf("/./");
            if (index < 0)
                break;
            normalized = normalized.substring(0, index) + normalized.substring(index + 2);
        }

        // Resolve occurrences of "/../" in the normalized path
        while (true)
        {
            int index = normalized.indexOf("/../");
            if (index < 0)
                break;
            if (index == 0)
                return (null); // Trying to go outside our context
            int index2 = normalized.lastIndexOf('/', index - 1);
            normalized = normalized.substring(0, index2) + normalized.substring(index + 3);
        }

        // Declare occurrences of "/..." (three or more dots) to be invalid
        // (on some Windows platforms this walks the directory tree!!!)
        if (normalized.indexOf("/...") >= 0)
            return (null);

        // Return the normalized path that we have completed
        return (normalized);

    }

}
