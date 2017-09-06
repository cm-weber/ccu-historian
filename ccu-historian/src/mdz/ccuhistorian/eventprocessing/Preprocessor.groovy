/*
    CCU-Historian, a long term archive for the HomeMatic CCU
    Copyright (C) 2011-2017 MDZ (info@ccu-historian.de)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package mdz.ccuhistorian.eventprocessing

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import mdz.eventprocessing.BasicProducer
import mdz.eventprocessing.Processor
import mdz.hc.DataPointIdentifier
import mdz.hc.Event
import mdz.hc.ProcessValue
import mdz.ccuhistorian.Main
import mdz.Utilities

@CompileStatic
@Slf4j
public class Preprocessor extends BasicProducer<Event> implements Processor<Event, Event> {

	public enum Type { DISABLED, DELTA_COMPR, TEMPORAL_COMPR }
	
	public void consume(Event event) throws Exception {
		try {
			int typeIndex=(event.dataPoint.attributes.preprocType as Integer)?:Type.DISABLED.ordinal()
			if (typeIndex!=Type.DISABLED.ordinal()) {
				if (typeIndex<0 || typeIndex>=Type.values().length)
					log.warn 'Preprocessor: Invalid preprocessing type {} (data point: {})', 
						typeIndex, event.dataPoint.id
				else {
					Type type=Type.values()[typeIndex]
					double param=(event.dataPoint.attributes.preprocParam as Double)?:0.0D
					switch (type) {
						case Type.DELTA_COMPR: event=applyDelta(event, param); break
						case Type.TEMPORAL_COMPR: event=applyTemporal(event, param); break
					}
				}
			}
			if (event!=null)
				produce event
		} catch (Throwable t) {
			log.error 'Preprocessor: Error', t
			// TODO: adjust when switching to logback
			log.debug '{}', Utilities.getStackTrace(t)
			Main.restart()
		}
	}
	
	public void stop() {
		log.debug 'Stopping preprocessor'
	}

	private Map<DataPointIdentifier, ProcessValue> deltaPreviousValues=[:]

	private Event applyDelta(Event event, double param) {
		log.trace 'Preprocessor: Applying delta compression to {}', event
		ProcessValue previousValue=deltaPreviousValues[event.dataPoint.id]
		event=applyDeltaHelper(event, previousValue, param)
		if (event!=null)
			deltaPreviousValues[event.dataPoint.id]=event.pv
		event
	}
	
	private Event applyDeltaHelper(Event event, ProcessValue previousValue, double param) {
		if (previousValue==null) return event
		if (!event.pv.value.class.is(previousValue.value.class)) {
			log.trace 'Preprocessor: Data type of data point {} changed', event.dataPoint.id
			return event
		}
		if (event.pv.value instanceof Number) {
			double last=((Number)previousValue.value).doubleValue()
			double current=((Number)event.pv.value).doubleValue()
			if (Math.abs(current-last)>=param) event 
			else {
				log.debug 'Preprocessor: Value change is below delta, event discarded (event: {})', event 
				null
			}
		} else if (event.pv.value instanceof Boolean) {
			boolean last=((Boolean)previousValue.value).booleanValue()
			boolean current=((Boolean)event.pv.value).booleanValue()
			if (current!=last) event
			else {
				log.debug 'Preprocessor: Value not changed, event discarded (event: {})', event
				null
			}
		} else if (event.pv.value instanceof String) {
			String last=(String)previousValue.value
			String current=(String)event.pv.value
			if (current!=last) event
			else {
				log.debug 'Preprocessor: Value not changed, event discarded (event: {})', event
				null
			}
		} else {
			log.warn 'Preprocessor: Invalid data type {} for delta preprocessing (data point: {})', 
				event.pv.value.class.name, event.dataPoint.id
			event
		}
	}
	
	private Map<DataPointIdentifier, Date> temporalTimestamps=[:]
	
	private Event applyTemporal(Event event, double param) {
		log.trace 'Preprocessor: Applying temporal compression to {}', event
		Date lastTimestamp=temporalTimestamps[event.dataPoint.id]
		
		if (lastTimestamp==null || (event.pv.timestamp.time-lastTimestamp.time)>=(param*1000)) {
			temporalTimestamps[event.dataPoint.id]=event.pv.timestamp
			event
		} else {
			log.debug 'Preprocessor: Time not elapsed, event discarded (event: {})', event
			null
		}
	}
}