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
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;


import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
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
public class ExploCoopBehaviour extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;
	
	private int exitValue;
	
	private String nodeGoal = "";
	
	private int timer, start, now, nb_move_fail, max_move_fail=100;
	
	private String oldNode="";

/**
 * 
 * @param myagent
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public ExploCoopBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, int timer) {
		super(myagent);
		this.myMap=((fsmAgent)this.myAgent).getMap();
		this.timer=timer;
		this.start = 0;
	}

	@Override
	public void action() {
		this.exitValue = 0;
		
		if(checkMsg() || checkTimer()) {
			return ;
		}
		
		
		
		this.myMap = ((fsmAgent)this.myAgent).getMap();
		
		//If no msg and agent can move
		if(((fsmAgent)this.myAgent).move && this.myMap!=null) {

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
					Couple<String, List<Couple<Observation, Integer>>> node = iter.next();
					String nodeId= node.getLeft();
					List<Couple<Observation, Integer>> list = node.getRight();
					boolean isNewNode=this.myMap.addNewNode(nodeId);
					//the node may exist, but not necessarily the edge
			        for(Couple<Observation, Integer> model : list) {
			            //System.out.print("Observation "+model.getLeft()+ " Integer "+model.getRight());
			        }
					//System.out.println(this.myAgent.getLocalName()+" nodeID "+nodeId);
					if (myPosition!=nodeId) {
						this.myMap.addEdge(myPosition, nodeId);
						if (nextNode==null && isNewNode) nextNode=nodeId;
					}
				}
	
				//3) while openNodes is not empty, continues.
				if (!this.myMap.hasOpenNode()){
					//Explo finished
					this.exitValue = 3;//End of exploration
					System.out.println(this.myAgent.getLocalName()+" - Exploration successufully done, behaviour removed.");
				}else{
					//4) select next move.
					//4.1 If there exist one open node directly reachable, go for it,
					//	 otherwise choose one from the openNode list, compute the shortestPath and go for it
					
					if (nextNode==null){
						if (((fsmAgent)this.myAgent).succesMerge) {
							if (nodeGoal.equals("")) {
								System.out.println(this.myAgent.getLocalName()+ "------first");
								List<String> opennodes=this.myMap.getOpenNodes();
								Random rand = new Random();
								nodeGoal = opennodes.get(rand.nextInt(opennodes.size()));
							}
							nextNode = this.myMap.getShortestPath(myPosition, nodeGoal).get(0);
							System.out.println(this.myAgent.getLocalName()+ "apres");
							if(nextNode.equals(nodeGoal)) {
								System.out.println(this.myAgent.getLocalName()+ "end");
								((fsmAgent)this.myAgent).succesMerge = false;
								nodeGoal = "";
							}
						}
						else {
						//no directly accessible openNode
						//chose one, compute the path and take the first step.
						nextNode=this.myMap.getShortestPathToClosestOpenNode(myPosition).get(0);//getShortestPath(myPosition,this.openNodes.get(0)).get(0);
						}
						//System.out.println(this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"| nextNode: "+nextNode+ " actual node :"+myPosition);
					}else {
						//System.out.println("nextNode notNUll - "+this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"\n -- nextNode: "+nextNode + " actual node :"+myPosition);
					}
					//4) At each time step, the agent blindly send all its graph to its surrounding to illustrate how to share its knowledge (the topology currently) with the the others agents. 	
					// If it was written properly, this sharing action should be in a dedicated behaviour set, the receivers be automatically computed, and only a subgraph would be shared.
					/*
					if (((ExploreCoopAgent)this.myAgent).changeNode) {
						((ExploreCoopAgent)this.myAgent).changeNode = false;
						nextNode = ((ExploreCoopAgent)this.myAgent).nextNode = nextNode;
					}
					*/
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
	
					((fsmAgent)this.myAgent).updateMap(this.myMap);
					((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);	
				}
				
				
				if (oldNode.equals(myPosition)) {
					if ( nb_move_fail >= max_move_fail) {
						System.out.println(this.myAgent.getLocalName() + " --> I Blocked a Golem ! (stop move)");
						this.exitValue = 3;
						return ;
					}
					nb_move_fail++;
				}else {
					oldNode = myPosition;
					nb_move_fail = 0;
				}
	
			}
		}
	}
	
	public boolean checkMsg() {
		
		MessageTemplate msgTemplateMap=MessageTemplate.and(
				MessageTemplate.MatchProtocol("ProtocoleShareMap"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		ACLMessage msgMap=this.myAgent.receive(msgTemplateMap);
		
		if (msgMap != null) {
			//System.out.println(this.myAgent.getLocalName() + " --> Receive a Map (stop move)");
			this.exitValue = 2;//Go to share map
			this.start = 0;
			((AbstractDedaleAgent)this.myAgent).postMessage(msgMap);
			return true;
		}
		
		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocolePoke"), 
				MessageTemplate.MatchPerformative(ACLMessage.INFORM) );	
		final ACLMessage msg = this.myAgent.receive(msgTemplate);

		//If receive a message, don't move
		if (msg != null) {
			//System.out.println(this.myAgent.getLocalName() + " --> Receive a poke (stop move)");
			this.exitValue = 2;//Go to share map
			this.start = 0;
			return true;
		}
		
		return false;
	}
	
	public boolean checkTimer() {
		if (this.start == 0) {
			this.start = (int) System.currentTimeMillis();
		}
		this.now = (int) System.currentTimeMillis();
		int res = this.now - this.start;
		//System.out.println("Timer : "+res);
		if ( res > this.timer) {
			System.out.println(this.myAgent.getLocalName()+" ---> Take a break to breathe");
			this.exitValue = 1;//Go to say hello, to know if there are friends nearby.
			this.start = 0;
			return true;
		}
		return false;
	}
	@Override
	public int onEnd() {
		return exitValue;
	}

}
