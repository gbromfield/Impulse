package com.grb.impulse.plugins;

import com.grb.impulse.*;
import com.grb.impulse.parsers.tl1.*;
import com.grb.util.property.*;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.SystemPropertySource;

import java.util.Map;

/**
 * Created by gbromfie on 11/23/15.
 */
public class TL1CmdCodeMatcher extends BaseTransform {

    /**
     * Regex verb string to match
     */
    public static final String VERB_PROPERTY = "verb";

    /**
     * Regex mod1 to match
     */
    public static final String MOD1_PROPERTY = "mod1";

    /**
     * Regex mod2 to match
     */
    public static final String MOD2_PROPERTY = "mod2";

    /**
     * Regex tid string to match
     */
    public static final String TID_PROPERTY = "tid";

    /**
     * Regex aid string to match
     */
    public static final String AID_PROPERTY = "aid";

    /**
     * Regex ctag string to match
     */
    public static final String CTAG_PROPERTY = "ctag";

    /**
     * Regex atag string to match
     */
    public static final String ATAG_PROPERTY = "atag";

    /**
     * Default regex verb
     * Specifiable in the configuration file or command line.
     */
    public static final String VERB_PROPERTY_DEFAULT = "";

    /**
     * Default regex mod1
     * Specifiable in the configuration file or command line.
     */
    public static final String MOD1_PROPERTY_DEFAULT = "";

    /**
     * Default regex mod2
     * Specifiable in the configuration file or command line.
     */
    public static final String MOD2_PROPERTY_DEFAULT = "";

    /**
     * Default regex tid
     * Specifiable in the configuration file or command line.
     */
    public static final String TID_PROPERTY_DEFAULT = "";

    /**
     * Default regex aid
     * Specifiable in the configuration file or command line.
     */
    public static final String AID_PROPERTY_DEFAULT = "";

    /**
     * Default regex ctag
     * Specifiable in the configuration file or command line.
     */
    public static final String CTAG_PROPERTY_DEFAULT = "";

    /**
     * Default regex atag
     * Specifiable in the configuration file or command line.
     */
    public static final String ATAG_PROPERTY_DEFAULT = "";

    protected String _verb;
    protected String _mod1;
    protected String _mod2;
    protected String _tid;
    protected String _aid;
    protected String _ctag;
    protected String _atag;

    public TL1CmdCodeMatcher(String transformName, String instanceName,
                             TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
    }

    @Override
    public void init() throws Exception {
        super.init();
        _verb = getStringProperty(VERB_PROPERTY);
        _mod1 = getStringProperty(MOD1_PROPERTY);
        _mod2 = getStringProperty(MOD2_PROPERTY);
        _tid = getStringProperty(TID_PROPERTY);
        _aid = getStringProperty(AID_PROPERTY);
        _ctag = getStringProperty(CTAG_PROPERTY);
        _atag = getStringProperty(ATAG_PROPERTY);
    }

    /**
     * [input] Incoming TL1 Response.
     *
     * @param argMap Data to be delayed.
     */
    @Input("onTL1In")
    public void onTL1In(Map<String, Object> argMap, TL1Message message) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onTL1In: \r\n" + Impulse.format(argMap));
        }
        String verb = null;
        String mod1 = null;
        String mod2 = null;
        String tid = null;
        String aid = null;
        String ctag = null;
        String atag = null;
        if (message instanceof TL1InputMessage) {
            verb = ((TL1InputMessage)message).getVerb();
            mod1 = ((TL1InputMessage)message).getMod1();
            mod2 = ((TL1InputMessage)message).getMod2();
            tid = ((TL1InputMessage)message).getTid();
            aid = ((TL1InputMessage)message).getAid();
            ctag = ((TL1InputMessage)message).getCTAG();
        } else if (message instanceof TL1AOMessage) {
            verb = ((TL1AOMessage) message).getVerb();
            mod1 = ((TL1AOMessage) message).getMod1();
            mod2 = ((TL1AOMessage) message).getMod2();
            tid = ((TL1AOMessage) message).getTid();
            atag = ((TL1AOMessage) message).getATAG();
        } else if (message instanceof TL1AckMessage) {
            verb = ((TL1AckMessage)message).getAckCode();
        }
        if (match(verb, mod1, mod2, tid, aid, ctag, atag)) {
            onCmdCodeMatch(argMap);
        } else {
            onNoCmdCodeMatch(argMap);
        }
    }

    private boolean match(String verb, String mod1, String mod2, String tid, String aid, String ctag, String atag) {
        if ((_verb != null) && (_verb.length() > 0) && (verb != null) && (verb.length() > 0)) {
            if (!verb.matches(_verb)) {
                return false;
            }
        }
        if ((_mod1 != null) && (_mod1.length() > 0) && (mod1 != null) && (mod1.length() > 0)) {
            if (!mod1.matches(_mod1)) {
                return false;
            }
        }
        if ((_mod2 != null) && (_mod2.length() > 0) && (mod2 != null) && (mod2.length() > 0)) {
            if (!mod2.matches(_mod2)) {
                return false;
            }
        }
        if ((_tid != null) && (_tid.length() > 0) && (tid != null) && (tid.length() > 0)) {
            if (!tid.matches(_tid)) {
                return false;
            }
        }
        if ((_aid != null) && (_aid.length() > 0) && (aid != null) && (aid.length() > 0)) {
            if (!aid.matches(_aid)) {
                return false;
            }
        }
        if ((_ctag != null) && (_ctag.length() > 0) && (ctag != null) && (ctag.length() > 0)) {
            if (!ctag.matches(_ctag)) {
                return false;
            }
        }
        if ((_atag != null) && (_atag.length() > 0) && (atag != null) && (atag.length() > 0)) {
            if (!atag.matches(_atag)) {
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
    public void onCmdCodeMatch(Map<String, Object> argMap) {
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
    public void onNoCmdCodeMatch(Map<String, Object> argMap) {
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

        Property<String> p = new Property<String>(VERB_PROPERTY, VERB_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(VERB_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(VERB_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Verb to match");
        props.put(p.getId(), p);

        p = new Property<String>(MOD1_PROPERTY, MOD1_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(MOD1_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(MOD1_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Mod1 to match");
        props.put(p.getId(), p);

        p = new Property<String>(MOD2_PROPERTY, MOD2_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(MOD2_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(MOD2_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Mod2 to match");
        props.put(p.getId(), p);

        p = new Property<String>(TID_PROPERTY, TID_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(TID_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(TID_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Tid to match");
        props.put(p.getId(), p);

        p = new Property<String>(AID_PROPERTY, AID_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(AID_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(AID_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Aid to match");
        props.put(p.getId(), p);

        p = new Property<String>(CTAG_PROPERTY, CTAG_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(CTAG_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CTAG_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Ctag to match");
        props.put(p.getId(), p);

        p = new Property<String>(ATAG_PROPERTY, ATAG_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(ATAG_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(ATAG_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "Atag to match");
        props.put(p.getId(), p);
    }
}
