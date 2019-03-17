/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

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
package bftsmart.communication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;

import javax.crypto.Mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bftsmart.consensus.messages.ConsensusMessage;
import bftsmart.consensus.messages.MessageFactory;
import bftsmart.consensus.roles.AcceptorSSLTLS;
import bftsmart.statemanagement.SMMessage;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.ForwardedMessage;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.leaderchange.LCMessage;
import bftsmart.tom.util.TOMUtil;
import bftsmart.tree.TreeManager;
import bftsmart.tree.messages.TreeMessage;

/**
 *
 * @author edualchieri, Tulio Ribeiro
 */
public class MessageHandlerSSLTLS {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private AcceptorSSLTLS acceptor;
	private TOMLayer tomLayer;
	private Mac mac;
	private TreeManager tm;

	public MessageHandlerSSLTLS() {
		try {
			this.mac = TOMUtil.getMacFactory();
		} catch (NoSuchAlgorithmException ex) {
			logger.error("Failed to create MAC engine", ex);
		}
	}

	public void setAcceptorSSLTLS(AcceptorSSLTLS acceptor) {
		this.acceptor = acceptor;
	}
	
	public TreeManager getTreeManager() {
		return this.tm;
	}
	public void setTreeManager(TreeManager tm) {
		//logger.warn(" TREE MANAGER DEFINED....: " + tm.toString());
		this.tm = tm;
	}

	public void setTOMLayer(TOMLayer tomLayer) {
		this.tomLayer = tomLayer;
	}

	@SuppressWarnings("unchecked")
	protected void processData(SystemMessage sm) {
		if (sm instanceof ConsensusMessage) {

			int myId = tomLayer.controller.getStaticConf().getProcessId();

			ConsensusMessage consMsg = (ConsensusMessage) sm;

			// If using SSL / TLS, the MAC will be turned off (TLS protocols already does),
			// so the else is unnecessary with SSL/TLS.
			if (tomLayer.controller.getStaticConf().getUseMACs() == false 
					|| consMsg.authenticated
					|| consMsg.getSender() == myId) {				
				acceptor.deliver(consMsg);
			}
			else if (consMsg.getType() == MessageFactory.ACCEPT 
					&& consMsg.getProof() != null) {
				
				// We are going to verify the MAC vector at the algorithm level
				HashMap<Integer, byte[]> macVector = (HashMap<Integer, byte[]>) consMsg.getProof();

				byte[] recvMAC = macVector.get(myId);

				ConsensusMessage cm = new ConsensusMessage(MessageFactory.ACCEPT, consMsg.getNumber(),
						consMsg.getEpoch(), consMsg.getSender(), consMsg.getValue());

				ByteArrayOutputStream bOut = new ByteArrayOutputStream(248);
				try {
					new ObjectOutputStream(bOut).writeObject(cm);
				} catch (IOException ex) {
					logger.error("Failed to serialize consensus message", ex);
				}

				byte[] data = bOut.toByteArray();

				byte[] myMAC = null;

				try {
					// this.mac.init(key);
					this.mac.init(tomLayer.getCommunication().getServersConnSSLTLS().getSecretKey(consMsg.getSender()));
					myMAC = this.mac.doFinal(data);
				} catch (InvalidKeyException ex) {
					logger.error("Failed to generate MAC", ex);
				}

				if (recvMAC != null && myMAC != null && Arrays.equals(recvMAC, myMAC))
					acceptor.deliver(consMsg);
				else {
					logger.warn("Invalid MAC from " + sm.getSender());
				}
			} else {
				logger.warn("Discarding unauthenticated message from " + sm.getSender());
			}

		} else {
			if (tomLayer.controller.getStaticConf().getUseMACs() == false || sm.authenticated) {
				/*** This is Joao's code, related to leader change */
				if (sm instanceof LCMessage) {
					LCMessage lcMsg = (LCMessage) sm;

					String type = null;
					switch (lcMsg.getType()) {

					case TOMUtil.STOP:
						type = "STOP";
						break;
					case TOMUtil.STOPDATA:
						type = "STOPDATA";
						break;
					case TOMUtil.SYNC:
						type = "SYNC";
						break;
					default:
						type = "LOCAL";
						break;
					}

					if (lcMsg.getReg() != -1 && lcMsg.getSender() != -1)
						logger.info("Received leader change message of type {} for regency {} from replica {}", type,
								lcMsg.getReg(), lcMsg.getSender());
					else
						logger.debug("Received leader change message from myself");

					if (lcMsg.TRIGGER_LC_LOCALLY)
						tomLayer.requestsTimer.run_lc_protocol();
					else
						tomLayer.getSynchronizerSSLTLS().deliverTimeoutRequest(lcMsg);
					/**************************************************************/

				} else if (sm instanceof ForwardedMessage) {
					TOMMessage request = ((ForwardedMessage) sm).getRequest();
					tomLayer.requestReceived(request);

					/** This is Joao's code, to handle state transfer */
				} else if (sm instanceof SMMessage) {
					SMMessage smsg = (SMMessage) sm;
					// System.out.println("(MessageHandler.processData) SM_MSG received: type " +
					// smsg.getType() + ", regency " + smsg.getRegency() + ", (replica " +
					// smsg.getSender() + ")");
					switch (smsg.getType()) {
					case TOMUtil.SM_REQUEST:
						tomLayer.getStateManager().SMRequestDeliver(smsg, tomLayer.controller.getStaticConf().isBFT());
						break;
					case TOMUtil.SM_REPLY:
						tomLayer.getStateManager().SMReplyDeliver(smsg, tomLayer.controller.getStaticConf().isBFT());
						break;
					case TOMUtil.SM_ASK_INITIAL:
						tomLayer.getStateManager().currentConsensusIdAsked(smsg.getSender());
						break;
					case TOMUtil.SM_REPLY_INITIAL:
						tomLayer.getStateManager().currentConsensusIdReceived(smsg);
						break;
					default:
						tomLayer.getStateManager().stateTimeout();
						break;
					}
					/******************************************************************/
				} else if(sm instanceof TreeMessage){
					TreeMessage treeM = (TreeMessage) sm;
					switch (treeM.getTreeOperationType()) {
					case INIT:
						//logger.warn("" + this.tm.toString());
						this.tm.initProtocol();
					break;
					case M:
						//logger.warn("Received TreeMessage M from: {}", treeM.getSender());
						this.tm.receivedM(treeM);
					break;
					case ALREADY:
						//logger.warn("Received TreeMessage ALREADY from: {}", treeM.getSender());
						this.tm.receivedAlready(treeM);
					break;
					case PARENT:
						//logger.warn("Received TreeMessage PARENT from: {}", treeM.getSender());
						this.tm.receivedParent(treeM);
					break;
					case FINISHED:
						//logger.warn("Received TreeMessage PARENT from: {}", treeM.getSender());
						this.tm.receivedFinished(treeM);
					break;
					case RECONFIG:
						logger.warn("Received TreeMessage RECONFIG");
					break;							
					case STATIC_TREE:
						logger.warn("Received TreeMessage STATIC_TREE");
						this.tm.createStaticTree();
						logger.warn("{}" , this.tm.toString());
					break;
					default:
						logger.warn("TreeMessage NOOP.");
						break;
					}
				}
				else {
					logger.warn("UNKNOWN MESSAGE TYPE: " + sm);
				}
			} else {
				logger.warn("Discarding unauthenticated message from " + sm.getSender());
			}
		}
	}

	protected void verifyPending() {
		tomLayer.processOutOfContext();
	}
}
