package util;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
public class Olog {
    public static Logger log = (Logger)LoggerFactory.getLogger(Olog.class);
    public static Logger test = (Logger)LoggerFactory.getLogger("test");
    public static Logger data = (Logger) LoggerFactory.getLogger("data");
}
