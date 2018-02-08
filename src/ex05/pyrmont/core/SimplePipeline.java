package ex05.pyrmont.core;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;

/**
 * 管道 的 简单实现
 * */
public class SimplePipeline implements Pipeline
{
    
    public SimplePipeline(Container container)
    {// 管道外面的容器
        setContainer(container);
    }

    // The basic Valve (if any) associated with this Pipeline.
    protected Valve basic = null;

    // The Container with which this Pipeline is associated.
    protected Container container = null;

    // the array of Valves 阀
    protected Valve valves[] = new Valve[0];

    public void setContainer(Container container)
    {
        this.container = container;
    }

    public Valve getBasic()
    {
        return basic;
    }

    /**设置 基础阀*/
    public void setBasic(Valve valve)
    {
        this.basic = valve;
        ((Contained) valve).setContainer(container);
    }

    /**向管道中添加阀,存储在values数组中*/
    public void addValve(Valve valve)
    {
        if (valve instanceof Contained)
            ((Contained) valve).setContainer(this.container);

        synchronized (valves)
        {// 每次 values阀值数组 都扩容加1
            Valve results[] = new Valve[valves.length + 1];
            System.arraycopy(valves, 0, results, 0, valves.length);
            results[valves.length] = valve;
            valves = results;
        }
    }

    public Valve[] getValves()
    {
        return valves;
    }
    
    /**
     * 转调 内部类SimplePipelineValueContext中的invokeNext方法
     * */
    public void invoke(Request request, Response response) throws IOException, ServletException
    {
        // Invoke the first Valve in this pipeline for this request.通过valueContext来调用管道(pipeline)中的阀(value)
        (new SimplePipelineValveContext()).invokeNext(request, response);
    }

    public void removeValve(Valve valve)
    {
    }

    // this class is copied from org.apache.catalina.core.StandardPipeline class's
    // StandardPipelineValveContext inner class. 内部类,可获取 外部类pipeline(管道)的成员,用于访问管道中的阀(values)
    protected class SimplePipelineValveContext implements ValveContext
    {

        protected int stage = 0;

        public String getInfo()
        {
            return null;
        }

        public void invokeNext(Request request, Response response) throws IOException, ServletException
        {
            int subscript = stage;//阀下标索引,从0开始
            stage = stage + 1;//阀 个数索引,从1开始
            // Invoke the requested Valve for the current request thread
            if (subscript < valves.length)
            {// !!!调用阀.invoke方法,传入当前的this(即SimplePipeline对象,用于贯穿调用整个管道中的阀数组)
                valves[subscript].invoke(request, response, this);
            }
            else if ((subscript == valves.length) && (basic != null))// 最后调用 基础阀(位于管道中阀的结尾)
            {
                basic.invoke(request, response, this);
            }
            else
            {
                throw new ServletException("No valve");
            }
        }
    } // end of inner class

}
