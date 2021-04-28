package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class DumbChaseBehaviour extends OneShotBehaviour{
	/**
	 * 
	 */
	private static final long serialVersionUID = 267531971032817028L;

	private MapRepresentation myMap;
	
	private int exitValue;
	
	private String nodeGoal = "";
	
	private int timer, nb_move_fail, max_move_fail=10;
	
	private String oldNode="";
	
	private boolean firstTime=true;

	private List<String> temp;
	
/**
 * 
 * @param myagent
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public DumbChaseBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, int timer) {
		super(myagent);
		this.myMap=((fsmAgent)this.myAgent).getMap();
		this.timer=timer;
	}

	@Override
	public void action() {
		this.exitValue = 0;
		
		if (firstTime) {
			firstTime=false;
			System.out.println(this.myAgent.getLocalName()+" Start Chase !");
		}
		
		if(checkMsg()) {
			return ;
		}

		
		this.myMap = ((fsmAgent)this.myAgent).getMap();
		
		//If no msg and agent can move
		if(((fsmAgent)this.myAgent).move) {

			//0) Retrieve the current position
			String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
			
			if (myPosition.equals(((fsmAgent)this.myAgent).moveTo)) {
				((fsmAgent)this.myAgent).moveTo = null;
			}
	
			if (myPosition!=null){
				//List of observable from the agent's current position
				List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
	
				/**
				 * Just added here to let you see what the agent is doing, otherwise he will be too quick
				 */
				try {
					this.myAgent.doWait(500);
				} catch (Exception e) {
					e.printStackTrace();
				}
	
				//1) remove the current node from openlist and add it to closedNodes.
				this.myMap.addNode(myPosition, MapAttribute.closed);
				
				//2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
				String nextNode=null;
				Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
				while(iter.hasNext()){
					Couple<String, List<Couple<Observation, Integer>>> node = iter.next();
					String nodeId= node.getLeft();
					List<Couple<Observation, Integer>> list = node.getRight();
					//the node may exist, but not necessarily the edge
					if (myPosition!=nodeId && nodeId!=oldNode && list.size()>0 ) {
						if (nextNode==null && !((fsmAgent)this.myAgent).GolemPoop.contains(nextNode)) nextNode=nodeId;
					}
				}

				//4) select next move.
				//4.1 If there exist one open node directly reachable, go for it,
				//	 otherwise choose one from the openNode list, compute the shortestPath and go for it
				if (((fsmAgent)this.myAgent).moveTo != null) {
					List<String> chemin = myMap.getShortestPath(myPosition, ((fsmAgent)this.myAgent).moveTo, ((fsmAgent)this.myAgent).blockedAgent);
					/*for(String c: chemin) {
						System.out.println(this.myAgent.getLocalName()+" ---> Move "+c);
					}*/
					if(chemin != null) {
						if(chemin.size()>0) {
							nextNode = chemin.get(0);
						}
					}	
				}
				int lastExit = ((fsmAgent)this.myAgent).getFSM().getLastExitValue();

				if (nextNode==null || lastExit == 1 || ((fsmAgent)this.myAgent).forceChangeNode){
					((fsmAgent)this.myAgent).forceChangeNode = false;
					while(nodeGoal.equals("") || myPosition.equals(nodeGoal)) {
						List<String> closednodes=this.myMap.getClosedNodes();
						Random rand = new Random();
						nodeGoal = closednodes.get(rand.nextInt(closednodes.size()));
						System.out.println(this.myAgent.getLocalName()+" ---> Init a new nodeGoal("+nodeGoal+") to search Golem");
					}
					temp = this.myMap.getShortestPath(myPosition, nodeGoal, ((fsmAgent)this.myAgent).blockedAgent);
					if (temp != null) {
						if( temp.size()>0 ) {
							nextNode = this.myMap.getShortestPath(myPosition, nodeGoal, ((fsmAgent)this.myAgent).blockedAgent).get(0);
						}else {
							return; //reset
						}
					}else {
						return ;//reset
					}
					if(nextNode.equals(nodeGoal)) {
						nodeGoal = "";
						System.out.println(this.myAgent.getLocalName()+" ---> Arrived to nodeGoal("+nodeGoal+")");
					}

				}else {
					//System.out.println(this.myAgent.getLocalName()+" ---> Find a Golem's poop");
				}

				((fsmAgent)this.myAgent).nextNode=nextNode;
				((fsmAgent)this.myAgent).updateMap(this.myMap);
				((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);
			}
			
			
			if (oldNode.equals(myPosition)) {
				if ( nb_move_fail >= max_move_fail) {
					nb_move_fail = 0;
					System.out.println(this.myAgent.getLocalName() + " --> Something block me ! (stop move)");
					this.exitValue = 3;
					return ;
				}
				nb_move_fail++;
				//System.out.println(this.myAgent.getLocalName()+" : nb_move = "+nb_move_fail);
			}else {
				oldNode = myPosition;
				nb_move_fail = 0;
			}
		}
	}
	
		
	public boolean checkMsg() {
		
		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocolePoke"), 
				MessageTemplate.MatchPerformative(ACLMessage.INFORM) );	
		final ACLMessage msg = this.myAgent.receive(msgTemplate);

		//If receive a message, don't move
		if (msg != null) {
			System.out.println(this.myAgent.getLocalName() + " --> Receive a poke (stop move)");
			this.exitValue = 4;//Go to share map
			return false;
		}
		
    	final MessageTemplate msgTemplateBlock = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleHelpBlockWumpus"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));	
    	final ACLMessage msgBlock = this.myAgent.receive(msgTemplateBlock);
    	
    	if (msgBlock != null) {
			System.out.println(this.myAgent.getLocalName() + " --> Receive a help bloc Wumpus protocole");
			this.exitValue = 5;//Go to help block
			return true;
    	}
    	
    	final MessageTemplate msgSomeone = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleSomeone"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));	
    	final ACLMessage msgS = this.myAgent.receive(msgSomeone);
    	
    	if (msgS != null) {
    		String m =msgS.getContent();
    		if ( m.equals(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
        		System.out.println(this.myAgent.getLocalName() + " --> Receive a check Someone msg");
        		((fsmAgent)this.myAgent).agentToContact = msgS.getSender().getLocalName();
        		exitValue = 2;
        		return true;
    		}
    	}
    	
		return false;
	}
	
	@Override
	public int onEnd() {
		return exitValue;
	}

}
