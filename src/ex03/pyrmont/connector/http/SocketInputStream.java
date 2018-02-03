package ex03.pyrmont.connector.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.EOFException;
import org.apache.catalina.util.StringManager;

/**
 * Extends InputStream to be more efficient reading lines during HTTP
 * header processing.
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * 该类 实际上是 org.apache.catalina.connector.http.SocketInputStrem类的一个副本
 * 提供一些方法 获取 请求行 和 请求头
 */
public class SocketInputStream extends InputStream
{

    // -------------------------------------------------------------- Constants

    /**
     * CR.
     */
    private static final byte CR = (byte) '\r';

    /**
     * LF.
     */
    private static final byte LF = (byte) '\n';

    /**
     * SP.
     */
    private static final byte SP = (byte) ' ';

    /**
     * HT.
     */
    private static final byte HT = (byte) '\t';

    /**
     * COLON.
     */
    private static final byte COLON = (byte) ':';

    /**
     * Lower case offset.
     */
    private static final int LC_OFFSET = 'A' - 'a';

    /**
     * Internal buffer.
     */
    protected byte buf[];

    /**
     * Last valid byte.buf字符数组中最后一个不为空的字符的下标,即输入流的总长度
     */
    protected int count;

    /**
     * Position in the buffer.buf字符数组的下标
     */
    protected int pos;

    /**
     * Underlying input stream.
     */
    protected InputStream is;

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a servlet input stream associated with the specified socket
     * input.
     *
     * @param is socket input stream
     * @param bufferSize size of the internal buffer
     */
    public SocketInputStream(InputStream is, int bufferSize)
    {

        this.is = is;
        buf = new byte[bufferSize];//初始化内部缓冲区buf数组,即byte[] buf = new byte[2048],2048的长度足够请求行所有数据的长度

    }

    // -------------------------------------------------------------- Variables

    /**
     * The string manager for this package.
     */
    protected static StringManager sm = StringManager.getManager(Constants.Package);

    // ----------------------------------------------------- Instance Variables

    // --------------------------------------------------------- Public Methods

