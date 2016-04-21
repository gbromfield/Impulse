package com.grb.impulse.plugins;

import com.grb.impulse.*;
import com.grb.util.property.*;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.SystemPropertySource;

import java.util.Map;

/**
 * Created by gbromfie on 11/23/15.
 */
public class CLIResponseMatcher extends BaseTransform {

    private boolean _match;

    public CLIResponseMatcher(String transformName, String instanceName,
                              TransformCreationContext transformCreationContext, Object... args) {
        super(transformName, instanceName, transformCreationContext);
        _match = false;
    }

    /**
     * [input] Incoming TL1 Response.
     *
     * @param argMap Data to be delayed.
     */
    @Input("onCLIIn")
    public void onCLIIn(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onCLIIn: \r\n" + Impulse.format(argMap));
        }
        if (_match) {
            onMatch(argMap);
        } else {
            onNoMatch(argMap);
        }
    }

    /**
     * [output] In progresses and responses.
     *
     * @param argMap
     */
    @Output("onRespMatch")
    public void onMatch(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onRespMatch: \r\n" + Impulse.format(argMap));
        }
        next("onRespMatch", argMap);
    }

    /**
     * [output] In progresses and responses.
     *
     * @param argMap
     */
    @Output("onNoRespMatch")
    public void onNoMatch(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onNoRespMatch: \r\n" + Impulse.format(argMap));
        }
        next("onNoRespMatch", argMap);
    }

    /**
     * [input] Setting of the ctag.
     *
     * @param argMap Data to be delayed.
     */
    @Input("onSetMatch")
    public void onSetMatch(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onSetMatch: \r\n" + Impulse.format(argMap));
        }
        _match = true;
    }

    /**
     * [input] Setting of the ctag.
     *
     * @param argMap Data to be delayed.
     */
    @Input("onSetNoMatch")
    public void onSetNoMatch(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("onSetNoMatch: \r\n" + Impulse.format(argMap));
        }
        _match = false;
    }
}
