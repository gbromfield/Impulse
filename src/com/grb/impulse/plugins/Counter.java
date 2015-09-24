package com.grb.impulse.plugins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

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
import com.grb.util.property.impl.ValueOfConverter;

/**
 * This transform counts the pieces of data received 
 * by the input "onData" and sends the data to output
 * "onMatch" if the count number matches a number or is
 * part of a range in the match property. If there is
 * no match the output "onNoMatch" is called.
 * <ul>
 * <li>if match property = "1" then the first piece of data 
 * goes to output "onMatch", the rest to output "onNoMatch".</li>
 * <li>if match property = "1-10" then the first 10 pieces 
 * go to output "onMatch", the rest to output "onNoMatch".</li>
 * <li>if match property = "*" then then all pieces 
 * go to output "onMatch", and nothing to output "onNoMatch".</li>
 * <li>if match property = "-" then then all pieces 
 * go to output "onNoMatch", and nothing to output "onMatch".</li>
 * <li>if match property = "even" then then all even count pieces 
 * go to output "onMatch", and odd to output "onNoMatch".</li>
 * <li>if match property = "odd" then then all odd count pieces 
 * go to output "onMatch", and even to output "onNoMatch".</li>
 * <li>if match property = "10th" then every 10th piece 
 * goes to output "onMatch", and everything else to output "onNoMatch".</li>
 * <li>if match property = "10%" then 10% of the data 
 * goes to output "onMatch", and 90% to output "onNoMatch".</li>
 * </ul>
 */
public class Counter extends BaseTransform {
	/**
	 * A comma separated list of integers and ranges of integers.
	 * <ul>
	 * <li>Example: 1,3-5,6,10- (matches the first, third to fifth, 
	 * sixth, and tenth and greater pieces of data)
     * <li>Example: * (matches all)
     * <li>Example: - (matches none)
     * <li>Example: even (matches even counts)
     * <li>Example: odd (matches odd counts)
     * <li>Example: 5th (matches every 5th)
     * <li>Example: 50% (matches 50% of the data)
     * </ul>
	 * <p>
     * Specifiable in the configuration file or command line. 
	 */
    public static final String MATCH_PROPERTY = "match";

    /**
     * Default global match value.
     */
    public static final String MATCH_PROPERTY_DEFAULT = "*";

    /**
     * This property indicates whether a globally scoped value is to be used 
     * to do the counting. If global counting is true the counter continues
     * counting regardless of home many Counter instances have been created.
     * This is useful if you want the counting to transcend reconnects where
     * a new Counter object may be instantiated resetting an instance level 
     * counter.
     * Specifiable in the configuration file or command line. 
     */
    public static final String GLOBAL_COUNTER_PROPERTY = "globalCounter";
    
    /**
     * Default global counter value.
     */
    public static final boolean GLOBAL_COUNTER_PROPERTY_DEFAULT = false;
    
    /**
     * This property keeps the value of the instance parameter.
     * This is incremented only when the {@link #GLOBAL_COUNTER_PROPERTY}
     * is set to false.
     */
    public static final String COUNT_PROPERTY = "count";
    
    /**
     * Default count value.
     */
    public static final int COUNT_PROPERTY_DEFAULT = 0;


    private String _matchStr;
    private boolean _globalCounter;
    private Property<Integer> _countProp;
    private PropertySource<Integer> _countJMXSource;
    private ArrayList<Integer> _minList;
    private ArrayList<Integer> _maxList;
    private boolean _matchAll;
    private boolean _matchNone;
    private boolean _matchEven;
    private boolean _matchOdd;
    private int _matchNth;
    private Double _percentage;
    private int _numPercentageMatched;
    private Object _matchLock;
    
    public Counter(String transformName, String instanceName,
            TransformCreationContext transformCreationContext, Object ... args) {
        super(transformName, instanceName, transformCreationContext);
        _minList = new ArrayList<Integer>();
        _maxList = new ArrayList<Integer>();
        _matchAll = false;
        _matchNone = false;
        _matchEven = false;
        _matchOdd = false;
        _matchNth = 0;
        _percentage = null;
        _numPercentageMatched = 0;
        _matchLock = new Object();
    }

