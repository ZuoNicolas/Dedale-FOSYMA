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
	private List<Node> NodeToBlock, New;
	private int exitValue;

	private boolean SuccessMove;
	
	public NeedHelpBehaviour(Agent a) {
		super(a);
	}
	
	@Override
	public void action() {
		myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		nextNode = ((fsmAgent)this.myAgent).nextNode;
		myMap = ((fsmAgent)this.myAgent).getMap();
		NodeToBlock = ((fsmAgent)this.myAgent).NodeToBlock;
		
        if(NodeToBlock.size() == 0) {
        	System.out.println(this.myAgent.getLocalName()+ " ---> All NodeToBlock is blocked !");
        	exitValue = 1;
        }else {
        	final MessageTemplate msgTemplate2 = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleHelpBlockWumpus"),
        														MessageTemplate.MatchPerformative(ACLMessage.INFORM));	
    		final ACLMessage msgBlock = this.myAgent.receive(msgTemplate2);
    		
    		if(msgBlock != null) {
    			try {
					Serializable New = msgBlock.getContentObject();
	    			System.out.println(this.myAgent.getLocalName() + " --> Receive serialized msg "+New);
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
    			//Non finit *****************************************
    		}
    		
    		final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);	
    		final ACLMessage msg = this.myAgent.receive(msgTemplate);

    		//If receive a message, don't move
    		//if (msg != null) {
    			System.out.println(this.myAgent.getLocalName() + " --> Receive msg, Send a Help to block Wumpus msg");
    			
    			ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
    			sendMsg.setProtocol("ProtocoleHelpBlockWumpus");
    			sendMsg.setSender(this.myAgent.getAID());
    			
    			try {
					sendMsg.setContentObject((Serializable) NodeToBlock);
				} catch (IOException e) {
					e.printStackTrace();
				}

    			//sendMsg.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
    			((AbstractDedaleAgent)this.myAgent).postMessage(sendMsg);
    			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
    			//((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
    		//}
        }
        
		SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(((fsmAgent)this.myAgent).nextNode);
		
		if(SuccessMove) {
			System.out.println(this.myAgent.getLocalName() + " --> The Wumpus is not blocked !");
			exitValue = 1;
			return ;
		}else {
			System.out.println(this.myAgent.getLocalName() + " --> The potential Wumpus still probably blocked !");
		}
        block(5000);
	}
	
	@Override
	public int onEnd() {
		return exitValue;
	}
}

