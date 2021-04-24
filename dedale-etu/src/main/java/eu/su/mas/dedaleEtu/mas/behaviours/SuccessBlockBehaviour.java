package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SuccessBlockBehaviour extends OneShotBehaviour {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4318104060489928048L;

	
	public SuccessBlockBehaviour(Agent a) {
		super(a);
	}

	@Override
	public void action() {
		
		final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);	
		final ACLMessage msg = this.myAgent.receive(msgTemplate);

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
		}
		
		block();
	}

}