    @SuppressWarnings("unchecked")
	@Override
    public void init() throws Exception {
        super.init();
        _matchStr = getStringProperty(MATCH_PROPERTY);
        _globalCounter = getBooleanProperty(GLOBAL_COUNTER_PROPERTY);
        if (_globalCounter) {
            // get global template property
            Iterator<Property<?>> it = getTransformContext().getProperties().values().iterator();
            while(it.hasNext()) {
                Property<?> p = it.next();
                if (p.getId().equals(COUNT_PROPERTY)) {
                    _countProp = (Property<Integer>)p;                    
                }
            }
            // remove local count property
            _properties.remove(COUNT_PROPERTY);
        } else {
            _countProp = (Property<Integer>)getProperty(COUNT_PROPERTY);
        }
        _countJMXSource = _countProp.getSource(JMXUtils.JMX_SOURCE);
        parseMatchStr();
    }

    /**
     * [input] - Incoming data. If there is a match {@link #onMatch(Map)} is
     * called, else {@link #onNoMatch(Map)}
     * 
     * @param argMap Incoming data.
     */
    @Input("onData")
    public void onData(Map<String, Object> argMap) {
        int count = _countProp.getValue() + 1;
        try {
			_countJMXSource.setValue(count);
		} catch (PropertyVetoException e) {
			// shouldn't happen
		}
        if (matches(count)) {
            onMatch(argMap);
        } else {
            onNoMatch(argMap);
        }
    }

