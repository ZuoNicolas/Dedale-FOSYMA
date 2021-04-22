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
	
	private int timer, start, now, nb_move_fail, max_move_fail=10;
	
	private String oldNode="";
	
	private boolean firstTime=true;
	
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
		this.start = 0;
	}

	@Override
	public void action() {
		this.exitValue = 0;
		
		if (firstTime) {
			firstTime=false;
			System.out.println(this.myAgent.getLocalName()+" Start Chase !");
		}
		
			
		this.myMap = ((fsmAgent)this.myAgent).getMap();
		
		//If no msg and agent can move
		if(((fsmAgent)this.myAgent).move) {

			//0) Retrieve the current position
			String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
	
			if (myPosition!=null){
				//List of observable from the agent's current position
				List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
	
				/**
				 * Just added here to let you see what the agent is doing, otherwise he will be too quick
				 */
				try {
					this.myAgent.doWait(200);
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
					/*
			        for(Couple<Observation, Integer> model : list) {
			        	obs = model.getLeft();
			            System.out.print("Observation "+model.getLeft()+ " Integer "+model.getRight());
			        }
			        */
					//System.out.println(this.myAgent.getLocalName()+" nodeID "+nodeId);
					if (myPosition!=nodeId && nodeId!=oldNode && list.size()>0 ) {
						if (nextNode==null) nextNode=nodeId;
					}
				}

				//4) select next move.
				//4.1 If there exist one open node directly reachable, go for it,
				//	 otherwise choose one from the openNode list, compute the shortestPath and go for it
				
				if (nextNode==null){
					if (nodeGoal.equals("") ) {
						List<String> opennodes=this.myMap.getClosedNodes();
						Random rand = new Random();
						nodeGoal = opennodes.get(rand.nextInt(opennodes.size()));
						System.out.println(this.myAgent.getLocalName()+" ---> Init a new nodeGoal("+nodeGoal+") to search Golem");
					}
					nextNode = this.myMap.getShortestPath(myPosition, nodeGoal).get(0);
					if(nextNode.equals(nodeGoal)) {
						nodeGoal = "";
						System.out.println(this.myAgent.getLocalName()+" ---> Arrived to nodeGoal("+nodeGoal+")");
					}

					//no directly accessible openNode
					//chose one, compute the path and take the first step.
					//nextNode=this.myMap.getShortestPathToClosestOpenNode(myPosition).get(0);//getShortestPath(myPosition,this.openNodes.get(0)).get(0);
					//System.out.println(this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"| nextNode: "+nextNode+ " actual node :"+myPosition);
				}else {
					//System.out.println("nextNode notNUll - "+this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"\n -- nextNode: "+nextNode + " actual node :"+myPosition);
					System.out.println(this.myAgent.getLocalName()+" ---> Find a Golem's poop");
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
					this.exitValue = 1;
					return ;
				}
				nb_move_fail++;
				System.out.print(nb_move_fail);
			}else {
				oldNode = myPosition;
				nb_move_fail = 0;
			}
		}
	}

	@Override
	public int onEnd() {
		return exitValue;
	}

}
