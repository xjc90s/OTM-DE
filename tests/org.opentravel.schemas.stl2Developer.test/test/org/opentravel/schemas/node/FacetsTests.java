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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opentravel.schemacompiler.model.TLAbstractFacet;
import org.opentravel.schemacompiler.model.TLAttribute;
import org.opentravel.schemacompiler.model.TLFacet;
import org.opentravel.schemacompiler.model.TLFacetType;
import org.opentravel.schemacompiler.model.TLListFacet;
import org.opentravel.schemacompiler.model.TLProperty;
import org.opentravel.schemacompiler.model.TLSimpleFacet;
import org.opentravel.schemacompiler.model.TLValueWithAttributes;
import org.opentravel.schemacompiler.repository.Repository;
import org.opentravel.schemas.controllers.DefaultProjectController;
import org.opentravel.schemas.controllers.DefaultRepositoryController;
import org.opentravel.schemas.controllers.MainController;
import org.opentravel.schemas.modelObject.TLValueWithAttributesFacet;
import org.opentravel.schemas.node.facets.ChoiceFacetNode;
import org.opentravel.schemas.node.facets.CustomFacetNode;
import org.opentravel.schemas.node.facets.FacetNode;
import org.opentravel.schemas.node.facets.ListFacetNode;
import org.opentravel.schemas.node.facets.OperationFacetNode;
import org.opentravel.schemas.node.facets.OperationNode;
import org.opentravel.schemas.node.facets.QueryFacetNode;
import org.opentravel.schemas.node.facets.RenamableFacet;
import org.opentravel.schemas.node.facets.RoleFacetNode;
import org.opentravel.schemas.node.facets.SimpleFacetNode;
import org.opentravel.schemas.node.facets.VWA_AttributeFacetNode;
import org.opentravel.schemas.node.properties.AttributeNode;
import org.opentravel.schemas.node.properties.ElementNode;
import org.opentravel.schemas.node.properties.PropertyNode;
import org.opentravel.schemas.node.properties.RoleNode;
import org.opentravel.schemas.testUtils.LoadFiles;
import org.opentravel.schemas.testUtils.MockLibrary;
import org.opentravel.schemas.testUtils.NodeTesters;
import org.opentravel.schemas.trees.repository.RepositoryNode;
import org.opentravel.schemas.types.TestTypes;
import org.opentravel.schemas.utils.FacetNodeBuilder;
import org.opentravel.schemas.utils.LibraryNodeBuilder;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Dave Hollander
 * 
 */
public class FacetsTests {
	static final Logger LOGGER = LoggerFactory.getLogger(FacetsTests.class);

	ModelNode model = null;
	TestTypes tt = new TestTypes();

	NodeTesters nt = new NodeTesters();
	LoadFiles lf = new LoadFiles();
	LibraryTests lt = new LibraryTests();
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

	@Test
	public void constructorTests() {

		// Given two libraries, one managed one not managed
		ln = ml.createNewLibrary("http://www.test.com/test1", "test1", defaultProject);
		LibraryNode ln_inChain = ml.createNewLibrary("http://www.test.com/test1c", "test1c", defaultProject);
		new LibraryChainNode(ln_inChain);
		ln_inChain.setEditable(true);
		assertTrue("Library must exist.", ln != null);
		assertTrue("Library must exist.", ln_inChain != null);
		constructorTL_Tests();
	}

	private void constructorTL_Tests() {
		// Given - one of each TLFacet type.
		TLFacet tlf = new TLFacet();
		assertTrue("Facet must not be null.", tlf != null);
		TLAbstractFacet tlAf = new TLFacet();
		assertTrue("Facet must not be null.", tlAf != null);
		TLListFacet tlLf = new TLListFacet(tlAf); // tlaf should be simple or detail.
		assertTrue("Facet must not be null.", tlLf != null);
		TLSimpleFacet tlSf = new TLSimpleFacet();
		assertTrue("Facet must not be null.", tlSf != null);
		TLValueWithAttributes tlVWA = new TLValueWithAttributes();
		assertTrue("Facet must not be null.", tlVWA != null);
		TLValueWithAttributesFacet tlVf = new TLValueWithAttributesFacet(tlVWA);
		assertTrue("Facet must not be null.", tlVf != null);
	}

