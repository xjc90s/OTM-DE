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
/**
 * 
 */
package org.opentravel.schemas.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opentravel.schemacompiler.model.TLAbstractEnumeration;
import org.opentravel.schemacompiler.model.TLClosedEnumeration;
import org.opentravel.schemacompiler.model.TLEnumValue;
import org.opentravel.schemacompiler.model.TLOpenEnumeration;
import org.opentravel.schemas.controllers.DefaultProjectController;
import org.opentravel.schemas.controllers.MainController;
import org.opentravel.schemas.node.libraries.LibraryChainNode;
import org.opentravel.schemas.node.libraries.LibraryNode;
import org.opentravel.schemas.node.properties.EnumLiteralNode;
import org.opentravel.schemas.testUtils.LoadFiles;
import org.opentravel.schemas.testUtils.MockLibrary;
import org.opentravel.schemas.testUtils.NodeTesters;
import org.opentravel.schemas.types.TypeProvider;

/**
 * @author Dave Hollander
 * 
 */
public class Enumeration_Tests {
	ModelNode model = null;
	LoadFiles lf = new LoadFiles();
	MockLibrary ml = null;
	LibraryNode ln = null;
	MainController mc;
	DefaultProjectController pc;
	ProjectNode defaultProject;

	@Before
	public void beforeAllTests() {
		mc = new MainController();
		ml = new MockLibrary();
		pc = (DefaultProjectController) mc.getProjectController();
		defaultProject = pc.getDefaultProject();
	}

	public void check(EnumerationOpenNode e) {
		assertTrue(e.getParent() != null);
		assertTrue(e.getName() != null);
		assertTrue(e.getLibrary() != null);
		assertTrue(e.getTLModelObject().getOwningLibrary() != null);
		// no npe
		e.getChildren();
	}

	public void check(EnumerationClosedNode e) {
		assertTrue(e.getParent() != null);
		assertTrue(e.getName() != null);
		assertTrue(e.getLibrary() != null);
		assertTrue(e.getTLModelObject().getOwningLibrary() != null);
		// no npe
		e.getChildren();
	}

	public TLAbstractEnumeration createTL(boolean open, String name) {
		TLAbstractEnumeration tlae = null;
		if (open)
			tlae = new TLOpenEnumeration();
		else
			tlae = new TLClosedEnumeration();

		if (name == null || name.isEmpty())
			name = "EnumName";
		tlae.setName(name);
		TLEnumValue tlcv1 = new TLEnumValue();
		tlcv1.setLiteral("value 1");
		tlae.addValue(tlcv1);
		return tlae;
	}

	@Test
	public void EN_ConstructorsTests() {
		ln = ml.createNewLibrary(defaultProject.getNSRoot(), "test", defaultProject);
		assertTrue("Library must be editable as needed to addProperty.", ln.isEditable_newToChain());

		// When - create 2
		EnumerationClosedNode ecn = new EnumerationClosedNode((TLClosedEnumeration) createTL(false, "EnumC"));
		EnumerationOpenNode eon = new EnumerationOpenNode((TLOpenEnumeration) createTL(true, "Enum0"));

		ln.addMember(ecn);
		ln.addMember(eon);
		ml.check(ln);

		// When - create two then use them to create other type
		EnumerationClosedNode ecn2 = new EnumerationClosedNode((TLClosedEnumeration) createTL(false, "EnumC2"));
		EnumerationOpenNode eon2 = new EnumerationOpenNode((TLOpenEnumeration) createTL(true, "Enum02"));
		ln.addMember(ecn2);
		ln.addMember(eon2);
		ml.check(ln);

		EnumerationClosedNode ecn3 = new EnumerationClosedNode(eon2);
		EnumerationOpenNode eon3 = new EnumerationOpenNode(ecn2);
		assertTrue(ln.contains(ecn3));
		assertTrue(ln.contains(eon3));
		assertTrue(!ln.contains(ecn2));
		assertTrue(!ln.contains(eon2));
		ml.check(ln);
	}

	@Test
	public void EN_createEnums() {
		ln = ml.createNewLibrary(defaultProject.getNSRoot(), "test", defaultProject);
		TLClosedEnumeration tlc = new TLClosedEnumeration();
		tlc.setName("ClosedEnum");
		TLEnumValue tlcv1 = new TLEnumValue();
		tlcv1.setLiteral("value 1");
		tlc.addValue(tlcv1);

		EnumerationClosedNode closedEnum = new EnumerationClosedNode(tlc);
		Assert.assertNotNull(closedEnum);
		Assert.assertEquals(1, closedEnum.getChildren().size());

		EnumerationOpenNode openEnum = ml.addOpenEnumToLibrary(ln, "OpenEnum");
		Assert.assertNotNull(openEnum);
		Assert.assertEquals(1, openEnum.getChildren().size());
	}

