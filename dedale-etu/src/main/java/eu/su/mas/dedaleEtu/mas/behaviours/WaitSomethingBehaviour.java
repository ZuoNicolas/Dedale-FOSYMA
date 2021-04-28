package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class WaitSomethingBehaviour extends OneShotBehaviour {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4468820870958544230L;

	private MapRepresentation myMap;
	
	private boolean haveMsg=true;

	private int exitValue;
	
	@Override
	public void action() {
		//1) receive the message
		
		this.exitValue=0;
		
		MessageTemplate msgTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("ProtocoleWaitSomeone"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		ACLMessage msg=this.myAgent.receive(msgTemplate);
		
		if (msg != null) {
			String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
			//Check if the Sender want to come on my position or not
			if(myPosition.equals(msg.getContent())) {
				this.myMap = ((fsmAgent)this.myAgent).getMap();
				System.out.println(this.myAgent.getLocalName()+" <--- The Someone is "+msg.getSender().getLocalName()+", back to chase");
				((fsmAgent)this.myAgent).GolemPoop.add(((fsmAgent)this.myAgent).nextNode);
				((fsmAgent)this.myAgent).blockedAgent.add(((fsmAgent)this.myAgent).nextNode);
				this.exitValue=1;
			}
		}else{
			if ( !haveMsg ) {
				haveMsg=true;
				this.exitValue=2;
				((fsmAgent)this.myAgent).succesMerge = false;
				System.out.println(this.myAgent.getLocalName()+" ---> No respond, it's probably a Wumpus");
				return ;
			}
			this.haveMsg=false;
			block(5000);// the behaviour goes to sleep until the arrival of a new message in the agent's Inbox.
		}

	}
	
	@Override
	public int onEnd() {
		return exitValue;
	}

}
