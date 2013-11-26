/*
 * Copyright 2011 Karl Pauls karlpauls@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.kalpatec.pojosr.framework;

import java.util.Enumeration;
import java.util.zip.ZipEntry;

class EntriesEnumeration implements Enumeration {

	private final Enumeration m_enumeration;
	private final String m_prefix;
	private volatile Object current;

	public EntriesEnumeration(Enumeration enumeration) {
		this(enumeration, null);
	}

	public EntriesEnumeration(Enumeration enumeration, String prefix) {
		m_enumeration = enumeration;
		m_prefix = prefix;
	}

	@Override
	public boolean hasMoreElements() {
		while ((current == null) && m_enumeration.hasMoreElements()) {
			String result = (String) ((ZipEntry) m_enumeration.nextElement()).getName();
			if (m_prefix != null) {
				if (result.startsWith(m_prefix)) {
					current = result.substring(m_prefix.length());
				}
			} else {
				current = result;
			}
		}
		return (current != null);
	}

	@Override
	public Object nextElement() {
		try {
			if (hasMoreElements()) {
				return current;
			} else {
				return m_enumeration.nextElement();
			}
		} finally {
			current = null;
		}
	}
}