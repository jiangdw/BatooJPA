/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.batoo.jpa.core.impl.model.type;

import java.util.Map;
import java.util.Set;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.batoo.jpa.core.impl.model.MetamodelImpl;
import org.batoo.jpa.core.impl.model.attribute.AssociatedSingularAttribute;
import org.batoo.jpa.core.impl.model.attribute.AttributeImpl;
import org.batoo.jpa.core.impl.model.attribute.BasicAttribute;
import org.batoo.jpa.core.impl.model.attribute.CollectionAttributeImpl;
import org.batoo.jpa.core.impl.model.attribute.EmbeddedAttribute;
import org.batoo.jpa.core.impl.model.attribute.ListAttributeImpl;
import org.batoo.jpa.core.impl.model.attribute.PluralAttributeImpl;
import org.batoo.jpa.core.impl.model.attribute.SetAttributeImpl;
import org.batoo.jpa.core.impl.model.attribute.SingularAttributeImpl;
import org.batoo.jpa.parser.impl.AbstractLocator;
import org.batoo.jpa.parser.metadata.attribute.AttributesMetadata;
import org.batoo.jpa.parser.metadata.attribute.BasicAttributeMetadata;
import org.batoo.jpa.parser.metadata.attribute.EmbeddedAttributeMetadata;
import org.batoo.jpa.parser.metadata.attribute.ManyToManyAttributeMetadata;
import org.batoo.jpa.parser.metadata.attribute.ManyToOneAttributeMetadata;
import org.batoo.jpa.parser.metadata.attribute.OneToManyAttributeMetadata;
import org.batoo.jpa.parser.metadata.attribute.OneToOneAttributeMetadata;
import org.batoo.jpa.parser.metadata.type.ManagedTypeMetadata;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Implementation of {@link ManagedType}.
 * 
 * @param <X>
 *            The represented type.
 * 
 * @author hceylan
 * @since $version
 */
public abstract class ManagedTypeImpl<X> extends TypeImpl<X> implements ManagedType<X> {

	private final AbstractLocator locator;
	private final Map<String, AttributeImpl<X, ?>> declaredAttributes = Maps.newHashMap();
	private final Map<String, SingularAttributeImpl<X, ?>> declaredSingularAttributes = Maps.newHashMap();
	private final Map<String, PluralAttributeImpl<X, ?, ?>> declaredPluralAttributes = Maps.newHashMap();
	private final Map<String, AttributeImpl<? super X, ?>> attributes = Maps.newHashMap();
	private final Map<String, SingularAttributeImpl<? super X, ?>> singularAttributes = Maps.newHashMap();
	private final Map<String, PluralAttributeImpl<? super X, ?, ?>> pluralAttributes = Maps.newHashMap();

	/**
	 * @param metamodel
	 *            the meta model
	 * @param clazz
	 *            the class of the represented type
	 * @param metadata
	 *            the metadata
	 * 
	 * @since $version
	 * @author hceylan
	 */
	public ManagedTypeImpl(MetamodelImpl metamodel, Class<X> clazz, ManagedTypeMetadata metadata) {
		super(metamodel, clazz);

		this.locator = metadata.getLocator();
	}

	/**
	 * Adds the attribute into attributes.
	 * 
	 * @param attribute
	 *            the declared attribute
	 * 
	 * @since $version
	 * @author hceylan
	 */
	@SuppressWarnings("unchecked")
	protected void addAttribute(AttributeImpl<? super X, ?> attribute) {
		if (attribute.getDeclaringType() == this) {
			this.declaredAttributes.put(attribute.getName(), (AttributeImpl<X, ?>) attribute);
		}

		this.attributes.put(attribute.getName(), attribute);

		if (attribute instanceof SingularAttribute) {
			if (attribute.getDeclaringType() == this) {
				this.declaredSingularAttributes.put(attribute.getName(), (SingularAttributeImpl<X, ?>) attribute);
			}

			this.singularAttributes.put(attribute.getName(), (SingularAttributeImpl<X, ?>) attribute);
		}
		else {
			if (attribute.getDeclaringType() == this) {
				this.declaredPluralAttributes.put(attribute.getName(), (PluralAttributeImpl<X, ?, ?>) attribute);
			}

			this.pluralAttributes.put(attribute.getName(), (PluralAttributeImpl<? super X, ?, ?>) attribute);
		}
	}

