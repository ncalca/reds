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
 * An <code>ObjectInputStream</code> that use the class annotations, if present,
 * as a codebase URL to retrieve the bytecode of the objects to be unmarshalled.
 * It uses the <code>RMIClassLoader</code> to load classes from the given
 * codebase (works only if a security manager is installed).
 * 
 * @see polimi.reds.broker.overlay.REDSMarshaller
 **********************************************************************/
public class REDSUnmarshaller extends ObjectInputStream {
	public REDSUnmarshaller(InputStream is) throws IOException {
		super(is);
	}

	protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		String codebase = null;
		try {
			codebase = (String) super.readObject();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try {
			if (codebase == null)
				return super.resolveClass(desc);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return java.rmi.server.RMIClassLoader.loadClass(codebase, desc.getName());
	}
}
