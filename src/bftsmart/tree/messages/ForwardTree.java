/**
Copyright (c) 2019-2019 Tulio Alberton Ribeiro.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package bftsmart.tree.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import bftsmart.communication.SystemMessage;
import bftsmart.consensus.messages.ConsensusMessage;

/**
 * This class represents a message used in the spanning tree configurations.
 */
public class ForwardTree extends SystemMessage {

	private byte[] signature; // signature
	private ConsensusMessage cm;
	/**
	 * Constructors.
	 */
	public ForwardTree() {
	}

	public ForwardTree(int from, ConsensusMessage cm) {
		super(from);
		this.cm = cm;
	}

	/**
	 * Externalizable implementation methods
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);// write from

		out.writeInt(signature.length);
		out.write(signature);
		out.writeObject(cm);
		
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

		super.readExternal(in);

		this.signature = new byte[in.readInt()];
		in.read(this.signature);
		this.cm = (ConsensusMessage) in.readObject();
		
	}

	/**
	 * getters and setters
	 * */ 
	
	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	public byte[] getSignature() {
		return signature;
	}

	public ConsensusMessage getConsensusMessage() {
		return this.cm;
	}
	
	
}
