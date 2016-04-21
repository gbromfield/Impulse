function replace(logger, connection, argMap, args) {
    var s = args[3];
    s = s.replace(/shelf-11/gi, "SHELF-GRAHAM");
    s = s.replace(/admin/gi, "GRAHAM");
    s = s.replace(/PV0028A/gi, "PV-GRAHAM");
    var ByteBuffer = Java.type("java.nio.ByteBuffer");
    var buffer = ByteBuffer.allocate(1000);
    buffer.put(s.getBytes());
    buffer.flip();
    args[1] = buffer;
    args[3] = s;
    return true;
}