	@Test
	public void buildersTests() {
		ln = ml.createNewLibrary("http://www.test.com/test1", "test1", defaultProject);

		FacetNode facetNode1 = FacetNodeBuilder.create(ln).addElements("E1", "E2", "E3").build();
		FacetNode facetNode2 = FacetNodeBuilder.create(ln).addAttributes("A1", "A2", "A3").build();
		FacetNode facetNode3 = FacetNodeBuilder.create(ln).addIndicators("I1", "I2").addAliases("Alias1").build();
		assertTrue("Built node 1 must not be null.", facetNode1 != null);
		assertTrue("Built node 2 must not be null.", facetNode2 != null);
		assertTrue("Built node 3 must not be null.", facetNode3 != null);
	}

	@Test
	public void inheritanceTests() {
		// Given a BO that extends another BO so that there are inherited children
		ln = ml.createNewLibrary("http://www.test.com/test1", "test1", defaultProject);
		BusinessObjectNode baseBO = ml.addBusinessObjectToLibrary(ln, "BaseBO");
		CustomFacetNode c1 = (CustomFacetNode) baseBO.addFacet("BaseC1", TLFacetType.CUSTOM);
		AttributeNode a1 = new AttributeNode(c1, "cAttr1");

		BusinessObjectNode extendedBO = ml.addBusinessObjectToLibrary_Empty(ln, "ExBO");
		new ElementNode(extendedBO.getSummaryFacet(), "ExEle");

		// When - objects are extended
		extendedBO.setExtension(baseBO);
		assertTrue("ExtendedBO extends BaseBO.", extendedBO.isExtendedBy(baseBO));

		// Then - there should be an inherited facet.
		// List<Node> inheritedFacets = extendedBO.getInheritedChildren(); // FIXME - should not be empty
		CustomFacetNode inheritedCustom = (CustomFacetNode) extendedBO.getCustomFacets().get(0);
		assertTrue("Must have inherited custom facet.", inheritedCustom != null);

		// Then - there should be inherited children in the facets.
		List<Node> inheritedKids = extendedBO.getSummaryFacet().getInheritedChildren();
		List<Node> kids = extendedBO.getSummaryFacet().getChildren();
		assertTrue("Extended BO must have children.", !kids.isEmpty());
		assertTrue("Extended BO must have inherited children.", !inheritedKids.isEmpty());

		// When - delete the attribute in c1 (base custom)
		inheritedKids = inheritedCustom.getInheritedChildren();
		kids = inheritedCustom.getChildren();
		c1.remove(a1);
		inheritedCustom.getChildren();
	}

	@Test
	public void copyFacetTest() {
		ln = ml.createNewLibrary("http://www.test.com/test1", "test1", defaultProject);

		// Given a facet with 8 mixed properties
		FacetNode facetNode1 = FacetNodeBuilder.create(ln).addElements("E1", "E2", "E3").addIndicators("I1", "I2")
				.addAttributes("A1", "A2", "A3").build();
		assertTrue("Starting facet must have 8 properties.", facetNode1.getChildren().size() == 8);

		// Given an standard facet and a VWA Attribute facet
		TLFacet tlf = new TLFacet();
		tlf.setFacetType(TLFacetType.SUMMARY);
		FacetNode fn = (FacetNode) NodeFactory.newComponentMember(null, tlf);
		VWA_Node vwa = ml.addVWA_ToLibrary(ln, "myVWA"); // vwa with one attr
		VWA_AttributeFacetNode an = (VWA_AttributeFacetNode) vwa.getAttributeFacet();

		// When copied
		fn.copyFacet(facetNode1); // FIXME - very slow
		an.copyFacet(facetNode1);

		// Then both should have 8 properties
		assertTrue("Facet must have 8 properties.", fn.getChildren().size() == 8);
		assertTrue("Attribute facet must have 9 properties.", an.getChildren().size() == 9);
	}

	// TODO
	// createPropertyTests()
	// getComponentType

	@Test
	public void renameableFacetTests() throws Exception {
		lf.loadTestGroupA(mc);
		for (LibraryNode lib : pc.getDefaultProject().getLibraries()) {
			lib.setEditable(true);
			assertTrue("Library must be editable.", lib.isEditable());
			ml.addChoice(lib, "choice1");
			classBasedTests(lib);
		}
	}

