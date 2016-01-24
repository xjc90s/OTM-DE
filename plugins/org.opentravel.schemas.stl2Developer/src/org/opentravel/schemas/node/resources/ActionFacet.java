/**
 * Copyright (C) 2014 OpenTravel Alliance (info@opentravel.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opentravel.schemas.node.resources;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.opentravel.schemacompiler.model.LibraryMember;
import org.opentravel.schemacompiler.model.NamedEntity;
import org.opentravel.schemacompiler.model.TLActionFacet;
import org.opentravel.schemacompiler.model.TLFacet;
import org.opentravel.schemacompiler.model.TLFacetType;
import org.opentravel.schemacompiler.model.TLReferenceType;
import org.opentravel.schemas.node.ChoiceObjectNode;
import org.opentravel.schemas.node.CoreObjectNode;
import org.opentravel.schemas.node.FacetNode;
import org.opentravel.schemas.node.Node;
import org.opentravel.schemas.node.resources.ResourceField.ResourceFieldType;
import org.opentravel.schemas.properties.Images;
import org.opentravel.schemas.properties.Messages;
import org.opentravel.schemas.views.RestResourceView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action Facet Controller. Provides getters, setters and listeners for editable fields.
 * 
 * @author Dave
 *
 */
public class ActionFacet extends ResourceBase<TLActionFacet> {
	private static final Logger LOGGER = LoggerFactory.getLogger(RestResourceView.class);
	protected String MSGKEY = "rest.ActionFacet";

	class BasePayloadListener implements ResourceFieldListener {
		@Override
		public boolean set(String name) {
			setBasePayload(name);
			return false;
		}
	}

	class ReferenceNameListener implements ResourceFieldListener {
		@Override
		public boolean set(String name) {
			setReferenceFacetName(name);
			return false;
		}
	}

	class ReferenceRepeatListener implements ResourceFieldListener {
		@Override
		public boolean set(String value) {
			setReferenceRepeat(Integer.parseInt(value));
			return false;
		}
	}

	class ReferenceTypeListener implements ResourceFieldListener {
		@Override
		public boolean set(String value) {
			setReferenceType(value);
			return false;
		}
	}

	/***************************************************************************
	 * 
	 */
	public ActionFacet(TLActionFacet tlActionFacet) {
		super(tlActionFacet);
		parent = this.getNode(((LibraryMember) tlObj.getOwningResource()).getListeners());
		assert parent instanceof ResourceNode;
		getParent().addChild(this);
		this.setLibrary(parent.getLibrary());
	}

	public ActionFacet(ResourceNode parent) {
		super(new TLActionFacet());
		this.parent = parent;
		parent.addChild(this);
		this.setLibrary(parent.getLibrary());
		getParent().getTLModelObject().addActionFacet(tlObj);
	}

	/**
	 * Create an action facet for subject business object or one of its facets.
	 * 
	 * @param parent
	 * @param FacetType
	 *            - type of facet or NULL to get the full substitution group
	 * @return
	 */
	public ActionFacet(ResourceNode parent, TLFacetType type) {
		this(parent);
		if (type == null) {
			setName("SubstitutionGroup");
			setReferenceFacetName(ResourceField.SUBGRP);
		} else
			for (Node fn : parent.getSubject().getChildren())
				if (fn instanceof FacetNode)
					if (((TLFacet) fn.getTLModelObject()).getFacetType().equals(type)) {
						setReferenceFacetName(fn.getLabel());
						setName(fn.getLabel());
					}
		setReferenceType(TLReferenceType.REQUIRED.toString());
	}

	@Override
	public ResourceNode getParent() {
		return (ResourceNode) parent;
	}

	public void addChildren() {
	}

	/**
	 * @return a list of core and choice objects
	 */
	public List<Node> getBasePayloads() {
		List<Node> candidates = new ArrayList<Node>();
		for (Node n : getLibrary().getDescendants_NamedTypes())
			if (n instanceof CoreObjectNode)
				candidates.add(n);
			else if (n instanceof ChoiceObjectNode)
				candidates.add(n);
		return candidates;
	}

	/**
	 * @return a list of core and choice objects by name including "NONE"
	 */
	public String[] getBasePayloadCandidates() {
		List<Node> candidates = getBasePayloads();
		String[] names = new String[candidates.size() + 1];
		int i = 0;
		names[i++] = ResourceField.NONE;
		for (Node n : candidates)
			names[i++] = n.getName();
		return names;
	}

	@Override
	public String getComponentType() {
		return "Action Facet";
	}

	public String getReferenceFacetName() {
		return tlObj.getReferenceFacetName() != null ? tlObj.getReferenceFacetName() : ResourceField.SUBGRP;
	}

	@Override
	public List<ResourceField> getFields() {
		List<ResourceField> fields = new ArrayList<ResourceField>();

		// Base Payload
		new ResourceField(fields, tlObj.getBasePayloadName(), MSGKEY + ".fields.basePayload", ResourceFieldType.Enum,
				new BasePayloadListener(), getBasePayloadCandidates());

		// Facet Reference = This can only be set to a facet in the resource subject business object
		new ResourceField(fields, getReferenceFacetName(), MSGKEY + ".fields.referenceFacetName",
				ResourceFieldType.Enum, new ReferenceNameListener(), getOwningComponent().getSubjectFacets(true));

		// Repeat Count - an int
		new ResourceField(fields, Integer.toString(tlObj.getReferenceRepeat()), MSGKEY + ".fields.referenceRepeat",
				ResourceFieldType.Int, new ReferenceRepeatListener());

		// Reference Type - enum list
		new ResourceField(fields, getReferenceType(), MSGKEY + ".fields.referenceType",
				ResourceField.ResourceFieldType.Enum, new ReferenceTypeListener(), getReferenceTypeStrings());

		return fields;
	}

	@Override
	public Image getImage() {
		return Images.getImageRegistry().get(Images.ActionFacet);

	}

	@Override
	public String getName() {
		return tlObj.getName() != null ? tlObj.getName() : "";
	}

	public String getReferenceType() {
		return tlObj.getReferenceType() != null ? tlObj.getReferenceType().toString() : "";
	}

	@Override
	public TLActionFacet getTLModelObject() {
		return tlObj;
	}

	@Override
	public String getTooltip() {
		return Messages.getString(MSGKEY + ".tooltip");
	}

	@Override
	public boolean isNameEditable() {
		return true;
	}

	@Override
	public void setName(final String name) {
		tlObj.setName(name);
	}

	/**
	 * @param name
	 *            of the business object facet
	 */
	public void setReferenceFacetName(String name) {
		if (name.equals(ResourceField.SUBGRP))
			tlObj.setReferenceFacetName(null);
		else
			tlObj.setReferenceFacetName(name);
		LOGGER.debug("Set Reference facet name to " + name + " : " + tlObj.getReferenceFacetName());
	}

	public void setReferenceRepeat(Integer i) {
		tlObj.setReferenceRepeat(i);
		LOGGER.debug("Set Reference repeat to " + i);
	}

	public void setReferenceType(String value) {
		tlObj.setReferenceType(TLReferenceType.valueOf(value));
		LOGGER.debug("Set Reference Type to " + value + " : " + tlObj.getReferenceType());
	}

	public boolean setBasePayload(String actionObject) {
		NamedEntity basePayload = null;
		for (Node n : getBasePayloads())
			if (n.getName().equals(actionObject) && n.getTLModelObject() instanceof NamedEntity)
				basePayload = (NamedEntity) n.getTLModelObject();
		tlObj.setBasePayload(basePayload);
		return true;
	}
}