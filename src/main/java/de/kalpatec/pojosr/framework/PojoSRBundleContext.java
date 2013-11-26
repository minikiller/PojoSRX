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

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import de.kalpatec.pojosr.framework.felix.framework.ServiceRegistry;
import de.kalpatec.pojosr.framework.felix.framework.capabilityset.SimpleFilter;
import de.kalpatec.pojosr.framework.felix.framework.util.EventDispatcher;

class PojoSRBundleContext implements BundleContext {

	private final Bundle m_bundle;
	private final ServiceRegistry m_reg;
	private final EventDispatcher m_dispatcher;
	private final Map<Long, Bundle> m_bundles;

	public PojoSRBundleContext(Bundle bundle, ServiceRegistry reg, EventDispatcher dispatcher, Map<Long, Bundle> bundles) {
		m_bundle = bundle;
		m_reg = reg;
		m_dispatcher = dispatcher;
		m_bundles = bundles;
	}

	@Override
	public boolean ungetService(ServiceReference reference) {
		return m_reg.ungetService(m_bundle, reference);
	}

	@Override
	public void removeServiceListener(ServiceListener listener) {
		m_dispatcher.removeListener(this, ServiceListener.class, listener);
	}

	@Override
	public void removeFrameworkListener(FrameworkListener listener) {
		m_dispatcher.removeListener(this, FrameworkListener.class, listener);
	}

	@Override
	public void removeBundleListener(BundleListener listener) {
		m_dispatcher.removeListener(this, BundleListener.class, listener);
	}

	@Override
	public ServiceRegistration registerService(String clazz, Object service, Dictionary properties) {
		return m_reg.registerService(m_bundle, new String[] { clazz }, service, properties);
	}

	@Override
	public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
		return m_reg.registerService(m_bundle, clazzes, service, properties);
	}

	@Override
	public Bundle installBundle(String location) throws BundleException {
		throw new BundleException("pojosr can't do that");
	}

	@Override
	public Bundle installBundle(String location, InputStream input) throws BundleException {

		throw new BundleException("pojosr can't do that");
	}

	@Override
	public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		return getAllServiceReferences(clazz, filter);
	}

	@Override
	public ServiceReference getServiceReference(String clazz) {
		try {
			return getBestServiceReference(getAllServiceReferences(clazz, null));
		} catch (InvalidSyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	private ServiceReference getBestServiceReference(ServiceReference[] refs) {
		if (refs == null) {
			return null;
		}

		if (refs.length == 1) {
			return refs[0];
		}

		// Loop through all service references and return
		// the "best" one according to its rank and ID.
		ServiceReference bestRef = refs[0];
		for (int i = 1; i < refs.length; i++) {
			if (bestRef.compareTo(refs[i]) < 0) {
				bestRef = refs[i];
			}
		}

		return bestRef;
	}

	@Override
	public Object getService(ServiceReference reference) {
		return m_reg.getService(m_bundle, reference);
	}

	@Override
	public String getProperty(String key) {
		return System.getProperty(key);
	}

	@Override
	public File getDataFile(String filename) {
		File root = new File("bundle" + m_bundle.getBundleId());
		if (System.getProperty("org.osgi.framework.storage") != null) {
			root = new File(new File(System.getProperty("org.osgi.framework.storage")), root.getName());
		}
		root.mkdirs();
		return filename.trim().length() > 0 ? new File(root, filename) : root;
	}

	@Override
	public Bundle[] getBundles() {
		Bundle[] result = m_bundles.values().toArray(new Bundle[m_bundles.size()]);
		Arrays.sort(result, new Comparator<Bundle>() {
			@Override
			public int compare(Bundle o1, Bundle o2) {
				return (int) (o1.getBundleId() - o2.getBundleId());
			}
		});
		return result;
	}

	@Override
	public Bundle getBundle(long id) {
		return m_bundles.get(id);
	}

	@Override
	public Bundle getBundle() {
		return m_bundle;
	}

	@Override
	public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		SimpleFilter simple = null;
		if (filter != null) {
			try {
				simple = SimpleFilter.parse(filter);
			} catch (Exception ex) {
				throw new InvalidSyntaxException(ex.getMessage(), filter);
			}
		}
		List<ServiceReference> result = m_reg.getServiceReferences(clazz, simple);
		return result.isEmpty() ? null : result.toArray(new ServiceReference[result.size()]);
	}

	@Override
	public Filter createFilter(String filter) throws InvalidSyntaxException {
		return FrameworkUtil.createFilter(filter);
	}

	@Override
	public void addServiceListener(ServiceListener listener) {
		try {
			addServiceListener(listener, null);
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void addServiceListener(final ServiceListener listener, String filter) throws InvalidSyntaxException {
		m_dispatcher.addListener(this, ServiceListener.class, listener, filter == null ? null : FrameworkUtil.createFilter(filter));
	}

	@Override
	public void addFrameworkListener(FrameworkListener listener) {
		m_dispatcher.addListener(this, FrameworkListener.class, listener, null);
	}

	@Override
	public void addBundleListener(BundleListener listener) {
		m_dispatcher.addListener(this, BundleListener.class, listener, null);
	}

	@Override
	public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
		return (ServiceRegistration<S>) registerService(clazz.getName(), service, properties);
	}

	@Override
	public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
		return (ServiceReference<S>) getServiceReference(clazz.getName());
	}

	@Override
	public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter) throws InvalidSyntaxException {
		ServiceReference<S>[] refs = (ServiceReference<S>[]) getServiceReferences(clazz.getName(), filter);
		if (refs == null) {
			return Collections.emptyList();
		}
		return Arrays.asList(refs);
	}

	@Override
	public Bundle getBundle(String location) {
		for (Bundle bundle : m_bundles.values()) {
			if (location.equals(bundle.getLocation())) {
				return bundle;
			}
		}
		return null;
	}
}
