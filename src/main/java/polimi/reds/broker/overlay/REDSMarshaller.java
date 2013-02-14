/***
 * * REDS - REconfigurable Dispatching System
 * * Copyright (C) 2003 Politecnico di Milano
 * * <mailto: cugola@elet.polimi.it> <mailto: picco@elet.polimi.it>
 * *
 * * This library is free software; you can redistribute it and/or modify it
 * * under the terms of the GNU Lesser General Public License as published by
 * * the Free Software Foundation; either version 2.1 of the License, or (at
 * * your option) any later version.
 * *
 * * This library is distributed in the hope that it will be useful, but
 * * WITHOUT ANY WARRANTY; without even the implied warranty of
 * * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * * General Public License for more details.
 * *
 * * You should have received a copy of the GNU Lesser General Public License
 * * along with this library; if not, write to the Free Software Foundation,
 * * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 ***/

package polimi.reds.broker.overlay;

import java.io.*;

/**********************************************************************
 * An <code>ObjectOutputStream</code> that annotate classes with a codebase URL.
 * This codebase is used at unmarshalling time to retrieve the bytecode of the
 * objects to be unmarshalled. The codebase URL is either taken from the system
 * property: "polimi.reds.client.codebase", or it is the codebase associated to
 * classes retrieved via a REDSUnmarshaller.
 * 
 * @see polimi.reds.broker.overlay.REDSUnmarshaller
 **********************************************************************/
public class REDSMarshaller extends ObjectOutputStream {
	private final String CODEBASE_PROPERTY_NAME = "polimi.reds.client.codebase";

	public REDSMarshaller(OutputStream os) throws IOException {
		super(os);
		os.flush();
	}

	protected void annotateClass(Class cl) throws IOException {
		String codebase = java.rmi.server.RMIClassLoader.getClassAnnotation(cl);
		if (codebase == null)
			codebase = System.getProperty(CODEBASE_PROPERTY_NAME);
		writeObject(codebase);
	}
}