	@Test
	public void changeEnums() throws Exception {
		ln = ml.createNewLibrary(defaultProject.getNSRoot(), "test", defaultProject);
		EnumerationOpenNode openEnum = ml.addOpenEnumToLibrary(ln, "OpenEnum");
		EnumerationClosedNode closedEnum = ml.addClosedEnumToLibrary(ln, "ClosedEnum");
		check(openEnum);
		check(closedEnum);

		EnumerationOpenNode o2 = new EnumerationOpenNode(closedEnum);
		EnumerationClosedNode c2 = new EnumerationClosedNode(openEnum);

	}

	@Test
	public void enumLiterals() {
		ln = ml.createNewLibrary(defaultProject.getNSRoot(), "test", defaultProject);
		EnumerationOpenNode openEnum = ml.addOpenEnumToLibrary(ln, "OpenEnum");
		EnumerationClosedNode closedEnum = ml.addClosedEnumToLibrary(ln, "ClosedEnum");

		EnumLiteralNode lit1 = new EnumLiteralNode(openEnum, "lit1");
		EnumLiteralNode lit2 = new EnumLiteralNode(openEnum, "lit2");

		assertNotNull("Must have assigned type.", lit1.getAssignedType());
		assertNotNull("Must have assigned type.", lit2.getAssignedType());

		TypeProvider type = lit1.getAssignedType();
		assertEquals("Must be required type.", lit1.getRequiredType(), type);
		type = lit2.getAssignedType();
		assertEquals("Must be required type.", lit2.getRequiredType(), type);
	}

	@Test
	public void changeEnumsManaged() throws Exception {
		LibraryChainNode lcn = ml.createNewManagedLibrary(defaultProject.getNSRoot(), "test", defaultProject);
		ln = lcn.getHead();

		EnumerationOpenNode openEnum = ml.addOpenEnumToLibrary(ln, "OpenEnum");
		EnumerationClosedNode closedEnum = ml.addClosedEnumToLibrary(ln, "ClosedEnum");
		Assert.assertTrue(ln.isValid()); // validates TL library

		EnumerationOpenNode o2 = new EnumerationOpenNode(closedEnum);
		EnumerationClosedNode c2 = new EnumerationClosedNode(openEnum);

		Assert.assertTrue(ln.isValid()); // validates TL library
		NodeTesters tt = new NodeTesters();
		tt.visitAllNodes(ln);

	}

	@Test
	public void enumBaseTypes() throws Exception {
		LibraryChainNode lcn = ml.createNewManagedLibrary(defaultProject.getNSRoot(), "test", defaultProject);
		ln = lcn.getHead();

		// Given - 4 enums - MUST have different names
		EnumerationOpenNode openBase = ml.addOpenEnumToLibrary(ln, "OpenEnumBase");
		EnumLiteralNode ob1 = new EnumLiteralNode(openBase, "o1");
		EnumLiteralNode ob2 = new EnumLiteralNode(openBase, "o2");

		EnumerationClosedNode closedBase = ml.addClosedEnumToLibrary(ln, "ClosedEnumBase");
		EnumLiteralNode cb1 = new EnumLiteralNode(closedBase, "c1");
		EnumLiteralNode cb2 = new EnumLiteralNode(closedBase, "c2");

		EnumerationOpenNode openExt = ml.addOpenEnumToLibrary(ln, "OpenExt");
		EnumLiteralNode oe1 = new EnumLiteralNode(openExt, "oe1");
		EnumerationClosedNode closedExt = ml.addClosedEnumToLibrary(ln, "ClosedExt");
		EnumLiteralNode ce1 = new EnumLiteralNode(closedExt, "ce1");

		// getExtendsTypeName used in FacetView
		assertTrue("Not extened must be empty", openBase.getExtendsTypeName().isEmpty());
		assertTrue("Not extened must be empty", openExt.getExtendsTypeName().isEmpty());
		assertTrue("Not extened must be empty", closedBase.getExtendsTypeName().isEmpty());
		assertTrue("Not extened must be empty", closedExt.getExtendsTypeName().isEmpty());
		check(openBase);
		check(openExt);
		check(closedBase);
		check(closedExt);

		// When - extend base with ext
		openExt.setExtension(openBase);
		closedExt.setExtension(closedBase);

		// Then
		assertTrue("Extension name must not be empty", !openExt.getExtendsTypeName().isEmpty());
		assertTrue("Ext must extend base", openExt.getExtensionBase() == openBase);
		assertTrue("ExtendsType must be the ExtensionBase", openExt.getExtensionBase() == openExt.getExtendsType());
		assertTrue("Extension name must not be empty", !closedExt.getExtendsTypeName().isEmpty());
		assertTrue("Ext must extend base", closedExt.getExtensionBase() == closedBase);

		// Then - test inherited children
		assertTrue("Must have inherited children", !openExt.getInheritedChildren().isEmpty());
		assertTrue("Must have inherited children", !closedExt.getInheritedChildren().isEmpty());

		ml.check(ln);
	}
}
