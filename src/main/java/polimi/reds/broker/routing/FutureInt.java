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

package polimi.reds.broker.routing;

/**
 * It contains an <code>int</code> value that can be set in a delayed instant.
 * This is useful when an unknown value must be returned. When that value will
 * be available it can be set using the <code>setValue</code> method. If one
 * tries to read the value before it was set, it is blocked until the value is
 * set.
 * 
 * @author Alessandro Monguzzi
 */
public class FutureInt {
	private int value = -1;

	/**
	 * Default constructor. The value is not set.
	 */
	public FutureInt() {
	}

	/**
	 * Sets the <code>value</code> and awake all those were waiting on it.
	 * 
	 * @param value
	 *            The value of the <code>FutureInt</code>
	 */
	public FutureInt(int value) {
		this.value = value;
		synchronized (this) {
			this.notifyAll();
		}
	}

	/**
	 * Checks whether the <code>value</code> has been set or not.
	 * 
	 * @return <code>true</code> iff the <code>value</code> has been set.
	 */
	public boolean isDone() {
		if (value == -1)
			return false;
		return true;
	}

	/**
	 * Sets the value and awake all those were waiting on it.
	 * 
	 * @param value
	 *            the value to set.
	 */
	public void setValue(int value) {
		this.value = value;
		synchronized (this) {
			this.notifyAll();
		}
	}

	/**
	 * Gets the value of the FutureInt. This method is blocking until
	 * <code>FutureInt.isDone</code> == <code>true</code>.
	 * 
	 * @return the value
	 */
	public int getValue() {
		if (!this.isDone())
			try {
				synchronized (this) {
					this.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		return value;
	}

	/**
	 * Decrements of 1 the value of the FutureInt. This method is blocking until
	 * <code>FutureInt.isDone</code> == <code>true</code>.
	 */
	public void decrement() {
		if (!this.isDone())
			try {
				synchronized (this) {
					this.wait();
				}
			} catch (InterruptedException e) {
			}
		value--;
	}
}
