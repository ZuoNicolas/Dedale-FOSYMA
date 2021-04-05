package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


public class ReceiveMapBehaviour extends SimpleBehaviour{

	private static final long serialVersionUID = 95283913118770598L;
	
	private boolean finished=false;
	
	private MapRepresentation myMap;

	/**
	 * 
	 * This behaviour is a one Shot.
	 * It receives a message tagged with an inform performative, print the content in the console and destroy itlself
	 * @param myagent
	 */
	public ReceiveMapBehaviour(final Agent myagent) {
		super(myagent);
	}


	@SuppressWarnings("unchecked")
	public void action() {
		//1) receive the message

		MessageTemplate msgTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("ProtocoleShareMap"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		ACLMessage msg=this.myAgent.receive(msgTemplate);


		if (msg != null) {
			this.myMap = ((ExploreCoopAgent)this.myAgent).getMap();
			System.out.println(this.myAgent.getLocalName()+"<----Map received from "+msg.getSender().getLocalName());

			SerializableSimpleGraph<String, MapAttribute> sgreceived=null;
			try {
				sgreceived = (SerializableSimpleGraph<String, MapAttribute>)msg.getContentObject();
			} catch (UnreadableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.myMap.mergeMap(sgreceived);
			
			((ExploreCoopAgent)this.myAgent).updateMap(this.myMap);
			System.out.println(this.myAgent.getLocalName()+"<---End Map merge");
			
			//Signaler sois-meme pour recommencer a bouger
			ACLMessage send_msg=new ACLMessage(ACLMessage.INFORM);
			send_msg.setProtocol("re-move");
			send_msg.setSender(this.myAgent.getAID());
			send_msg.setContent("End Merge");

			this.myAgent.postMessage(send_msg);
			
			//maj variable
			((ExploreCoopAgent)this.myAgent).move=true;
		}else{
			block();// the behaviour goes to sleep until the arrival of a new message in the agent's Inbox.
		}

	}
	public boolean done() {
		return finished;
	}


}