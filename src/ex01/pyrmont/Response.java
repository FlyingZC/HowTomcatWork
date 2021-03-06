package ex01.pyrmont;

import java.io.OutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;

/*
  HTTP Response = Status-Line
    *(( general-header | response-header | entity-header ) CRLF)
    CRLF
    [ message-body ]
    Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
*/

/**
 * @author flyingzc
 * HTTP响应
 */
public class Response
{

    private static final int BUFFER_SIZE = 1024;

    Request request;

    OutputStream output;

    /**
     * 构造函数 接收 一个OutputStream对象
     * @param output
     */
    public Response(OutputStream output)
    {
        this.output = output;
    }

    /**
     * 接收一个Request对象为参数
     * @param request
     */
    public void setRequest(Request request)
    {
        this.request = request;
    }

    /**
     * 发送一个静态资源到浏览器,如HTML文件
     * @throws IOException
     */
    public void sendStaticResource() throws IOException
    {
        byte[] bytes = new byte[BUFFER_SIZE];
        FileInputStream fis = null;
        try
        {//D:\workspace-e3\HowTomcatWork\WebContent\index.html
            File file = new File(HttpServer.WEB_ROOT, request.getUri());
            if (file.exists())
            {   //读取webroot下的index页面
                fis = new FileInputStream(file);
                int ch = fis.read(bytes, 0, BUFFER_SIZE);
                while (ch != -1)
                {
                    output.write(bytes, 0, ch);//将读取的静态页面内容,输出到输出流
                    ch = fis.read(bytes, 0, BUFFER_SIZE);
                }
            }
            else
            {
                // file not found
                String errorMessage = "HTTP/1.1 404 File Not Found\r\n" + "Content-Type: text/html\r\n"
                        + "Content-Length: 23\r\n" + "\r\n" + "<h1>File Not Found,这是客户端返回的数据</h1>";
                output.write(errorMessage.getBytes());
            }
        }
        catch (Exception e)
        {
            // thrown if cannot instantiate a File object
            System.out.println(e.toString());
        }
        finally
        {
            if (fis != null)
                fis.close();
        }
    }
}
