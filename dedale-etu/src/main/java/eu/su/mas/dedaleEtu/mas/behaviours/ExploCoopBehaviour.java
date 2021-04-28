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
	
	private int timer, start, now, nb_move_fail, max_move_fail=20, timer_spam = 1000;
	
	private String oldNode="";
	
	private boolean SuccessMove;
	
	private List<String> temp;
	
	private List<Couple<String,Integer>> list_spam = new ArrayList<>();
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
		//list_spam.add(new Couple<>("teste", Integer.valueOf(5)));
		if(((fsmAgent)this.myAgent).endExplo) {
			this.exitValue = 4;//End of exploration
			((fsmAgent)this.myAgent).forceChangeNode=true;
			return ;
		}
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
					/*
			        for(Couple<Observation, Integer> model : list) {
			            System.out.print("Observation "+model.getLeft()+ " Integer "+model.getRight()+" | ");
			        }
			        */
			        //System.out.println(this.myAgent.getLocalName()+" nodeID "+nodeId);
					if (myPosition!=nodeId) {
						this.myMap.addEdge(myPosition, nodeId);
						if (nextNode==null && isNewNode) nextNode=nodeId;
					}
				}
	
				//3) while openNodes is not empty, continues.
				if (!this.myMap.hasOpenNode()){
					//Explo finished
					this.exitValue = 4;//End of exploration
					((fsmAgent)this.myAgent).endExplo=true;
					System.out.println(this.myAgent.getLocalName()+" - Exploration successufully done");
				}else{
					//4) select next move.
					//4.1 If there exist one open node directly reachable, go for it,
					//	 otherwise choose one from the openNode list, compute the shortestPath and go for it
					
					if (nextNode==null || ((fsmAgent)this.myAgent).forceChangeNode){
						if (((fsmAgent)this.myAgent).succesMerge || ((fsmAgent)this.myAgent).forceChangeNode) {
							if (nodeGoal.equals("") || oldNode.equals(myPosition) || ((fsmAgent)this.myAgent).forceChangeNode) {
								List<String> opennodes=this.myMap.getOpenNodes();
								Random rand = new Random();
								nodeGoal = opennodes.get(rand.nextInt(opennodes.size()));
							}
							temp = this.myMap.getShortestPath(myPosition, nodeGoal, ((fsmAgent)this.myAgent).blockedAgent);

							if (temp != null || temp.size()>0) {
								
								nextNode = temp.get(0);
							}else {
								nodeGoal = "";
								System.out.println(this.myAgent.getLocalName()+" ---> null Path, reset");
								return ;
							}
							
							if(nextNode.equals(nodeGoal)) {
								((fsmAgent)this.myAgent).forceChangeNode = false;
								((fsmAgent)this.myAgent).succesMerge = false;
								nodeGoal = "";
							}
						}
						else {
						//no directly accessible openNode
						//chose one, compute the path and take the first step.
						temp = this.myMap.getShortestPathToClosestOpenNode(myPosition, ((fsmAgent)this.myAgent).blockedAgent);
						if (temp != null) {
							nextNode = temp.get(0);
						}else {
							System.out.println(this.myAgent.getLocalName()+" ---> null Path, reset");
							return ;
						}
						nextNode=temp.get(0);//getShortestPath(myPosition,this.openNodes.get(0)).get(0);
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

					((fsmAgent)this.myAgent).nextNode=nextNode;
					((fsmAgent)this.myAgent).updateMap(this.myMap);
					SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);	
				}
				
				
				if (!SuccessMove) {
					if ( nb_move_fail >= max_move_fail) {
						System.out.println(this.myAgent.getLocalName() + " --> I probably blocked a Wumpus ! (stop move)");
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
	}
	
	public boolean checkMsg() {
		
		MessageTemplate msgTemplateMap=MessageTemplate.and(
				MessageTemplate.MatchProtocol("ProtocoleShareMap"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		ACLMessage msgMap=this.myAgent.receive(msgTemplateMap);
		//System.out.println(this.myAgent.getLocalName() + " --> "+this.myAgent.getCurQueueSize());
		if (msgMap != null && !containToList_spam(msgMap.getSender().getLocalName())) {
			System.out.println(this.myAgent.getLocalName() + " --> Receive a Map (stop move)");
			this.exitValue = 2;//Go to share map
			this.start = 0;
			list_spam.add(new Couple<>(msgMap.getSender().getLocalName(), Integer.valueOf((int) System.currentTimeMillis())));
			((AbstractDedaleAgent)this.myAgent).postMessage(msgMap);
			return true;
		}
		
		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocolePoke"), 
				MessageTemplate.MatchPerformative(ACLMessage.INFORM) );	
		final ACLMessage msg = this.myAgent.receive(msgTemplate);

		//If receive a message, don't move
		if (msg != null && !containToList_spam(msg.getSender().getLocalName())) {
			System.out.println(this.myAgent.getLocalName() + " --> Receive a poke (stop move)");
			list_spam.add(new Couple<>(msg.getSender().getLocalName(), Integer.valueOf((int) System.currentTimeMillis())));
			this.exitValue = 2;//Go to share map
			this.start = 0;
			return true;
		}
		
		final MessageTemplate msgTPass = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleByPass"), 
				MessageTemplate.MatchPerformative(ACLMessage.INFORM) );	
		final ACLMessage msgPass = this.myAgent.receive(msgTPass);
		
		if(msgPass != null) {
			((fsmAgent)this.myAgent).blockedAgent.add(msgPass.getContent());
			return false;
		}
		
    	final MessageTemplate msgTemplateBlock = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleHelpBlockWumpus"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));	
    	final ACLMessage msgBlock = this.myAgent.receive(msgTemplateBlock);
    	
    	if (msgBlock != null) {
    		List<String> NodeToBlock = new ArrayList<String>();
			try {
				NodeToBlock = (List<String>) msgBlock.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();			
			}
			boolean error2 = NodeToBlock.remove(msgBlock.getContent());
			
			if ( !error2 ) {
				System.out.println(this.myAgent.getLocalName() + " --> Receive a help blocK Wumpus protocole, but it's wrong I'm not a Wumpus");
				((fsmAgent)this.myAgent).blockedAgent.add(msgBlock.getContent());
	        	return false;
    	    }

			((fsmAgent)this.myAgent).NodeToBlock = NodeToBlock;
			
			((fsmAgent)this.myAgent).blockedAgent.add(((fsmAgent)this.myAgent).NodeToBlock.get(0));
			((fsmAgent)this.myAgent).NodeToBlock.remove(0);
			if(((fsmAgent)this.myAgent).NodeToBlock.contains(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
				((fsmAgent)this.myAgent).NodeToBlock.remove(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
				this.exitValue = 5;
				return true;
			}
			((fsmAgent)this.myAgent).moveTo = ((fsmAgent)this.myAgent).NodeToBlock.get(0);
			((fsmAgent)this.myAgent).NodeToBlock.remove(0);
			System.out.println(this.myAgent.getLocalName() + " --> Receive a help blocK Wumpus protocole, go to help block");
			
			this.exitValue = 4;//Go to help block
			return true;
    	}
		return false;
	}
	
	public boolean checkTimer() {
		Iterator<Couple<String,  Integer>> iter=list_spam.iterator();
		
		while(iter.hasNext()){
			Couple<String, Integer> agent = iter.next();
			Integer time = agent.getRight();
			this.now = (int) System.currentTimeMillis();
			int res = this.now - time.intValue();
			//System.out.println(this.myAgent.getLocalName()+" ---> blocked "+ agent.getLeft());
			if ( res > this.timer_spam) {
				//System.out.println(this.myAgent.getLocalName()+" ---> remove blocked "+ agent.getLeft());
				iter.remove();
			}
		}
		if (this.start == 0) {
			this.start = (int) System.currentTimeMillis();
		}
		this.now = (int) System.currentTimeMillis();
		int res = this.now - this.start;
		//System.out.println("Timer : "+res);
		if ( res > this.timer) {
			//System.out.println(this.myAgent.getLocalName()+" ---> Take a break to breathe");
			this.exitValue = 1;//Go to say hello, to know if there are friends nearby.
			this.start = 0;
			return true;
		}
		return false;
	}
	
	public boolean containToList_spam(String agent_name) {
		Iterator<Couple<String,  Integer>> iter=list_spam.iterator();

		while(iter.hasNext()){
			Couple<String, Integer> agent = iter.next();
			String name= agent.getLeft();
			if ( name.equals(agent_name)) {
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
