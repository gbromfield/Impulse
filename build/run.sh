#!/bin/sh
# Don't forget to run dos2unix on this file
JMXARG1="-Dcom.sun.management.jmxremote"
JMXARG2="-Dcom.sun.management.jmxremote.port=12333"
JMXARG3="-Dcom.sun.management.jmxremote.ssl=false"
JMXARG4="-Dcom.sun.management.jmxremote.authenticate=false"
# To activate JMX add 
# "$JMXARG1" "$JMXARG2" "$JMXARG3" "$JMXARG4" 
# to the command line
# Optional JMX argument if port is being listened to but still having trouble connecting
OPTJMXARG1="-Djava.rmi.server.hostname=xxx"
# Optional Log4J debugging
OPTLOG4JARGS="-Dlog4j.debug"
java -cp "*" -Dlog4j.configuration="file:log4j.properties" "$JMXARG1" "$JMXARG2" "$JMXARG3" "$JMXARG4" com.grb.impulse.Impulse "$@"
