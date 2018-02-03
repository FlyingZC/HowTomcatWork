package ex02.pyrmont;

import java.io.File;

/**
 * @author flyingzc
 * 定义常量,如webroot路径
 */
public class Constants
{
    public static final String WEB_ROOT = System.getProperty("user.dir") + File.separator + "webcontent";
    public static final int PORT = 9080;
}
