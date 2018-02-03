package ex02.pyrmont;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.io.File;
import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * @author flyingzc
 * 处理 对Servlet资源的请求
 */
public class ServletProcessor1
{

    public void process(Request request, Response response)
    {

        String uri = request.getUri();//获取请求的uri,类似于/servlet/servletName
        String servletName = uri.substring(uri.lastIndexOf("/") + 1);//截取类名
        URLClassLoader loader = null;//URLClassLoader类是java.lang.ClassLoader类的直接子类
        //以下,通过URLClassLoader 载入该servlet类
        try
        {
            // create a URLClassLoader
            URL[] urls = new URL[1];
            URLStreamHandler streamHandler = null;
            File classPath = new File(Constants.WEB_ROOT);//D:\workspace-e3\HowTomcatWork\webroot
            // the forming of repository is taken from the createClassLoader method in
            // org.apache.catalina.startup.ClassLoaderFactory
            //java.net.URL构造函数接收三个参数(,仓库路径即查找servlet类的目录,)
            String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString();
            //生成仓库后 会调用org.apache.catalina.startup.ClassLoaderFactory类的createClassLoader()方法,
            //生成URL对象后会调用org.apache.catalina.loader.StandardClassLoader类的addRepository()方法
            // the code for forming the URL is taken from the addRepository method in
            // org.apache.catalina.loader.StandardClassLoader class.
            urls[0] = new URL(null, repository, streamHandler);
            loader = new URLClassLoader(urls);
        }
        catch (IOException e)
        {
            System.out.println(e.toString());
        }
        Class myClass = null;
        try
        {
            myClass = loader.loadClass(servletName);//载入servlet类
        }
        catch (ClassNotFoundException e)
        {
            System.out.println(e.toString());
        }

        Servlet servlet = null;

        try
        {
            servlet = (Servlet) myClass.newInstance();//创建Servlet类
            servlet.service((ServletRequest) request, (ServletResponse) response);//调用service方法
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
        }
        catch (Throwable e)
        {
            System.out.println(e.toString());
        }

    }
}
