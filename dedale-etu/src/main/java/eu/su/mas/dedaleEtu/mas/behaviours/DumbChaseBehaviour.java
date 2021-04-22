package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.Iterator;
import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class DumbChaseBehaviour extends SimpleBehaviour{
	/**
	 * 
	 */
	private static final long serialVersionUID = 267531971032817028L;

	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;
	private String oldNode ="";
/**
 * 
 * @param myagent
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public DumbChaseBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap) {
		super(myagent);
		this.myMap=myMap;
		
	}

	@Override
	public void action() {
		
		//If no msg and agent can move + msg_endMerge(si il y a eu echange)
		if(((ExploreCoopAgent)this.myAgent).move) {
			
			if(this.myMap==null) {
				System.out.println("Erreur création d'une nouvelle map dans DumbChaseBehaviour");
				this.myMap= new MapRepresentation();
			}
			else {
				this.myMap = ((ExploreCoopAgent)this.myAgent).getMap();
			}
			
	
			//0) Retrieve the current position
			String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
	
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
					String nodeId=iter.next().getLeft();
					boolean isNewNode=this.myMap.addNewNode(nodeId);
					//the node may exist, but not necessarily the edge
					if (myPosition!=nodeId) {
						this.myMap.addEdge(myPosition, nodeId);
						if (nextNode==null && isNewNode) nextNode=nodeId;
					}
				}
	
				//3) while openNodes is not empty, continues.
				if (!this.myMap.hasOpenNode()){
					//Explo finished
					
					List<Behaviour> lb = ((ExploreCoopAgent)this.myAgent).getLB();
				    for (Behaviour b : lb) {
				    	if (! b.getBehaviourName().equals("ExploCoopBehaviour")) {
				    		System.out.println(b.getBehaviourName());
				    		this.myAgent.removeBehaviour(b);
				    	}
				      }
					finished=true;
					System.out.println(this.myAgent.getLocalName()+" - Exploration successufully done, behaviour removed.");
				}else{
					//4) select next move.
					//4.1 If there exist one open node directly reachable, go for it,
					//	 otherwise choose one from the openNode list, compute the shortestPath and go for it
					
					if (nextNode==null){
						//no directly accessible openNode
						//chose one, compute the path and take the first step.
						nextNode=this.myMap.getShortestPathToClosestOpenNode(myPosition).get(0);//getShortestPath(myPosition,this.openNodes.get(0)).get(0);
						System.out.println(this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"| nextNode: "+nextNode+ " actual node :"+myPosition);
						((ExploreCoopAgent)this.myAgent).nextNode = nextNode;
					}else {
						System.out.println("nextNode notNUll - "+this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"\n -- nextNode: "+nextNode + " actual node :"+myPosition);
					}
					//4) At each time step, the agent blindly send all its graph to its surrounding to illustrate how to share its knowledge (the topology currently) with the the others agents. 	
					// If it was written properly, this sharing action should be in a dedicated behaviour set, the receivers be automatically computed, and only a subgraph would be shared.
					if (((ExploreCoopAgent)this.myAgent).changeNode) {
						((ExploreCoopAgent)this.myAgent).changeNode = false;
						nextNode = ((ExploreCoopAgent)this.myAgent).nextNode = nextNode;
					}
	//				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
	//				msg.setProtocol("SHARE-TOPO");
	//				msg.setSender(this.myAgent.getAID());
	//				if (this.myAgent.getLocalName().equals("1stAgent")) {
	//					msg.addReceiver(new AID("2ndAgent",false));
	//				}else {
	//					msg.addReceiver(new AID("1stAgent",false));
	//				}
	//				SerializableSimpleGraph<String, MapAttribute> sg=this.myMap.getSerializableGraph();
	//				try {					
	//					msg.setContentObject(sg);
	//				} catch (IOException e) {
	//					e.printStackTrace();
	//				}
	//				((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
	
					((ExploreCoopAgent)this.myAgent).updateMap(this.myMap);
					((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);	
				}
	
			}
		}
	}

	@Override
	public boolean done() {
		return finished;
	}
}