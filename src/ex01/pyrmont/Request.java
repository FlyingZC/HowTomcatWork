package ex01.pyrmont;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

/**
 * @author flyingzc
 * HTTP请求的第一部分-请求行
 * 请求行以请求方法开始,接着是请求的URI和请求所使用的协议及其版本,并以CRLF符结束
 * 请求行中的元素以空格分开
 * 例如:
 * GET /index.html HTTP/1.1
 */
public class Request
{

    /**
     * 客户端请求的输入流
     */
    private InputStream input;

    /**
     * 客户端请求的uri
     */
    private String uri;

    public Request(InputStream input)
    {
        this.input = input;
    }

    /**
     * 解析HTTP请求报文中的原始数据,字节流到字符流
     */
    public void parse()
    {
        // Read a set of characters from the socket
        StringBuffer request = new StringBuffer(2048);
        int i;//从输入流中读取到的字节的长度
        byte[] buffer = new byte[2048];//保存从输入流中读取到的字节流
        try
        {
            //从客户端的输入流中读取,每次读取buffer数组大小
            i = input.read(buffer);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            i = -1;
        }
        for (int j = 0; j < i; j++)
        {
            try
            {
                new String(buffer,"ISO8859-1");
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
            request.append((char) buffer[j]);
        }
        System.out.print(request.toString());
        //将解析后的uri存入成员变量中,如得到/index.html
        uri = parseUri(request.toString());
    }

    /**
     * 解析HTTP请求的uri
     * 参见类注释的 uri请求行的格式,如得到/index.html
     * @param requestString
     * @return
     */
    private String parseUri(String requestString)
    {
        int index1, index2;
        //请求行 是以 空格分割的
        index1 = requestString.indexOf(' ');
        if (index1 != -1)
        {
            index2 = requestString.indexOf(' ', index1 + 1);
            if (index2 > index1)
                return requestString.substring(index1 + 1, index2);
        }
        return null;
    }

    public String getUri()
    {
        return uri;
    }

}
