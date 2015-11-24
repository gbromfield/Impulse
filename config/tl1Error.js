function sendTL1Error(logger, connection, argMap, args) {
    printConnectionContext(logger, connection, argMap, args);
    var String = Java.type("java.lang.String");
    var response = new String("\r\n\n " + args[5].getTid() + " 85-10-09 22:05:12\r\nM  " + args[5].getCTAG() + " DENY;");
    var ByteBuffer = Java.type("java.nio.ByteBuffer");
    var buffer = ByteBuffer.allocate(100);
    buffer.put(response.getBytes());
    buffer.flip();
    args[1] = buffer;
}