	/**
	 * Creates and adds the attributes of the managed type from the metadata.
	 * 
	 * @param typeMetadata
	 *            the metadata
	 * 
	 * @since $version
	 * @author hceylan
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void addAttributes(ManagedTypeMetadata typeMetadata) {
		final AttributesMetadata attributesMetadata = typeMetadata.getAttributes();

		// basic attributes
		for (final BasicAttributeMetadata metadata : attributesMetadata.getBasics()) {
			this.addAttribute(new BasicAttribute(this, metadata));
		}

		// many to one attributes
		for (final ManyToOneAttributeMetadata metadata : attributesMetadata.getManyToOnes()) {
			this.addAttribute(new AssociatedSingularAttribute(this, PersistentAttributeType.MANY_TO_ONE, metadata));
		}

		// one to one attributes
		for (final OneToOneAttributeMetadata metadata : attributesMetadata.getOneToOnes()) {
			this.addAttribute(new AssociatedSingularAttribute(this, PersistentAttributeType.ONE_TO_ONE, metadata));
		}

		// one to many attributes
		for (final OneToManyAttributeMetadata metadata : attributesMetadata.getOneToManies()) {
			this.addAttribute(PluralAttributeImpl.create(this, metadata, PersistentAttributeType.ONE_TO_MANY));
		}

		// many to many attributes
		for (final ManyToManyAttributeMetadata metadata : attributesMetadata.getManyToManies()) {
			this.addAttribute(PluralAttributeImpl.create(this, metadata, PersistentAttributeType.MANY_TO_MANY));
		}

		// embedded attributes
		for (final EmbeddedAttributeMetadata metadata : attributesMetadata.getEmbeddeds()) {
			this.addAttribute(new EmbeddedAttribute(this, metadata));
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public AttributeImpl<? super X, ?> getAttribute(String name) {
		return this.attributes.get(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public Set<Attribute<? super X, ?>> getAttributes() {
		final Set<Attribute<? super X, ?>> attributes = Sets.newHashSet();
		attributes.addAll(this.attributes.values());

		return attributes;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public CollectionAttributeImpl<? super X, ?> getCollection(String name) {
		return (CollectionAttributeImpl<? super X, ?>) this.getAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> CollectionAttributeImpl<? super X, E> getCollection(String name, Class<E> elementType) {
		return (CollectionAttributeImpl<? super X, E>) this.getAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public AttributeImpl<X, ?> getDeclaredAttribute(String name) {
		return this.declaredAttributes.get(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public Set<Attribute<X, ?>> getDeclaredAttributes() {
		final Set<Attribute<X, ?>> attributes = Sets.newHashSet();
		attributes.addAll(this.declaredAttributes.values());

		return attributes;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public CollectionAttributeImpl<X, ?> getDeclaredCollection(String name) {
		return (CollectionAttributeImpl<X, ?>) this.getDeclaredAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> CollectionAttributeImpl<X, E> getDeclaredCollection(String name, Class<E> elementType) {
		return (CollectionAttributeImpl<X, E>) this.getDeclaredAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public ListAttributeImpl<X, ?> getDeclaredList(String name) {
		return (ListAttributeImpl<X, ?>) this.getDeclaredAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> ListAttributeImpl<X, E> getDeclaredList(String name, Class<E> elementType) {
		return (ListAttributeImpl<X, E>) this.getDeclaredAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public MapAttribute<X, ?, ?> getDeclaredMap(String name) {
		return (MapAttribute<X, ?, ?>) this.getDeclaredAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <K, V> MapAttribute<X, K, V> getDeclaredMap(String name, Class<K> keyType, Class<V> valueType) {
		return (MapAttribute<X, K, V>) this.getDeclaredAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public Set<PluralAttribute<X, ?, ?>> getDeclaredPluralAttributes() {
		final Set<PluralAttribute<X, ?, ?>> attributes = Sets.newHashSet();
		attributes.addAll(this.declaredPluralAttributes.values());

		return attributes;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public SetAttributeImpl<X, ?> getDeclaredSet(String name) {
		return (SetAttributeImpl<X, ?>) this.getDeclaredAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> SetAttributeImpl<X, E> getDeclaredSet(String name, Class<E> elementType) {
		return (SetAttributeImpl<X, E>) this.getDeclaredAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public SingularAttributeImpl<X, ?> getDeclaredSingularAttribute(String name) {
		return (SingularAttributeImpl<X, ?>) this.getDeclaredAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <Y> SingularAttributeImpl<X, Y> getDeclaredSingularAttribute(String name, Class<Y> type) {
		return (SingularAttributeImpl<X, Y>) this.getDeclaredAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public Set<SingularAttribute<X, ?>> getDeclaredSingularAttributes() {
		final Set<SingularAttribute<X, ?>> attributes = Sets.newHashSet();
		attributes.addAll(this.declaredSingularAttributes.values());

		return attributes;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public ListAttributeImpl<? super X, ?> getList(String name) {
		return (ListAttributeImpl<? super X, ?>) this.getAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> ListAttributeImpl<? super X, E> getList(String name, Class<E> elementType) {
		return (ListAttributeImpl<? super X, E>) this.getAttribute(name);
	}

	/**
	 * Returns the locator of the managed type.
	 * 
	 * @return the locator of the managed type
	 * 
	 * @since $version
	 * @author hceylan
	 */
	public AbstractLocator getLocator() {
		return this.locator;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public MapAttribute<? super X, ?, ?> getMap(String name) {
		return (MapAttribute<? super X, ?, ?>) this.getAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <K, V> MapAttribute<? super X, K, V> getMap(String name, Class<K> keyType, Class<V> valueType) {
		return (MapAttribute<? super X, K, V>) this.getMap(name);
	}

	/**
	 * Returns the name of the type.
	 * 
	 * @return the name of the type
	 * 
	 * @since $version
	 * @author hceylan
	 */
	public String getName() {
		return this.getJavaType().getSimpleName();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public Set<PluralAttribute<? super X, ?, ?>> getPluralAttributes() {
		final Set<PluralAttribute<? super X, ?, ?>> attributes = Sets.newHashSet();
		attributes.addAll(this.pluralAttributes.values());

		return attributes;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public SetAttributeImpl<? super X, ?> getSet(String name) {
		return (SetAttributeImpl<? super X, ?>) this.getAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> SetAttributeImpl<? super X, E> getSet(String name, Class<E> elementType) {
		return (SetAttributeImpl<? super X, E>) this.getAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public SingularAttributeImpl<? super X, ?> getSingularAttribute(String name) {
		return (SingularAttributeImpl<? super X, ?>) this.getAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <Y> SingularAttributeImpl<? super X, Y> getSingularAttribute(String name, Class<Y> type) {
		return (SingularAttributeImpl<? super X, Y>) this.getAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public Set<SingularAttribute<? super X, ?>> getSingularAttributes() {
		final Set<SingularAttribute<? super X, ?>> attributes = Sets.newHashSet();
		attributes.addAll(this.singularAttributes.values());

		return attributes;
	}
}
