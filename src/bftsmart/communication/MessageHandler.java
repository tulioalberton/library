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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bftsmart.consensus.messages.ConsensusMessage;
import bftsmart.consensus.roles.Acceptor;
import bftsmart.statemanagement.SMMessage;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.ForwardedMessage;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.leaderchange.LCMessage;
import bftsmart.tom.util.TOMUtil;
import bftsmart.tree.MultiRootedSP;
import bftsmart.tree.messages.ForwardTree;
import bftsmart.tree.messages.TreeMessage;

/**
 *
 * @author edualchieri, Tulio Ribeiro
 */
public class MessageHandler {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private Acceptor acceptor;
	private TOMLayer tomLayer;
	private MultiRootedSP mrSP;

	public MessageHandler() {
	}

	public void setAcceptor(Acceptor acceptor) {
		this.acceptor = acceptor;
	}
	
	public MultiRootedSP getMultiRootedSP() {
		return this.mrSP;
	}
	public void setMultiRootedSP(MultiRootedSP mrSP) {
		this.mrSP = mrSP;
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
			if (consMsg.authenticated
					|| consMsg.getSender() == myId) {				
				acceptor.deliver(consMsg);
			}
			else {
				logger.warn("Discarding unauthenticated message from " + sm.getSender());
			}

		} else if(sm instanceof ForwardTree) {
        	ForwardTree fwd = (ForwardTree) sm;
        	
        	ConsensusMessage consMsg = fwd.getConsensusMessage();        	
        	consMsg.authenticated = true;
        	logger.info("### Catched a ForwardTreeMessage, "
        			+ "direction:{}, "
        			+ "originator:{}, "
        			+ "from:{}, "
        			+ "with a CM message: {}", 
        			new Object[] {fwd.getDirection(), 
        					consMsg.getSender(),
        					fwd.getSender(), 
        					consMsg.getType()		
        			});
        	
        	this.mrSP.forwardTreeMessage(fwd);
        	
        	/**
        	 * TESTE ONLY
        	 */
        	int myId = tomLayer.controller.getStaticConf().getProcessId();
			
			// If using SSL / TLS, the MAC will be turned off (TLS protocols already does),
			// so the else is unnecessary with SSL/TLS.
			if (consMsg.authenticated
					|| consMsg.getSender() == myId) {
				acceptor.deliver(consMsg);
			}else {
				logger.warn("Discarding unauthenticated message from:{},"
						+ " ForwardedMessage... " , sm.getSender());
			}
        	/**
        	 * END
        	 */
        	
        	
        } else {
			if (sm.authenticated) {
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
						tomLayer.getSynchronizer().deliverTimeoutRequest(lcMsg);
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
					this.mrSP.treatMessages(treeM);
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
