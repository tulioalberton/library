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
package bftsmart.communication.client.netty;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bftsmart.reconfiguration.ClientViewController;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

public class NettyClientPipelineFactory {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	NettyClientServerCommunicationSystemClientSide ncs;
	ConcurrentHashMap<Integer, NettyClientServerSession> sessionTable;

	// ******* EDUARDO BEGIN **************//
	ClientViewController controller;
	// ******* EDUARDO END **************//

	ReentrantReadWriteLock rl;

	public NettyClientPipelineFactory(NettyClientServerCommunicationSystemClientSide ncs,
			ConcurrentHashMap<Integer, NettyClientServerSession> sessionTable, ClientViewController controller,
			ReentrantReadWriteLock rl) {
		this.ncs = ncs;
		this.sessionTable = sessionTable;

		this.rl = rl;
		this.controller = controller;

	}

	public ByteToMessageDecoder getDecoder() {
		return new NettyTOMMessageDecoder(true, sessionTable, controller, rl);
	}

	public MessageToByteEncoder getEncoder() {
		return new NettyTOMMessageEncoder(true, sessionTable, rl);
	}

	public SimpleChannelInboundHandler getHandler() {
		return ncs;
	}
}
