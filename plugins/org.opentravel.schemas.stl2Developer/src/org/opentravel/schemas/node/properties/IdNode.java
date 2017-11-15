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
package org.opentravel.schemas.node.properties;

import org.opentravel.schemacompiler.model.TLAttribute;
import org.opentravel.schemacompiler.model.TLAttributeType;
import org.opentravel.schemas.node.ModelNode;
import org.opentravel.schemas.node.Node;
import org.opentravel.schemas.node.NodeFactory;
import org.opentravel.schemas.node.NodeFinders;
import org.opentravel.schemas.node.interfaces.INode;
import org.opentravel.schemas.types.TypeProvider;

/**
 * A property node that represents an XML ID. See {@link NodeFactory#newMemberOLD(INode, Object)}
 * 
 * @author Dave Hollander
 * 
 */
public class IdNode extends AttributeNode {
	TypeProvider idType = null;

	public IdNode(PropertyOwnerInterface parent, String name) {
		super(parent, name);
		if (name == null || name.isEmpty())
			name = "id";
		setName(name);
		idType = (TypeProvider) NodeFinders.findNodeByName("ID", ModelNode.XSD_NAMESPACE);
		Node x;
		if (idType == null)
			x = NodeFinders.findNodeByName("ID", ModelNode.XSD_NAMESPACE);
		assert idType != null;
		getTLModelObject().setType((TLAttributeType) idType.getTLModelObject());
	}

	public IdNode(TLAttribute tlObj, PropertyOwnerInterface parent) {
		super(tlObj, parent);
		idType = (TypeProvider) NodeFinders.findNodeByName("ID", ModelNode.XSD_NAMESPACE);
		getTLModelObject().setType((TLAttributeType) idType.getTLModelObject());
	}

	@Override
	public boolean canAssign(Node type) {
		return (type == idType);
	}

	@Override
	public TypeProvider getRequiredType() {
		return idType;
	}
	// @Override
	// public boolean setAssignedType(TLModelElement tla) {
	// return false; // You can't assign to ID
	// }
}
