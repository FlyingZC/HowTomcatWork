package ex03.pyrmont.connector.http;

import java.io.File;

public final class Constants
{
    public static final String WEB_ROOT = System.getProperty("user.dir") + File.separator + "webcontent";
    
    public static final int PORT = 9080;

    public static final String Package = "ex03.pyrmont.connector.http";

    public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;

    public static final int PROCESSOR_IDLE = 0;

    public static final int PROCESSOR_ACTIVE = 1;
}
