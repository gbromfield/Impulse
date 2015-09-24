package com.grb.impulse.plugins;

import java.util.Iterator;
import java.util.Map;

import com.grb.impulse.BaseTransform;
import com.grb.impulse.Impulse;
import com.grb.impulse.Input;
import com.grb.impulse.Output;
import com.grb.impulse.TransformContext;
import com.grb.impulse.TransformCreationContext;
import com.grb.util.property.JMXUtils;
import com.grb.util.property.Property;
import com.grb.util.property.PropertyConversionException;
import com.grb.util.property.PropertySource;
import com.grb.util.property.PropertyVetoException;
import com.grb.util.property.impl.MapPropertySource;
import com.grb.util.property.impl.SystemPropertySource;

/**
 * This transform prints out the data received on its input and 
 * then passes it through to its output.
 */
public class Printer extends BaseTransform {
    private enum PrintOutput {
        stdout,
        stderr,
        debugLog,
        errorLog,
        fatalLog,
        infoLog,
        warnLog
    }
    
    /**
     * This property indicates the format of the output. 
     * If there is a "%s" in the format then this will 
     * contain the input data.
     */
    public static final String PRINT_FORMAT_PROPERTY = "format";

    /**
     * Default format value.
     */
    public static final String PRINT_FORMAT_PROPERTY_DEFAULT = "%s\n";

    /**
     * This property indicates where to send the print output.
     * One of [stdout, stderr, debugLog, errorLog, fatalLog, infoLog, warnLog].
     * The log options use Log4j.
     */
    public static final String PRINT_OUTPUT_PROPERTY = "output";

    /**
     * Default print output value.
     */
    public static final String PRINT_OUTPUT_PROPERTY_DEFAULT = "stdout";

    /**
     * This property indicates which property in the argMap to print.
     * To print the whole argMap use "*", to print none "-"
     */
    public static final String PRINT_ARG_MAP_KEY_PROPERTY = "argMapKey";

    /**
     * Default print arg map key value.
     */
    public static final String PRINT_ARG_MAP_KEY_PROPERTY_DEFAULT = "*";

    private String _format;
    private String _argMapKey;
    private PrintOutput[] _outputs;
    
    public Printer(String transformName, String instanceName,
            TransformCreationContext transformCreationContext, Object ... args) {
        super(transformName, instanceName, transformCreationContext);
    }

    @Override
    public void init() throws Exception {
        super.init();
        _format = getStringProperty(PRINT_FORMAT_PROPERTY);
        _argMapKey = getStringProperty(PRINT_ARG_MAP_KEY_PROPERTY);
        String outputs = getStringProperty(PRINT_OUTPUT_PROPERTY);
        if (outputs == null) {
            _outputs = new PrintOutput[0];
        } else {
            String[] outputArray = outputs.split(",");
            _outputs = new PrintOutput[outputArray.length];
            for (int i = 0; i < outputArray.length; i++) {
                _outputs[i] = PrintOutput.valueOf(outputArray[i]);
            }
        }
    }
    
    /**
     * [input] Incoming data object.
     * 
     * @param argMap Incoming data object.
     */
    @Input("onData")
    public void onData(Map<String, Object> argMap) {
        if ((argMap != null) && (!_argMapKey.equals("-"))) {
        	if (_argMapKey.equals("*")) {
            	int idx = 1;
            	int size = argMap.size();
            	Iterator<String> it = argMap.keySet().iterator();
            	while(it.hasNext()) {
            		String key = it.next();
            		Object value = argMap.get(key);
                    String in = convert(value);
                    String out = String.format("[argument %d of %d, key=%s] %s", idx, size, key, String.format(_format, in));
                    print(out);
            	}
        	} else {
        		Object value = argMap.get(_argMapKey);
        		String in = convert(value);
        		String out = String.format("[key=%s] %s", _argMapKey, String.format(_format, in));
                print(out);
        	}
        }
        onOutput(argMap);
    }
    
    private String convert(Object value) {
        return Impulse.format(value);
    }
    
    private void print(String out) {
        for(int j = 0; j < _outputs.length; j++) {
            switch(_outputs[j]) {
            case debugLog:
                _logger.debug(out);
                break;
                
            case errorLog:
                _logger.error(out);
                break;
                
            case fatalLog:
                _logger.fatal(out);
                break;
                
            case infoLog:
                _logger.info(out);
                break;
                
            case stderr:
                System.err.print(out);
                break;
                
            case stdout:
                System.out.print(out);
                break;
                
            case warnLog:
                _logger.warn(out);
                break;
            }
        }
    }
    
    /**
     * [output] Outgoing data object (unmodified).
     * 
     * @param argMap
     */
    @Output("onOutput")
    public void onOutput(Map<String, Object> argMap) {
        next("onOutput", argMap);
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
        
        Property<String> p = new Property<String>(PRINT_FORMAT_PROPERTY, PRINT_FORMAT_PROPERTY_DEFAULT, false,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(PRINT_FORMAT_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new SystemPropertySource<String>(ctx.getPropertyString(PRINT_FORMAT_PROPERTY), PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "This property indicates the format of the output");
        props.put(p.getId(), p);

        p = new Property<String>(PRINT_OUTPUT_PROPERTY, PRINT_OUTPUT_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(PRINT_OUTPUT_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new SystemPropertySource<String>(ctx.getPropertyString(PRINT_OUTPUT_PROPERTY), PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "This property indicates where to send the print output");
        props.put(p.getId(), p);

        p = new Property<String>(PRINT_ARG_MAP_KEY_PROPERTY, PRINT_ARG_MAP_KEY_PROPERTY_DEFAULT, true,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(PRINT_ARG_MAP_KEY_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new SystemPropertySource<String>(ctx.getPropertyString(PRINT_ARG_MAP_KEY_PROPERTY), PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "This property indicates which arg map property to print");
        props.put(p.getId(), p);
    }
}
