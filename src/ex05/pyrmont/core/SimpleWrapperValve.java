package ex05.pyrmont.core;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;
import org.apache.catalina.Contained;
import org.apache.catalina.Container;

/**
 * @author flyingzc
 * 一个基础阀,注意与SimpleWrapper的区别.专门用于处理 对 simpleWrapper类的请求
 */
public class SimpleWrapperValve implements Valve, Contained
{

    protected Container container;
    // 基础阀 无需再使用invokeNext调用其他阀.只需要调用simpleWrapper.allocate()获取servlet实例,并调用其service方法
    public void invoke(Request request, Response response, ValveContext valveContext)
            throws IOException, ServletException
    {

        SimpleWrapper wrapper = (SimpleWrapper) getContainer();// 获取容器,此处为SimpleWrapper
        ServletRequest sreq = request.getRequest();
        ServletResponse sres = response.getResponse();
        Servlet servlet = null;
        HttpServletRequest hreq = null;
        if (sreq instanceof HttpServletRequest)
            hreq = (HttpServletRequest) sreq;
        HttpServletResponse hres = null;
        if (sres instanceof HttpServletResponse)
            hres = (HttpServletResponse) sres;

        // Allocate a servlet instance to process this request,分配一个servlet实例处理请求
        try
        {
            servlet = wrapper.allocate();
            if (hres != null && hreq != null)
            {// !!!调用servlet的service方法
                servlet.service(hreq, hres);
            }
            else
            {
                servlet.service(sreq, sres);
            }
        }
        catch (ServletException e)
        {
        }
    }

    public String getInfo()
    {
        return null;
    }

    public Container getContainer()
    {
        return container;
    }

    public void setContainer(Container container)
    {
        this.container = container;
    }
}