	private void classBasedTests(LibraryNode lib) {
		LOGGER.debug("Checking query facets in " + lib);
		for (Node n : lib.getDescendants()) {
			if (n instanceof RenamableFacet)
				checkFacet((RenamableFacet) n);
			else if (n instanceof SimpleFacetNode)
				checkFacet((SimpleFacetNode) n);
			else if (n instanceof OperationFacetNode)
				checkFacet((OperationFacetNode) n);
			else if (n instanceof ListFacetNode)
				checkFacet((ListFacetNode) n);
			else if (n instanceof OperationNode)
				checkFacet((OperationNode) n);
			else if (n instanceof VWA_AttributeFacetNode)
				checkFacet((VWA_AttributeFacetNode) n);
			else if (n instanceof RoleFacetNode)
				checkRoleFacet((FacetNode) n);
			else if (n instanceof FacetNode)
				checkBaseFacet((FacetNode) n);
		}
	}

	@Test
	public void roleFacetTests() throws Exception {
		String myNS = "http://local/junits";
		// ln = ml.createNewLibrary("http://www.test.com/test1", "test1", defaultProject);
		ln = LibraryNodeBuilder.create("Example", myNS, "p", new Version(1, 1, 0)).build(defaultProject, pc);
		ln.setEditable(true);
		assertTrue("Library is minor version.", ln.isMinorVersion());

		CoreObjectNode core = ml.addCoreObjectToLibrary(ln, "Core1");
		checkRoleFacet(core.getRoleFacet());
		List<Node> inheritedKids = core.getRoleFacet().getInheritedChildren();
		// TODO - make sure minor version has inherited children

		DefaultRepositoryController rc = (DefaultRepositoryController) mc.getRepositoryController();
		assertTrue("Repository controller is not null.", rc != null);
		List<RepositoryNode> repos = rc.getAll();
		RepositoryNode localRepoNode = rc.getLocalRepository();
		LOGGER.debug("Repo namespace is ", rc.getLocalRepository().getNamespaceWithPrefix());
		Repository localRepo = localRepoNode.getRepository();
		List<String> repoRootNSs = localRepo.listRootNamespaces();
		List<String> repoNSs = localRepo.listAllNamespaces();
		List<String> repoBaseNSs = localRepo.listBaseNamespaces();
		LOGGER.debug("Repo Root namespaces: ", repoRootNSs);
		LOGGER.debug("Repo Base namespaces: ", repoBaseNSs);
		LOGGER.debug("Repo All namespaces: ", repoNSs);
		try {
			localRepo.createRootNamespace(myNS);
		} catch (Exception e) {
			LOGGER.debug("Error setting Repo Root namespaces: ", e.getLocalizedMessage());
		}
		LOGGER.debug("Repo Root namespaces: ", localRepo.listRootNamespaces());

		ln.setNamespace(rc.getLocalRepository().getNamespace());
		LOGGER.debug("Set namespace to ", ln.getNamespace());
		List<LibraryNode> libs = new ArrayList<LibraryNode>();
		libs.add(ln);
		List<LibraryChainNode> lcns = rc.manage(rc.getLocalRepository(), libs);
		assertTrue("There are library chains.", !lcns.isEmpty());
	}

	public void checkFacet(ListFacetNode lf) {
		LOGGER.debug("Checking List Facet Node: " + lf);

		if (lf.isSimpleListFacet())
			LOGGER.debug("Simple List Facet Node");
		if (lf.isDetailListFacet())
			LOGGER.debug("Detail List Facet Node");

		assertFalse("Must NOT be delete-able.", lf.isDeleteable());
		assertFalse("Must NOT be valid parent.", lf.isValidParentOf(PropertyNodeType.ATTRIBUTE));
		assertFalse("Must NOT be valid parent.", lf.isValidParentOf(PropertyNodeType.ELEMENT));
	}

	public void checkFacet(SimpleFacetNode sf) {
		LOGGER.debug("Checking Simple Facet Node: " + sf);
		assertFalse("Must NOT be delete-able.", sf.isDeleteable());
	}

	public void checkFacet(VWA_AttributeFacetNode vf) {
		LOGGER.debug("Checking VWA Attribute Facet Node: " + vf);

		assertTrue("Must be valid parent of attribute.", vf.isValidParentOf(PropertyNodeType.ATTRIBUTE));
		assertTrue("Must be valid parent of indicator.", vf.isValidParentOf(PropertyNodeType.INDICATOR));
		assertTrue("Must be valid parent of id.", vf.isValidParentOf(PropertyNodeType.ID));

		assertFalse("Must NOT be delete-able.", vf.isDeleteable());
		assertFalse("Must NOT be type provider.", vf.isTypeProvider());
		assertFalse("Must NOT be assignable.", vf.isAssignable());
		assertFalse("Must NOT be valid parent of element.", vf.isValidParentOf(PropertyNodeType.ELEMENT));
		assertFalse("Must NOT be valid parent of ID Reference.", vf.isValidParentOf(PropertyNodeType.ID_REFERENCE));
		assertFalse("Must NOT be valid parent of indicator element.",
				vf.isValidParentOf(PropertyNodeType.INDICATOR_ELEMENT));

		// Behaviors
		List<Node> properties = new ArrayList<Node>();
		PropertyNode p = new AttributeNode(new TLAttribute(), null);
		p.setName("attr1");
		properties.add(p);
		vf.addProperties(properties, false);
		assertTrue("Must have new property as child.", vf.getChildren().contains(p));
	}

