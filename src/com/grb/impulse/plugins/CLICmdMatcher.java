package com.grb.impulse.plugins;

import com.grb.impulse.*;
import com.grb.impulse.parsers.cli.CLIMessage;
import com.grb.util.property.*;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.SystemPropertySource;

import java.util.Map;

/**
 * Created by gbromfie on 11/23/15.
 */
public class CLICmdMatcher extends BaseTransform {

    /**
     * Regex cmd1 string to match
     */
    public static final String CMD1_PROPERTY = "cmd1";

    /**
     * Regex cmd2 to match
     */
    public static final String CMD2_PROPERTY = "cmd2";

    /**
     * Regex cmd3 to match
     */
    public static final String CMD3_PROPERTY = "cmd3";

    /**
     * Regex cmd4 string to match
     */
    public static final String CMD4_PROPERTY = "cmd4";

    /**
     * Regex cmd5 string to match
     */
    public static final String CMD5_PROPERTY = "cmd5";

    /**
     * Default regex cmd1
     * Specifiable in the configuration file or command line.
     */
    public static final String CMD1_PROPERTY_DEFAULT = "";

    /**
     * Default regex cmd2
     * Specifiable in the configuration file or command line.
     */
    public static final String CMD2_PROPERTY_DEFAULT = "";

    /**
     * Default regex cmd3
     * Specifiable in the configuration file or command line.
     */
    public static final String CMD3_PROPERTY_DEFAULT = "";

    /**
     * Default regex cmd4
     * Specifiable in the configuration file or command line.
     */
    public static final String CMD4_PROPERTY_DEFAULT = "";

    /**
     * Default regex cmd5
     * Specifiable in the configuration file or command line.
     */
    public static final String CMD5_PROPERTY_DEFAULT = "";

    protected String _cmd1;
    protected String _cmd2;
    protected String _cmd3;
    protected String _cmd4;
    protected String _cmd5;

    public CLICmdMatcher(String transformName, String instanceName,
                         TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
    }

    @Override
    public void init() throws Exception {
        super.init();
        _cmd1 = getStringProperty(CMD1_PROPERTY);
        _cmd2 = getStringProperty(CMD2_PROPERTY);
        _cmd3 = getStringProperty(CMD3_PROPERTY);
        _cmd4 = getStringProperty(CMD4_PROPERTY);
        _cmd5 = getStringProperty(CMD5_PROPERTY);
    }

    /**
     * [input] Incoming TL1 Response.
     *
     * @param argMap Data to be delayed.
     */
    @Input("onCLIIn")
    public void onCLIIn(Map<String, Object> argMap, CLIMessage message) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onCLIIn: \r\n" + Impulse.format(argMap));
        }
        String cmd1 = null;
        String cmd2 = null;
        String cmd3 = null;
        String cmd4 = null;
        String cmd5 = null;
        String cmdStr = message.getDataStr();
        String[] cmds = cmdStr.split(" ");
        if (cmds.length > 0) {
            cmd1 = cmds[0];
        }
        if (cmds.length > 1) {
            cmd2 = cmds[1];
        }
        if (cmds.length > 2) {
            cmd3 = cmds[2];
        }
        if (cmds.length > 3) {
            cmd4 = cmds[3];
        }
        if (cmds.length > 4) {
            cmd5 = cmds[4];
        }
        if (match(cmd1, cmd2, cmd3, cmd4, cmd5)) {
            onCmdMatch(argMap);
        } else {
            onNoCmdMatch(argMap);
        }
    }

    private boolean match(String cmd1, String cmd2, String cmd3, String cmd4, String cmd5) {
        if ((_cmd1 != null) && (_cmd1.length() > 0) && (cmd1 != null) && (cmd1.length() > 0)) {
            if (!cmd1.matches(_cmd1)) {
                return false;
            }
        }
        if ((_cmd2 != null) && (_cmd2.length() > 0) && (cmd2 != null) && (cmd2.length() > 0)) {
            if (!cmd2.matches(_cmd2)) {
                return false;
            }
        }
        if ((_cmd3 != null) && (_cmd3.length() > 0) && (cmd3 != null) && (cmd3.length() > 0)) {
            if (!cmd3.matches(_cmd3)) {
                return false;
            }
        }
        if ((_cmd4 != null) && (_cmd4.length() > 0) && (cmd4 != null) && (cmd4.length() > 0)) {
            if (!cmd4.matches(_cmd4)) {
                return false;
            }
        }
        if ((_cmd5 != null) && (_cmd5.length() > 0) && (cmd5 != null) && (cmd5.length() > 0)) {
            if (!cmd5.matches(_cmd5)) {
                return false;
            }
        }
        return true;
    }

    /**
     * [output] In progresses and responses.
     *
     * @param argMap
     */
    @Output("onCmdMatch")
    public void onCmdMatch(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onCmdMatch: \r\n" + Impulse.format(argMap));
        }
        next("onCmdMatch", argMap);
    }

    /**
     * [output] In progresses and responses.
     *
     * @param argMap
     */
    @Output("onNoCmdMatch")
    public void onNoCmdMatch(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onNoCmdMatch: \r\n" + Impulse.format(argMap));
        }
        next("onNoCmdMatch", argMap);
    }

    /**
     * This method is for declaring this transform's properties. This method is called
     * using reflection at application startup by the transform's context. These properties
     * will be initialized after this call to validate their values. The TransformContext
     * for this Transform owns the properties and then clones the properties
     * for the transform when the transform is created so that each Transform has its
     * own copy.
     *
     * @param ctx the transform context.
     * @throws PropertyVetoException
     * @throws PropertyConversionException
     */
    @SuppressWarnings("unchecked")
    public static void getProperties(TransformContext ctx) throws PropertyVetoException, PropertyConversionException {
        Map<String, Property<?>> props = ctx.getProperties();

        Property<String> p = new Property<String>(CMD1_PROPERTY, CMD1_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(CMD1_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CMD1_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Cmd1 to match");
        props.put(p.getId(), p);

        p = new Property<String>(CMD2_PROPERTY, CMD2_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(CMD2_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CMD2_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Cmd2 to match");
        props.put(p.getId(), p);

        p = new Property<String>(CMD3_PROPERTY, CMD3_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(CMD3_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CMD3_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Cmd3 to match");
        props.put(p.getId(), p);

        p = new Property<String>(CMD4_PROPERTY, CMD4_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(CMD4_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CMD4_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Cmd4 to match");
        props.put(p.getId(), p);

        p = new Property<String>(CMD5_PROPERTY, CMD5_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(CMD5_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CMD5_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Cmd5 to match");
        props.put(p.getId(), p);
    }
}