    /**
     * [output] Called when a match is made.
     * 
     * @param argMap
     */
    @Output("onMatch")
    public void onMatch(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug(String.format("Matching count %d for data %s", _countProp.getValue(), Impulse.format(argMap)));
        }
        next("onMatch", argMap);
    }

    /**
     * [output] Called when a match is NOT made.
     * 
     * @param argMap
     */
    @Output("onNoMatch")
    public void onNoMatch(Map<String, Object> argMap) {
        if (_logger.isDebugEnabled()) {
            _logger.debug(String.format("No match for count %d for data %s", _countProp.getValue(), Impulse.format(argMap)));
        }
        next("onNoMatch", argMap);
    }
    
    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
        super.setAttribute(attribute);
        if (attribute.getName().equals(MATCH_PROPERTY)) {
            _matchStr = getStringProperty(MATCH_PROPERTY);
            parseMatchStr();
        }
    }

    private void parseMatchStr() {
        synchronized(_matchLock) {
            if (_matchStr != null) {
                _minList.clear();
                _maxList.clear();
                _matchAll = false;
                _matchNone = false;
                _matchEven = false;
                _matchOdd = false;
                _matchNth = 0;
                _percentage = null;
                _numPercentageMatched = 0;
                String trimmedLowerMatchStr = _matchStr.trim().toLowerCase();
                if (trimmedLowerMatchStr.equals("*")) {
                    _matchAll = true;
                } else if (trimmedLowerMatchStr.equals("-")) {
                    _matchNone = true;
                } else if (trimmedLowerMatchStr.equals("even")) {
                    _matchEven = true;
                } else if (trimmedLowerMatchStr.equals("odd")) {
                    _matchOdd = true;
                } else if ((trimmedLowerMatchStr.endsWith("nd")) ||
                           (trimmedLowerMatchStr.endsWith("rd")) || 
                           (trimmedLowerMatchStr.endsWith("th")) ||
                           (trimmedLowerMatchStr.endsWith("st"))) {
                    _matchNth = Integer.parseInt(trimmedLowerMatchStr.substring(0, trimmedLowerMatchStr.length() - 2));
                } else if (trimmedLowerMatchStr.endsWith("%")) {
                    _percentage = Double.parseDouble(trimmedLowerMatchStr.substring(0, trimmedLowerMatchStr.length() - 1));
                    if ((_percentage < 0) || (_percentage > 100)) {
                        throw new IllegalArgumentException("Illegal format of a percentage: " + _percentage + ", must be in the range [0-100]");
                    }
                } else {
                    String[] commaList = trimmedLowerMatchStr.split(",");
                    for(int i = 0; i < commaList.length; i++) {
                        String[] dashList = commaList[i].split("-");
                        if (dashList.length == 1) {
                            if (commaList[i].contains("-")) {
                                int value = Integer.parseInt(dashList[0]);
                                _minList.add(value);
                                _maxList.add(Integer.MAX_VALUE);
                            } else {
                                int value = Integer.parseInt(dashList[0]);
                                _minList.add(value);
                                _maxList.add(value);
                            }
                        } else if (dashList.length == 2) {
                            _minList.add(Integer.parseInt(dashList[0]));
                            _maxList.add(Integer.parseInt(dashList[1]));
                        } else {
                            throw new IllegalArgumentException("Illegal format of a range: " + commaList[i]);
                        }
                    }
                }
            }
        }
    }
    
    private boolean matches(int value) {
        synchronized(_matchLock) {
            if (_matchAll) {
                return true;
            } else if (_matchNone) {
                return false;
            } else if (_matchEven) {
                return ((value % 2) == 0);
            } else if (_matchOdd) {
                return ((value % 2) == 1);
            } else if (_matchNth >= 2) {
                return ((value % _matchNth) == 0);
            } else if (_percentage != null) {
                double random = Math.random();
                double percent = _percentage / 100;
                boolean matched = (random < percent);
                if (matched) {
                    _numPercentageMatched++;
                }
                if (_logger.isDebugEnabled()) {
                    int total = _countProp.getValue();
                    int percentageMatched = (int)(((double)_numPercentageMatched / (double)total) * 100);
                    _logger.debug(String.format("Stats - Random: %f, Configured: %f, Matched: %d, Total: %d, %% Matched: %d%%",
                            random, percent, _numPercentageMatched, total, percentageMatched));
                }
                return matched;  
            } else {
                for (int i = 0; i < _minList.size(); i++) {
                    int min = _minList.get(i);
                    int max = _maxList.get(i);
                    if ((value >= min) && (value <= max)) {
                        return true;
                    }
                }
            }
            return false;
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

        Property<String> p = new Property<String>(MATCH_PROPERTY, MATCH_PROPERTY_DEFAULT,
                new PropertySource<String>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1),
                new SystemPropertySource<String>(ctx.getPropertyString(MATCH_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID),
                new MapPropertySource<String>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(MATCH_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID));
        p.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "A comma separated list of integers and ranges of integers. Example: 1,3-5,6,10-");
        props.put(p.getId(), p);

        Property<Boolean> bp = new Property<Boolean>(GLOBAL_COUNTER_PROPERTY, GLOBAL_COUNTER_PROPERTY_DEFAULT,
                new PropertySource<Boolean>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1, ValueOfConverter.getConverter(Boolean.class)),
                new SystemPropertySource<Boolean>(ctx.getPropertyString(GLOBAL_COUNTER_PROPERTY), PropertySource.PRIORITY_2, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)),
                new MapPropertySource<Boolean>(Impulse.CONFIG_FILE_SOURCE, ctx.getPropertyString(GLOBAL_COUNTER_PROPERTY), Impulse.ConfigProperties, PropertySource.PRIORITY_3, PropertySource.NULL_INVALID, ValueOfConverter.getConverter(Boolean.class)));
        bp.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "This property indicates whether a globally scoped value is to be used to do the counting");
        props.put(bp.getId(), bp);
        
        Property<Integer> ip = new Property<Integer>(COUNT_PROPERTY, COUNT_PROPERTY_DEFAULT, 
                new PropertySource<Integer>(JMXUtils.JMX_SOURCE, PropertySource.PRIORITY_1, ValueOfConverter.getConverter(Integer.class)));
        ip.getUserDataMap().put(Impulse.PROPERTY_DESCRIPTION_KEY, "This property keeps the value of the instance count parameter");
        props.put(ip.getId(), ip);        
    }
}
