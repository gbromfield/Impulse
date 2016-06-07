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
    tl1Message = argMap.get("TL1AgentFramer.TL1Message.tl1Message");
    var response = new String("\r\n\n   \"" + tl1Message.getTid() + "\" " + tl1Message.getDate() +
        " " + tl1Message.getTime() + "\r\nM  " + tl1Message.getCTAG() +
        " DENY\r\n   SARB\r\n   /* Out To Lunch */\r\n;");
    var ByteBuffer = Java.type("java.nio.ByteBuffer");
    var buffer = ByteBuffer.allocate(100);
    buffer.put(response.getBytes());
    buffer.flip();
    argMap.put("TL1AgentFramer.ByteBuffer.bytesTL1Message", buffer);
}