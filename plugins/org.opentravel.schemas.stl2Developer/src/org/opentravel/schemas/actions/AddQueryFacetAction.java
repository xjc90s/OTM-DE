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
package org.opentravel.schemas.actions;

import org.opentravel.schemacompiler.model.TLFacetType;
import org.opentravel.schemas.commands.ContextualFacetHandler;
import org.opentravel.schemas.node.interfaces.LibraryMemberInterface;
import org.opentravel.schemas.node.typeProviders.ChoiceObjectNode;
import org.opentravel.schemas.node.typeProviders.facetOwners.BusinessObjectNode;
import org.opentravel.schemas.properties.ExternalizedStringProperties;
import org.opentravel.schemas.properties.StringProperties;
import org.opentravel.schemas.stl2developer.MainWindow;

/**
 * Front end for ContextualFacetHandler()
 * 
 * @see ContextualFacetHandler#addContextualFacet(ChoiceObjectNode)
 * 
 * @author Dave Hollander
 * 
 */
public class AddQueryFacetAction extends OtmAbstractAction {
	private static StringProperties propsDefault = new ExternalizedStringProperties("action.addQuery");

	public AddQueryFacetAction(final MainWindow mainWindow) {
		super(mainWindow, propsDefault);
	}

	public AddQueryFacetAction(final MainWindow mainWindow, final StringProperties props) {
		super(mainWindow, props);
	}

	@Override
	public void run() {
		LibraryMemberInterface current = getOwnerOfNavigatorSelection();
		if (current instanceof BusinessObjectNode)
			new ContextualFacetHandler().addContextualFacet((BusinessObjectNode) current, TLFacetType.QUERY);
	}

	@Override
	public boolean isEnabled() {
		// Unmanaged or in the most current (head) library in version chain.
		LibraryMemberInterface n = getOwnerOfNavigatorSelection();
		return n instanceof BusinessObjectNode ? n.isEditable_newToChain() : false;
	}
}
