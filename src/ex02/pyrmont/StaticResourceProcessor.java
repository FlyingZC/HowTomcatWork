package ex02.pyrmont;

import java.io.IOException;

/**
 * @author flyingzc
 * 处理 对静态资源的请求
 */
public class StaticResourceProcessor
{

    public void process(Request request, Response response)
    {
        try
        {
            response.sendStaticResource();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
