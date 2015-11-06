function printArgs(outputPortName, argMap, args, connectionNames) {
    print('outputPortName=' + outputPortName);
    for (i = 0; i < args.length; i++) {
        print('args=' + args[i]);
    }
    for (i = 0; i < connectionNames.length; i++) {
        print('connection=' + connectionNames[i]);
    }
    return connectionNames;
}