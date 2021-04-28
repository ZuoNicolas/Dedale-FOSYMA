package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.graphstream.graph.Node;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class NeedHelpBehaviour extends OneShotBehaviour {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8780845722125588676L;
	
	private String myPosition;
	private String nextNode;
	private MapRepresentation myMap;
	private List<String> NodeToBlock, News, agentToContact = ((fsmAgent) this.myAgent).getList_AgentNames();
	private int exitValue;

	private boolean SuccessMove, check, size;
	
	public NeedHelpBehaviour(Agent a) {
		super(a);
	}
	
	@Override
	public void action() {
		myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		nextNode = ((fsmAgent)this.myAgent).nextNode;
		myMap = ((fsmAgent)this.myAgent).getMap();
		NodeToBlock = ((fsmAgent)this.myAgent).NodeToBlock;
		size = false;
		
        if(NodeToBlock.size() == 0) {
        	System.out.println(this.myAgent.getLocalName()+ " ---> All NodeToBlock is blocked !");
        	exitValue = 1;
        	return ;
        }else {
        	final MessageTemplate msgTemplate2 = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleHelpBlockWumpus"),
        														MessageTemplate.MatchPerformative(ACLMessage.INFORM));	
    		final ACLMessage msgBlock = this.myAgent.receive(msgTemplate2);
    		
    		if(msgBlock != null) {
    			try {
					News = (List<String>) msgBlock.getContentObject();
					String error = News.get(0);
					News.remove(0);
					if (error.equals(((fsmAgent) this.myAgent).nextNode)){
		    			System.out.println(this.myAgent.getLocalName() + " --> Wrong ! It's not a golem at "+error);
						NodeToBlock = News;
						((fsmAgent)this.myAgent).NodeToBlock = News;
						((fsmAgent)this.myAgent).blockedAgent.add(error);
						((fsmAgent)this.myAgent).moveTo = ((fsmAgent)this.myAgent).NodeToBlock.get(0);
						((fsmAgent)this.myAgent).NodeToBlock.remove(0);
						exitValue = 2;
						return ;
					}else {
		    			System.out.println(this.myAgent.getLocalName() + " --> Receive serialized msg ");
		    			check = true;
		    			for(String n:News) {
		    				System.out.println(this.myAgent.getLocalName() + " -1-> "+n);
		    				if (!NodeToBlock.contains(n)) {
		    					check = false;
		    				}
		    			}
		    			News.remove(myPosition);
		    			System.out.println(this.myAgent.getLocalName() + " --> Remove my pos "+myPosition);
					}
					
	    			size = NodeToBlock.size() > News.size();
	    			if(check && size) {
	    				NodeToBlock = News;
	    				((fsmAgent)this.myAgent).NodeToBlock = NodeToBlock ;
	    			}else {
	    				size = false;
	    			}
	    			
				} catch (UnreadableException e) {
					e.printStackTrace();
				}

    		}
    		
    		final MessageTemplate msgTemplateB =  MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleByPass"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));		
    		final ACLMessage msgB = this.myAgent.receive(msgTemplateB);
    		
    		if (msgB != null) {
    			boolean error2 = NodeToBlock.remove(msgB.getContent());
    			
    			if ( error2 ) {
        			agentToContact.remove(msgB.getSender().getLocalName());
        			System.out.println(this.myAgent.getLocalName() + " --> Remove to NodeToBlock "+msgB.getContent());
        			
        	        if(NodeToBlock.size() == 0) {
        	        	System.out.println(this.myAgent.getLocalName()+ " ---> All NodeToBlock is blocked !");
        	        	exitValue = 1;
        	        	return ;
        	        }
    			}else {
    				((fsmAgent)this.myAgent).blockedAgent.add(msgB.getContent());
    	        	exitValue = 2;
    	        	return ;
    			}

    	        
    		}
    		
    		final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);	
    		final ACLMessage msg = this.myAgent.receive(msgTemplate);
    		
			
			((fsmAgent)this.myAgent).NodeToBlock = NodeToBlock;
    		//If receive a message, don't move
    		if (msg != null || size ) {
    			System.out.println(this.myAgent.getLocalName() + " --> Receive msg from "+msg.getSender().getLocalName());
    			System.out.print(this.myAgent.getLocalName() + " --> Need to block ");
    			for(String n: NodeToBlock) {
    				System.out.print(n+" ");
    			}
    			System.out.println();
    			NodeToBlock.add(0, myPosition);
    			ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
    			sendMsg.setProtocol("ProtocoleHelpBlockWumpus");
    			sendMsg.setSender(this.myAgent.getAID());
    			
    			try {
					sendMsg.setContentObject((Serializable) NodeToBlock);
				} catch (IOException e) {
					e.printStackTrace();
				}

    			sendMsg.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
    			
    			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
    			((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
    			NodeToBlock.remove(0);
    		}else {
        			
    			System.out.println(this.myAgent.getLocalName() + " --> Send a Help to block Wumpus msg");
    			System.out.print(this.myAgent.getLocalName() + " --> Need to block ");
    			for(String n: NodeToBlock) {
    				System.out.print(n+" ");
    			}
    			System.out.println();
    			NodeToBlock.add(0, myPosition);
    			ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
    			sendMsg.setProtocol("ProtocoleHelpBlockWumpus");
    			sendMsg.setSender(this.myAgent.getAID());
    			
    			try {
					sendMsg.setContentObject((Serializable) NodeToBlock);
				} catch (IOException e) {
					e.printStackTrace();
				}

    			for (String agentName : agentToContact) {
    				sendMsg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
    			}
    			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
    			((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
    			NodeToBlock.remove(0);
        		
    		}
        }
        
		SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(((fsmAgent)this.myAgent).nextNode);
		
		if(SuccessMove) {
			System.out.println(this.myAgent.getLocalName() + " --> The Wumpus is not blocked ! (NeedHelpBehaviour)");
			exitValue = 2;
			return ;
		}else {
			System.out.println(this.myAgent.getLocalName() + " --> The potential Wumpus still probably blocked ! (NeedHelpBehaviour) "+((fsmAgent)this.myAgent).nextNode);
		}
        block(5000);
	}
	
	@Override
	public int onEnd() {
		return exitValue;
	}
}

