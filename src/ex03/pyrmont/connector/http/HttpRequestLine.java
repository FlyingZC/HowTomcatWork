package ex03.pyrmont.connector.http;

/**
 * HTTP request line enum type.
 *
 * @author Remy Maucherat
 * @version $Revision: 1.6 $ $Date: 2002/03/18 07:15:40 $
 */

final class HttpRequestLine
{

    // -------------------------------------------------------------- Constants

    public static final int INITIAL_METHOD_SIZE = 8;

    public static final int INITIAL_URI_SIZE = 64;

    public static final int INITIAL_PROTOCOL_SIZE = 8;

    public static final int MAX_METHOD_SIZE = 1024;

    public static final int MAX_URI_SIZE = 32768;

    public static final int MAX_PROTOCOL_SIZE = 1024;

    // ----------------------------------------------------------- Constructors

    public HttpRequestLine()
    {

        this(new char[INITIAL_METHOD_SIZE], 0, new char[INITIAL_URI_SIZE], 0, new char[INITIAL_PROTOCOL_SIZE], 0);

    }

    public HttpRequestLine(char[] method, int methodEnd, char[] uri, int uriEnd, char[] protocol, int protocolEnd)
    {

        this.method = method;
        this.methodEnd = methodEnd;
        this.uri = uri;
        this.uriEnd = uriEnd;
        this.protocol = protocol;
        this.protocolEnd = protocolEnd;

    }

    // ----------------------------------------------------- Instance Variables
    /**请求行 中 保存请求方法的字符数组GET或POST等*/
    public char[] method;
    /**请求行 中 请求方法的结束下标,默认初始化为0*/
    public int methodEnd;
    /**请求行 中 保存请求uri的字符数组/servlet/Modernservlet?userName=zz&password=pwdval*/
    public char[] uri;
    /**请求行 中 请求uri的结束下标,默认初始化为0*/
    public int uriEnd;
    /**请求行 中 保存请求协议的字符数组HTTP/1.1*/
    public char[] protocol;
    /**请求行 中 请求协议的结束下标,默认初始化为0*/
    public int protocolEnd;

    // ------------------------------------------------------------- Properties

    // --------------------------------------------------------- Public Methods

    /**
     * Release all object references, and initialize instance variables, in
     * preparation for reuse of this object.
     */
    public void recycle()
    {

        methodEnd = 0;
        uriEnd = 0;
        protocolEnd = 0;

    }

    /**
     * Test if the uri includes the given char array.
     */
    public int indexOf(char[] buf)
    {
        return indexOf(buf, buf.length);
    }

    /**
     * Test if the value of the header includes the given char array.
     */
    public int indexOf(char[] buf, int end)
    {
        char firstChar = buf[0];
        int pos = 0;
        while (pos < uriEnd)
        {
            pos = indexOf(firstChar, pos);//22
            if (pos == -1)
                return -1;
            if ((uriEnd - pos) < end)
                return -1;
            for (int i = 0; i < end; i++)
            {
                if (uri[i + pos] != buf[i])//字符 可以使用 != 或==判断
                    break;
                if (i == (end - 1))
                    return pos;
            }
            pos++;
        }
        return -1;
    }

    /**
     * Test if the value of the header includes the given string.
     */
    public int indexOf(String str)
    {
        return indexOf(str.toCharArray(), str.length());
    }

    /**
     * Returns the index of a character in the value.返回一个字符在uri字符数组中的位置,传入(查询的字符,开始下标)
     */
    public int indexOf(char c, int start)
    {
        for (int i = start; i < uriEnd; i++)
        {
            if (uri[i] == c)
                return i;
        }
        return -1;
    }

    // --------------------------------------------------------- Object Methods

    public int hashCode()
    {
        // FIXME
        return 0;
    }

    public boolean equals(Object obj)
    {
        return false;
    }

}
