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
package mdz.hc;

import mdz.hc.DataPointIdentifier;

import org.junit.Test

class DataPointIdentifierTest {
	
	@Test
	public void testDataPointIdentifier() {
		DataPointIdentifier dpid=new DataPointIdentifier('itf', 'addr', 'ident')
		assert dpid.toString()=='itf.addr.ident'
		
		DataPointIdentifier dpid2=new DataPointIdentifier('itf', 'addr', 'ident')
		assert dpid==dpid2
		assert dpid.hashCode()==dpid2.hashCode() 
		
		dpid2.interfaceId='itf2'
		assert dpid!=dpid2
		
		dpid=['itf2','','']
		assert dpid.toString()=='itf2..'
		
		dpid=[null, null, 'ident3']
		assert dpid.toString()=='..ident3'
	}
}
