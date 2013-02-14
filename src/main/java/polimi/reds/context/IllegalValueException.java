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

package polimi.reds.context;

/**
 * Thrown to indicate that a method has been passed a value that is not
 * compatible with the operator
 * 
 */
public class IllegalValueException extends RuntimeException {

	private static final long serialVersionUID = -3022466693112989270L;

	public IllegalValueException() {
		super();
	}

	public IllegalValueException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public IllegalValueException(String arg0) {
		super(arg0);
	}

	public IllegalValueException(Throwable arg0) {
		super(arg0);
	}

}
