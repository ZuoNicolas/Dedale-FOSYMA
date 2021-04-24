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
		final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);	
		final ACLMessage msg = this.myAgent.receive(msgTemplate);
		SuccessMove = false;

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
