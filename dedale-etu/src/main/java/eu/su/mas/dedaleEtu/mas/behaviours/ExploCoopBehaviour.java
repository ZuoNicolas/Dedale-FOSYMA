package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;


import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


/**
 * <pre>
 * This behaviour allows an agent to explore the environment and learn the associated topological map.
 * The algorithm is a pseudo - DFS computationally consuming because its not optimised at all.
 * 
 * When all the nodes around him are visited, the agent randomly select an open node and go there to restart its dfs. 
 * This (non optimal) behaviour is done until all nodes are explored. 
 * 
 * Warning, this behaviour does not save the content of visited nodes, only the topology.
 * Warning, the sub-behaviour ShareMap periodically share the whole map
 * </pre>
 * @author hc
 *
 */
public class ExploCoopBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;
	
	private boolean SuccesMove = true;

/**
 * 
 * @param myagent
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public ExploCoopBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap) {
		super(myagent);
		this.myMap=myMap;
		
	}

	@Override
	public void action() {
		final MessageTemplate msgTemplate_endMerge = MessageTemplate.and(MessageTemplate.MatchProtocol("re-move"), 
				MessageTemplate.MatchPerformative(ACLMessage.INFORM) );	

		final ACLMessage msg_endMerge = this.myAgent.receive(msgTemplate_endMerge);
		
		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocolePoke"), 
				MessageTemplate.MatchPerformative(ACLMessage.INFORM) );	

		final ACLMessage msg = this.myAgent.receive(msgTemplate);

		//If receive a message, don't move
		if (msg != null) {
			((ExploreCoopAgent)this.myAgent).move = false;
			msg.getContent();
			System.out.println(this.myAgent.getLocalName() + " --> Stop move");
			this.myAgent.postMessage(msg);
			block();
		}
		
		if( msg_endMerge != null) {
			((ExploreCoopAgent)this.myAgent).move = true;
			System.out.println(this.myAgent.getLocalName() + " --> Start move");
		}
		
		//If no msg and agent can move + msg_endMerge(si il y a eu echange)
		if(((ExploreCoopAgent)this.myAgent).move) {

			
			if(this.myMap==null) {
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
					}else {
						System.out.println("nextNode notNUll - "+this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"\n -- nextNode: "+nextNode + " actual node :"+myPosition);
					}
					//4) At each time step, the agent blindly send all its graph to its surrounding to illustrate how to share its knowledge (the topology currently) with the the others agents. 	
					// If it was written properly, this sharing action should be in a dedicated behaviour set, the receivers be automatically computed, and only a subgraph would be shared.
	
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