	public void checkFacet(OperationNode sf) {
		LOGGER.debug("Checking Operation Node: " + sf);
		// assertTrue("Must be delete-able.", sf.isDeleteable());
	}

	public void checkFacet(OperationFacetNode of) {
		LOGGER.debug("Checking Operation Facet Node: " + of);

		assertTrue("Must be delete-able.", of.isDeleteable());
		assertFalse("Must NOT be type provider.", of.isTypeProvider());
		assertFalse("Must NOT be assignable.", of.isAssignable());
	}

	public void checkBaseFacet(FacetNode fn) {
		LOGGER.debug("Checking Facet: " + fn);
		if (fn.isIDFacet())
			LOGGER.debug("Checking id Facet: " + fn);
		if (fn.isSummaryFacet())
			LOGGER.debug("Checking summary Facet: " + fn);
		if (fn.isDetailFacet())
			LOGGER.debug("Checking detail Facet: " + fn);
		if (fn.isSimpleListFacet())
			LOGGER.debug("Checking simple list Facet: " + fn);
		if (fn.isDetailListFacet())
			LOGGER.debug("Checking detail list Facet: " + fn);

		assertTrue("Must be valid parent to attributes.", fn.isValidParentOf(PropertyNodeType.ATTRIBUTE));
		assertTrue("Must be valid parent to elements.", fn.isValidParentOf(PropertyNodeType.ELEMENT));
		assertFalse("Must NOT be delete-able.", fn.isDeleteable());

		// assertTrue("Must be assignable.", qn.isAssignable());
		// assertTrue("Must be assignable to complex.", qn.isComplexAssignable());
		// assertTrue("Must be type provider.", qn.isTypeProvider());
		// assertFalse("May be default facet.", qn.isDefaultFacet());

		// assertFalse("Must be renamable.", qn.isRenameable());
		// assertFalse("Must NOT be assignable to element ref", qn.isAssignableToElementRef());
		// assertFalse("Must NOT be assignable to simple.", qn.isAssignableToSimple());
		// assertFalse("Must NOT be assignable to simple.", qn.isSimpleAssignable());
		// assertFalse("Must NOT be assignable to VWA.", qn.isAssignableToVWA());
		// assertFalse("Must be query facet.", qn.isQueryFacet());
		// assertFalse("Must NOT be custom facet.", qn.isCustomFacet());
		// assertFalse("Must NOT be named type.", qn.isNamedType());

		// Behaviors
		AttributeNode attr = new AttributeNode(fn, "att1");
		ElementNode ele = new ElementNode(fn, "ele1");
		assertTrue("Must be able to add attributes.", attr.getParent() == fn);
		assertTrue("Must be able to add elements.", ele.getParent() == fn);

		// Create array of attribute and element properties to add
		List<Node> properties = new ArrayList<Node>();
		attr = new AttributeNode(new TLAttribute(), null);
		ele = new ElementNode(new TLProperty(), null);
		attr.setName("attr1");
		ele.setName("ele1");
		properties.add(attr);
		properties.add(ele);

		// Then add using facet method
		fn.addProperties(properties, false);
		assertTrue("Must have new property as child.", fn.getChildren().contains(attr));
		assertTrue("Must have new property as child.", fn.getChildren().contains(ele));
	}

	public void checkFacet(QueryFacetNode qn) {
		LOGGER.debug("Checking query Facet: " + qn);
		assertTrue("Must be delete-able.", qn.isDeleteable());
	}

