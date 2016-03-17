function sendTL1Error(logger, connection, argMap, args) {
    printConnectionContext(logger, connection, argMap, args);
    var String = Java.type("java.lang.String");
    var response = new String("\r\n\n " + args[5].getTid() + " " + args[5].getDate() + " " + args[5].getTime() + "\r\nM  " + args[5].getCTAG() + " DENY;");
    var ByteBuffer = Java.type("java.nio.ByteBuffer");
    var buffer = ByteBuffer.allocate(100);
    buffer.put(response.getBytes());
    buffer.flip();
    args[1] = buffer;
}