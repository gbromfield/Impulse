/**
 * Prints the Connection Context.
 *
 * @param connectionContext ConnectionContext to print.
 * @returns {*}
 */
function printConnectionContext(logger, connection, argMap, args) {
    logger.info('connection=' + connection);
    logger.info('argMap=' + argMap);
    if (args != null) {
        var i = 0;
        while(i < args.length) {
            logger.info('args[' + i + "]= " + args[i]);
            i++;
        }
    }
    /**
     * Returning false cancels the following of this connection.
     */
    return true;
}

function sendTL1Error(logger, connection, argMap, args) {
    printConnectionContext(logger, connection, argMap, args);
    var String = Java.type("java.lang.String");
    tl1Message = argMap.get("TL1ManagerFramer.TL1Message.tl1Message");
    var JDate = Java.type("java.util.Date");
    var now = new JDate();
    var SimpleDateFormat = Java.type("java.text.SimpleDateFormat");
    var dateFmt = new SimpleDateFormat("yy-MM-dd");
    var timeFmt = new SimpleDateFormat("HH:mm:ss");
    var tid = "NE";
    if (tl1Message.getTid()) {
        tid = tl1Message.getTid();
    }
    var response = new String("\r\n\n   \"" + tid + "\" " + dateFmt.format(now) +
        " " + timeFmt.format(now) + "\r\nM  " + tl1Message.getCTAG() +
        " DENY\r\n   SARB\r\n   /* Out To Lunch */\r\n;");
    var ByteBuffer = Java.type("java.nio.ByteBuffer");
    var buffer = ByteBuffer.allocate(100);
    buffer.put(response.getBytes());
    buffer.flip();
    argMap.put("TL1ManagerFramer.ByteBuffer.bytesTL1Message", buffer);
}