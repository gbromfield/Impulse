@echo off
set JMXARGS=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=12333 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false
REM Optional JMX argument if port is being listened to but still having trouble connecting
set OPTJMXARGS=-Djava.rmi.server.hostname=xxx
REM Optional Log4J debugging
set OPTLOG4JARGS = "-Dlog4j.debug"
java -cp "*" %JMXARGS% -Dlog4j.configuration="file:log4j.properties" com.grb.impulse.Impulse %1 %2 %3 %4 %5
