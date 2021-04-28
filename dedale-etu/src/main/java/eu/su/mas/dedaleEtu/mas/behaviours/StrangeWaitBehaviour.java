package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class StrangeWaitBehaviour extends OneShotBehaviour {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1147419770546269281L;
	
	private boolean SuccessMove;

	private int exitValue;

	public StrangeWaitBehaviour(Agent a) {
		super(a);
	}
	@Override
	public void action() {
		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocolePoke"), 
				MessageTemplate.MatchPerformative(ACLMessage.INFORM) );	
		final ACLMessage msg = this.myAgent.receive(msgTemplate);
		
		SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(((fsmAgent)this.myAgent).nextNode);
		
		if(SuccessMove) {
			System.out.println(this.myAgent.getLocalName() + " --> The Wumpus is not blocked ! (StrangeWaitBehaviour)");
			exitValue = 1;
			return ;
		}else {
			System.out.println(this.myAgent.getLocalName() + " --> The potential Wumpus still probably blocked ! (StrangeWaitBehaviour) "+((fsmAgent)this.myAgent).nextNode);
		}
		
		//If receive a message, don't move
		if (msg != null) {

			System.out.println(this.myAgent.getLocalName() + " --> Receive msg, I am not a Golem bypass me");
			
			ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
			sendMsg.setProtocol("ProtocoleByPass");
			sendMsg.setSender(this.myAgent.getAID());
			sendMsg.setContent(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());

			sendMsg.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));

			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
			((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
		}else {
			ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
			sendMsg.setProtocol("ProtocoleByPass");
			sendMsg.setSender(this.myAgent.getAID());
			sendMsg.setContent(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());

			for(String n: ((fsmAgent)this.myAgent).getList_AgentNames())
			sendMsg.addReceiver(new AID(n,AID.ISLOCALNAME));
			
			((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
		}
		
		

		
		block(5000);
		
	}

	@Override
	public int onEnd() {
		return exitValue;
	}
}
