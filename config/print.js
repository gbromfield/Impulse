/**
 * Prints the Connection Context.
 *
 * @param connectionContext ConnectionContext to print.
 * @returns {*}
 */
function printConnectionContext(connectionContext) {
    connectionContext.log.info('connectionContext=' + connectionContext);
    /**
     * return the input connection context without modification
     * if you want to have a script act as defined in the configuration
     * file.
     * To change the behavior return an appropriate connection context.
     */
    return connectionContext;
}