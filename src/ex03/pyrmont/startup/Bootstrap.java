package ex03.pyrmont.startup;

import ex03.pyrmont.connector.http.HttpConnector;

/**
 * @author flyingzc
 * 启动类
 */
public final class Bootstrap
{
    public static void main(String[] args)
    {
        // 创建 连接器 并 调用start方法,连接器 会 等待HTTP请求
        HttpConnector connector = new HttpConnector();
        connector.start();
    }
}