	public void checkFacet(RenamableFacet rf) {
		LOGGER.debug("Checking rename-able Facet: " + rf);
		final String NEWCONTEXT = "myContext"; // must be ignored
		final String NEWNAME = "myName";

		assertTrue("Must be renamable.", rf.isRenameable());

		// setContext()
		// String dc = rf.getLibrary().getDefaultContextId();
		// String fc = ((TLFacet) rf.getTLModelObject()).getContext();
		// assertTrue("Initial context must be default context.",
		// rf.getLibrary().getDefaultContextId().equals(((TLFacet) rf.getTLModelObject()).getContext()));
		rf.setContext(NEWCONTEXT);
		// fc = ((TLFacet) rf.getTLModelObject()).getContext();
		assertTrue("Context must be set to default.",
				rf.getLibrary().getDefaultContextId().equals(((TLFacet) rf.getTLModelObject()).getContext()));

		// setName()
		//
		rf.setName(NEWNAME);
		String n = rf.getName();
		assertTrue("Facet must have new name.", rf.getName().contains(NEWNAME));

		// Inherited statements
		//
		assertTrue("Must be assignable.", rf.isAssignable());
		assertTrue("Must be assignable to complex.", rf.isComplexAssignable());
		assertTrue("Must be type provider.", rf.isTypeProvider());
		assertTrue("Must be valid parent to attributes.", rf.isValidParentOf(PropertyNodeType.ATTRIBUTE));
		assertTrue("Must be valid parent to elements.", rf.isValidParentOf(PropertyNodeType.ELEMENT));

		assertFalse("Must NOT be assignable to element ref", rf.isAssignableToElementRef());
		assertFalse("Must NOT be assignable to simple.", rf.isAssignableToSimple());
		assertFalse("Must NOT be assignable to simple.", rf.isSimpleAssignable());
		assertFalse("Must NOT be assignable to VWA.", rf.isAssignableToVWA());
		assertFalse("Must NOT be default facet.", rf.isDefaultFacet());
		assertFalse("Must NOT be named type.", rf.isNamedType());

		// Behaviors
		//
		AttributeNode attr = new AttributeNode(rf, "att1");
		ElementNode ele = new ElementNode(rf, "ele1");
		assertTrue("Must be able to add attributes.", attr.getParent() == rf);
		assertTrue("Must be able to add elements.", ele.getParent() == rf);

		if (rf instanceof QueryFacetNode)
			checkFacet((QueryFacetNode) rf);
		else if (rf instanceof CustomFacetNode)
			checkFacet((CustomFacetNode) rf);
		else if (rf instanceof ChoiceFacetNode)
			checkFacet((ChoiceFacetNode) rf);
	}

	public void checkFacet(CustomFacetNode qn) {
		LOGGER.debug("Checking Facet: " + qn);
		assertTrue("Must be delete-able.", qn.isDeleteable());
	}

	public void checkFacet(ChoiceFacetNode qn) {
		LOGGER.debug("Checking Facet: " + qn);
		assertTrue("Must be delete-able.", qn.isDeleteable());
	}

	public void checkRoleFacet(FacetNode roleFacet) {
		LOGGER.debug("Checking Facet: " + roleFacet);

		assertTrue("Must be role facet.", roleFacet instanceof RoleFacetNode);
		// assertTrue("Must be delete-able.", roleFacet.isDeleteable());
		assertTrue("Must be assignable.", roleFacet.isAssignable());
		assertTrue("Must be assignable to complex.", roleFacet.isComplexAssignable());
		assertTrue("Must be type provider.", roleFacet.isTypeProvider());
		assertTrue("Must be valid parent to roles.", roleFacet.isValidParentOf(PropertyNodeType.ROLE));
		// FIXME - assertTrue("Must be assignable to VWA.", roleFacet.isAssignableToVWA());

		assertFalse("Must NOT be assignable to simple.", roleFacet.isAssignableToSimple());
		assertFalse("Must NOT be assignable to simple.", roleFacet.isSimpleAssignable());
		assertFalse("Must NOT be valid parent to attributes.", roleFacet.isValidParentOf(PropertyNodeType.ATTRIBUTE));
		assertFalse("Must NOT be valid parent to elements.", roleFacet.isValidParentOf(PropertyNodeType.ELEMENT));
		assertFalse("Must NOT be renamable.", roleFacet.isRenameable());

		assertFalse("Must NOT be assignable to element ref", roleFacet.isAssignableToElementRef());
		assertFalse("Must NOT be default facet.", roleFacet.isDefaultFacet());
		assertFalse("Must NOT be named type.", roleFacet.isNamedType());

		// Behaviors
		RoleNode role = new RoleNode((RoleFacetNode) roleFacet, "newRole1");
		assertTrue("Must be able to add roles.", role.getParent() == roleFacet);
		assertTrue("Must be able to add child.", roleFacet.getChildren().contains(role));
	}
}