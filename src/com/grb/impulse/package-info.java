/**
 * Impulse is a utility for testing network traffic interactions with clients.
 * Impulse at its simplest is a server that listens on a port(s), accepts
 * connections, reads data, and then acts on that data. The acts that can be 
 * performed are implemented through transform plugins. 
 * 
 * <h3>Transforms</h3>
 * Transforms take data read from the network, then does something with it, 
 * and then passes it on to the next transform. By creating simple transforms 
 * and having the ability to chain them together in different ways creates a 
 * simple language for manipulating data. Transforms are written in Java and the
 * description/properties/operations about the transforms can be viewed in the Javadoc.
 * A simple example would be an echo application where you would need 3 transforms:
 * <li>1. a SocketServer to listen on a port.
 * <li>2. a Server that handles read/writing to a client.
 * <li>3. an echoer that takes what's read from the Server and it writes it back.
 * 
 * <h3>Config File</h3>
 * The transform plugins to be used are described in a configuration file which
 * is passed as an argument to Impulse's main. The configuration file contains 
 * property values for the transforms as well as how they all chain together. 
 * Sample config files can be found in the config directory.
 * The basic syntax follows (order is not important):
 * <li>transform.[name].classname=[java classname] - for declaring a transform.
 * <li>transform.[name].[property1]=[someValue] - for setting property1 value to someValue on a transform.
 * What properties can be set are documented in the transform javadoc.
 * <li>link.out.[output transform].[output name]=[input transform].[input name] - for creating connections.
 * One output can go to many inputs. In this case the outputs need to be numbered like: 
 * link.out.[output transform].[output name].1=[input transform 1].[input name] and 
 * link.out.[output transform].[output name].2=[input transform 2].[input name]. 
 * The outputs are called in numerical order. The type of the output must match
 * the type of the input. Usually transforms take Objects or ByteBuffers as 
 * inputs/outputs. If a transform input takes no arguments, you can link it with an
 * output that outputs an argument, it is just stripped. 
 * 
 * <h3>Getting Started</h3>
 * For starting Impulse, there is a run.sh and run.bat startup files that use the
 * following syntax:<p>
 * Impulse [-Dkey=value ...] configFile 
 * <p>
 * Alternately you can specify the system property 
 * -DconfigFile=<config file> for specifying a config file. The run.sh and run.bat
 * startup files configure JMX to use port 12333, and to use the Log4J.properties file 
 * in the current directory. For documentation on the transforms look in the docs directory
 * for the transform javadocs under the package "com.grb.impulse.plugins". Inputs and
 * Outputs are labelled to see what are valid chains that can be made. The best resource
 * is always the sample config files. Logging can be configured in three ways:
 * <li> configuring the log4j.properties file.
 * <li> setting the log4jLogLevel property on a transform in a transform properties file.
 * <li> setting the log4jLogLevel property on a transform in JMX.
 * <p>Transform properties can sometimes be configured through the command line where
 * enabled. For example: -Dtransform.tunnel.serverIPPort=192.168.160.128:55555. Which 
 * properties are configurable through the command line is transform specific (in this 
 * example the Tunnel transform allows it for the serverIPPort property).
 * 
 * <h3>Transform Lifetime</h3>
 * Transforms can be auto started (statically created) or created on demand. An example 
 * of an auto started transform is a socket server. Typically you would want the socket 
 * server to be started and listening on a port at startup. This is accomplished by 
 * specifying autoStart=true for the socket server in the config file. An example of a 
 * created on demand transform is a tunnel where you don't want the tunnel created until 
 * a connection has been accepted by a socket server. You would connect the socket 
 * server's output onNewConnection to the tunnel's onNewConnection input. Since the 
 * tunnel was not auto started, the tunnel is created as well as all the transforms in 
 * its chain. This configuration will create a new tunnel for every new connection. 
 * In order to distinguish one tunnel from another in JMX, the transform name will be 
 * the same name as assigned in the config file  with a index number concatenated on the end.
 * There will also be an entry with the exact same name as in the config file but that 
 * is used as a template for the numbered entries. Modifying the template will have an 
 * affect on new transforms based on that template.
 */
package com.grb.impulse;