    /**
     * Read the request line, and copies it to the given buffer. This
     * function is meant to be used during the HTTP request header parsing.
     * Do NOT attempt to read the request body using it.
     *
     * @param requestLine Request line object
     * @throws IOException If an exception occurs during the underlying socket
     * read operations, or if the given buffer is not big enough to accomodate
     * the whole line.
     * 解析请求行
     * GET /myAPP/ModernServlet?userName=zz&psw=11 HTTP/1.1
     * 其中第二部分是URL 加 可选的查询字符串
     */
    public void readRequestLine(HttpRequestLine requestLine) throws IOException
    {

        // Recycling check 结束下标若不是0,则置为0
        if (requestLine.methodEnd != 0)
            requestLine.recycle();

        // Checking for a blank line
        int chr = 0;
        do
        { // Skipping CR or LF
            try
            {
                chr = read();
            }
            catch (IOException e)
            {
                chr = -1;
            }
        }
        while ((chr == CR) || (chr == LF));
        if (chr == -1)
            throw new EOFException(sm.getString("requestStream.readline.error"));
        pos--;//pos置为0

        // Reading the method name
        // 下面开始解析 请求方法 
        int maxRead = requestLine.method.length;//requestLine中默认 请求方法的最大长度8,当读取超长时,会扩容
        int readStart = pos;//开始读取的位置
        int readCount = 0;//已经读了几位

        boolean space = false;

        while (!space)
        {
            // if the buffer is full, extend it 当读取的长度 大于等于 默认设置的长度时,需要对requestLine.method数组进行扩容
            if (readCount >= maxRead)
            {
                if ((2 * maxRead) <= HttpRequestLine.MAX_METHOD_SIZE)
                {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(requestLine.method, 0, newBuffer, 0, maxRead);
                    requestLine.method = newBuffer;
                    maxRead = requestLine.method.length;
                }
                else
                {
                    throw new IOException(sm.getString("requestStream.readline.toolong"));
                }
            }
            // We're at the end of the internal buffer读到内部缓存区数据的尾部
            if (pos >= count)
            {
                int val = read();
                if (val == -1)
                {
                    throw new IOException(sm.getString("requestStream.readline.error"));
                }
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == SP)// 当前字符为空格
            {
                space = true;
            }
            requestLine.method[readCount] = (char) buf[pos];// 将当前位置读到的字节 转为字符,存入到method数组中
            readCount++;
            pos++;
        }
        //methodEnd = 3,读取请求方法的结束下标
        requestLine.methodEnd = readCount - 1;

        // Reading URI
        //下面开始解析 请求的uri
        maxRead = requestLine.uri.length;//默认长度为64
        readStart = pos;//开始读取的下标
        readCount = 0;//解析请求方法时,已经读取的字符数

        space = false;

        boolean eol = false;//结束标志

        while (!space)
        {
            // if the buffer is full, extend it
            if (readCount >= maxRead)
            {
                if ((2 * maxRead) <= HttpRequestLine.MAX_URI_SIZE)
                {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(requestLine.uri, 0, newBuffer, 0, maxRead);
                    requestLine.uri = newBuffer;
                    maxRead = requestLine.uri.length;
                }
                else
                {
                    throw new IOException(sm.getString("requestStream.readline.toolong"));
                }
            }
            // We're at the end of the internal buffer
            if (pos >= count)
            {
                int val = read();
                if (val == -1)
                    throw new IOException(sm.getString("requestStream.readline.error"));
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == SP)
            {
                space = true;
            }
            else if ((buf[pos] == CR) || (buf[pos] == LF))// \r 或 \n
            {
                // HTTP/0.9 style request
                eol = true;
                space = true;
            }
            requestLine.uri[readCount] = (char) buf[pos];
            readCount++;
            pos++;
        }

        requestLine.uriEnd = readCount - 1;//uri的结束下标

        // Reading protocol
        //下面开始解析 请求的协议
        maxRead = requestLine.protocol.length;
        readStart = pos;
        readCount = 0;

        while (!eol)
        {
            // if the buffer is full, extend it
            if (readCount >= maxRead)
            {
                if ((2 * maxRead) <= HttpRequestLine.MAX_PROTOCOL_SIZE)
                {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(requestLine.protocol, 0, newBuffer, 0, maxRead);
                    requestLine.protocol = newBuffer;
                    maxRead = requestLine.protocol.length;
                }
                else
                {
                    throw new IOException(sm.getString("requestStream.readline.toolong"));
                }
            }
            // We're at the end of the internal buffer
            if (pos >= count)
            {
                // Copying part (or all) of the internal buffer to the line
                // buffer
                int val = read();
                if (val == -1)
                    throw new IOException(sm.getString("requestStream.readline.error"));
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == CR)
            {
                // Skip CR.
            }
            else if (buf[pos] == LF)
            {
                eol = true;
            }
            else
            {
                requestLine.protocol[readCount] = (char) buf[pos];
                readCount++;
            }
            pos++;
        }

        requestLine.protocolEnd = readCount;//请求协议 的 结束下标

    }

