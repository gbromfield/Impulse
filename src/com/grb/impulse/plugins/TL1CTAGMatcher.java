package com.grb.impulse.plugins;

import com.grb.impulse.*;
import com.grb.impulse.parsers.tl1.*;
import com.grb.util.property.*;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.SystemPropertySource;

import java.util.HashSet;
import java.util.Map;

/**
 * Created by gbromfie on 11/23/15.
 */
public class TL1CTAGMatcher extends BaseTransform {

    /**
     * Delay for the TL1 response. After the delay the data is sent
     * to the it's outputs.
     * Can be specified as a single value; or as a range in the form of
     * min-max. If a range is given a random value in that range will
     * be chosen.
     */
    public static final String CTAG_REGEX_PROPERTY = "ctagRegex";

    /**
     * Default delay for the TL1 response.
     * Can be specified as a single value; or as a range in the form of
     * min-max. If a range is given a random value in that range will
     * be chosen.
     * Specifiable in the configuration file or command line.
     */
    public static final String CTAG_REGEX_PROPERTY_DEFAULT = "";

    protected String _ctag;
    protected HashSet<String> _ctagSet;

    public TL1CTAGMatcher(String transformName, String instanceName,
                         TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
    }

    @Override
    public void init() throws Exception {
        super.init();
        _ctag = getStringProperty(CTAG_REGEX_PROPERTY);
        _ctagSet = null;
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
        if (message instanceof TL1ResponseMessage) {
            String respCtag = ((TL1ResponseMessage)message).getCTAG();
            if (respCtag != null) {
                if (_ctag != null) {
                    if (respCtag.matches(_ctag)) {
                        onCtagMatch(argMap);
                        return;
                    }
                }
                if (_ctagSet != null) {
                    if (_ctagSet.contains(respCtag)) {
                        _ctagSet.remove(respCtag);
                        onCtagMatch(argMap);
                        return;
                    }
                }
            }
        }
        onNoCtagMatch(argMap);
    }

    /**
     * [output] In progresses and responses.
     *
     * @param argMap
     */
    @Output("onCtagMatch")
    public void onCtagMatch(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onCtagMatch: \r\n" + Impulse.format(argMap));
        }
        next("onCtagMatch", argMap);
    }

    /**
     * [output] In progresses and responses.
     *
     * @param argMap
     */
    @Output("onNoCtagMatch")
    public void onNoCtagMatch(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onNoCtagMatch: \r\n" + Impulse.format(argMap));
        }
        next("onNoCtagMatch", argMap);
    }

    /**
     * [input] Setting of the ctag.
     *
     * @param argMap Data to be delayed.
     */
    @Input("onSetCtag")
    public void onSetCtag(Map<String, Object> argMap, TL1Message message) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onSetCtag: \r\n" + Impulse.format(argMap));
        }
        if (message instanceof TL1InputMessage) {
            if (_ctagSet == null) {
                String inputCtag = ((TL1InputMessage)message).getCTAG();
                if (inputCtag != null) {
                    _ctagSet.add(inputCtag);
                }
            }
        }
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

        Property<String> p = new Property<String>(CTAG_REGEX_PROPERTY, CTAG_REGEX_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(CTAG_REGEX_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(CTAG_REGEX_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "CTAG to match");
        props.put(p.getId(), p);
    }
}
