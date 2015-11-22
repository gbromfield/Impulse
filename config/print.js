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