    /**解析请求头Key-value,每次调用只会解析一个key-value,所以要循环调用 直到解析完
     * Read a header, and copies it to the given buffer. This
     * function is meant to be used during the HTTP request header parsing.
     * Do NOT attempt to read the request body using it.
     *
     * @param requestLine Request line object
     * @throws IOException If an exception occurs during the underlying socket
     * read operations, or if the given buffer is not big enough to accomodate
     * the whole line.
     */
    public void readHeader(HttpHeader header) throws IOException
    {

        // Recycling check
        if (header.nameEnd != 0)
            header.recycle();

        // Checking for a blank line
        int chr = read();
        if ((chr == CR) || (chr == LF))
        { // Skipping CR
            if (chr == CR)
                read(); // Skipping LF
            header.nameEnd = 0;
            header.valueEnd = 0;
            return;
        }
        else
        {
            pos--;
        }

        // Reading the header name

        int maxRead = header.name.length;
        int readStart = pos;
        int readCount = 0;

        boolean colon = false;

        while (!colon)
        {
            // if the buffer is full, extend it
            if (readCount >= maxRead)
            {
                if ((2 * maxRead) <= HttpHeader.MAX_NAME_SIZE)
                {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(header.name, 0, newBuffer, 0, maxRead);
                    header.name = newBuffer;
                    maxRead = header.name.length;
                }
                else
                {
                    throw new IOException(sm.getString("requestStream.readline.toolong"));
                }
            }
            // We're at the end of the internal buffer
            if (pos >= count)
            {
                int val = read();
                if (val == -1)
                {
                    throw new IOException(sm.getString("requestStream.readline.error"));
                }
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == COLON)
            {
                colon = true;
            }
            char val = (char) buf[pos];
            if ((val >= 'A') && (val <= 'Z'))
            {
                val = (char) (val - LC_OFFSET);
            }
            header.name[readCount] = val;
            readCount++;
            pos++;
        }

        header.nameEnd = readCount - 1;

        // Reading the header value (which can be spanned over multiple lines)

        maxRead = header.value.length;
        readStart = pos;
        readCount = 0;

        int crPos = -2;

        boolean eol = false;
        boolean validLine = true;

        while (validLine)
        {

            boolean space = true;

            // Skipping spaces
            // Note : Only leading white spaces are removed. Trailing white
            // spaces are not.
            while (space)
            {
                // We're at the end of the internal buffer
                if (pos >= count)
                {
                    // Copying part (or all) of the internal buffer to the line
                    // buffer
                    int val = read();
                    if (val == -1)
                        throw new IOException(sm.getString("requestStream.readline.error"));
                    pos = 0;
                    readStart = 0;
                }
                if ((buf[pos] == SP) || (buf[pos] == HT))
                {
                    pos++;
                }
                else
                {
                    space = false;
                }
            }

            while (!eol)
            {
                // if the buffer is full, extend it
                if (readCount >= maxRead)
                {
                    if ((2 * maxRead) <= HttpHeader.MAX_VALUE_SIZE)
                    {
                        char[] newBuffer = new char[2 * maxRead];
                        System.arraycopy(header.value, 0, newBuffer, 0, maxRead);
                        header.value = newBuffer;
                        maxRead = header.value.length;
                    }
                    else
                    {
                        throw new IOException(sm.getString("requestStream.readline.toolong"));
                    }
                }
                // We're at the end of the internal buffer
                if (pos >= count)
                {
                    // Copying part (or all) of the internal buffer to the line
                    // buffer
                    int val = read();
                    if (val == -1)
                        throw new IOException(sm.getString("requestStream.readline.error"));
                    pos = 0;
                    readStart = 0;
                }
                if (buf[pos] == CR)
                {
                }
                else if (buf[pos] == LF)
                {
                    eol = true;
                }
                else
                {
                    // FIXME : Check if binary conversion is working fine
                    int ch = buf[pos] & 0xff;
                    header.value[readCount] = (char) ch;
                    readCount++;
                }
                pos++;
            }

            int nextChr = read();

            if ((nextChr != SP) && (nextChr != HT))
            {
                pos--;
                validLine = false;
            }
            else
            {
                eol = false;
                // if the buffer is full, extend it
                if (readCount >= maxRead)
                {
                    if ((2 * maxRead) <= HttpHeader.MAX_VALUE_SIZE)
                    {
                        char[] newBuffer = new char[2 * maxRead];
                        System.arraycopy(header.value, 0, newBuffer, 0, maxRead);
                        header.value = newBuffer;
                        maxRead = header.value.length;
                    }
                    else
                    {
                        throw new IOException(sm.getString("requestStream.readline.toolong"));
                    }
                }
                header.value[readCount] = ' ';
                readCount++;
            }

        }

        header.valueEnd = readCount;

    }

    /**
     * Read byte.读取输入流到内部缓冲区buf[]数组中,并返回请求行的第一个字符
     */
    public int read() throws IOException
    {
        if (pos >= count)
        {
            fill();
            if (pos >= count)
                return -1;
        }
        return buf[pos++] & 0xff;//相当于按位与 上 1111 1111,返回的还是前面buf[pos]的原值,第一位返回GET的G,即71
    }

    /**
     *
     */
    /*
    public int read(byte b[], int off, int len)
        throws IOException {
    
    }
    */

    /**
     *
     */
    /*
    public long skip(long n)
        throws IOException {
    
    }
    */

    /**
     * Returns the number of bytes that can be read from this input
     * stream without blocking.
     */
    public int available() throws IOException
    {
        return (count - pos) + is.available();
    }

    /**
     * Close the input stream.
     */
    public void close() throws IOException
    {
        if (is == null)
            return;
        is.close();
        is = null;
        buf = null;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Fill the internal buffer using data from the underlying input stream.使用来自底层输入流的数据填充内部缓冲区,初始化count和buf[]数组
     */
    protected void fill() throws IOException
    {
        pos = 0;
        count = 0;
        int nRead = is.read(buf, 0, buf.length);//将inputStream读入到byte[] buf数组中
        if (nRead > 0)
        {
            count = nRead;
        }
    }

}
