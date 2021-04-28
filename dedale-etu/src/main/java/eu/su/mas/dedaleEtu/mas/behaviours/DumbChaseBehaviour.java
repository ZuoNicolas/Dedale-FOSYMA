package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
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
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class DumbChaseBehaviour extends OneShotBehaviour{
	/**
	 * 
	 */
	private static final long serialVersionUID = 267531971032817028L;

	private MapRepresentation myMap;
	
	private int exitValue;
	
	private String nodeGoal = "";
	
	private int timer, nb_move_fail, max_move_fail=((fsmAgent)this.myAgent).AgentSensitivity;
	
	private String oldNode="";
	
	private boolean firstTime=true;

	private List<String> temp;

	private List<String> leavePath;

	private String receiveAgentName;

	private boolean modeLeavePath = false;

	private boolean SuccessMove;
	
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
		
		if(modeLeavePath) {
			if(!leavePath.contains(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
				try {
					this.myAgent.doWait(((fsmAgent)this.myAgent).AgentSpeed * 3);
				} catch (Exception e) {
					e.printStackTrace();
				}
				modeLeavePath = false;
				leavePath= null;
				return ; 
			}
			String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

			List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition

			//1) remove the current node from openlist and add it to closedNodes.
			this.myMap.addNode(myPosition, MapAttribute.closed);

			//2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
			String nextNode=null;
			Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
			while(iter.hasNext()){
				Couple<String, List<Couple<Observation, Integer>>> node = iter.next();
				String nodeId= node.getLeft();

				if (myPosition!=nodeId) {
					this.myMap.addEdge(myPosition, nodeId);
					if (nextNode==null && leavePath.contains(nodeId)) nextNode=nodeId;
				}
			}
			if (nextNode == null) {
				System.out.println(this.myAgent.getLocalName()+" ---> Sorry I'm not smart enough to find a knot to let you through");
				sendSorryMsg();
				modeLeavePath = false;
				leavePath= null;
				try {
					this.myAgent.doWait(((fsmAgent)this.myAgent).AgentSpeed * 3);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return ; 
			}else{
				
				((fsmAgent)this.myAgent).nextNode=nextNode;
				((fsmAgent)this.myAgent).updateMap(this.myMap);
				SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);	
				
				if(SuccessMove) {
					System.out.println(this.myAgent.getLocalName()+" ---> I find a node to back ! Good luck to your important mision ");
				}else {
					System.out.println(this.myAgent.getLocalName()+" ---> Sorry I find a node to back, but something block me ");
					sendSorryMsg();
					modeLeavePath = false;
					leavePath= null;
				}
			}
			return ;
		}
		
		if (((fsmAgent)this.myAgent).moveTo != null) {
			this.exitValue = 5;
		}
		
		if (firstTime) {
			firstTime=false;
			System.out.println(this.myAgent.getLocalName()+" Start Chase !");
		}
		
		if(checkMsg()) {
			clearMail();
			return ;
		}

		
		this.myMap = ((fsmAgent)this.myAgent).getMap();

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
				this.myAgent.doWait(((fsmAgent)this.myAgent).AgentSpeed);
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
						((fsmAgent)this.myAgent).blockedAgent.clear();
						return; //reset
					}
				}else {
					((fsmAgent)this.myAgent).blockedAgent.clear();
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
			SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);
		}
		
		
		if (!SuccessMove) {
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
	
		
	public boolean checkMsg() {
		
		while(true) {
			MessageTemplate msgTemplatelp=MessageTemplate.and(
					MessageTemplate.MatchProtocol("ProtocoleLeavePath"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		
			ACLMessage msglp=this.myAgent.receive(msgTemplatelp);
			
			if(msglp!=null) {
				try {
					leavePath = (List<String>) msglp.getContentObject();
				} catch (UnreadableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				receiveAgentName = msglp.getSender().getLocalName();
				modeLeavePath  = true;
			}
			else {
				break;
			}
		}
		if (modeLeavePath) {
			return true;
		}

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
    		List<String> NodeToBlock = new ArrayList<String>();
			try {
				NodeToBlock = (List<String>) msgBlock.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();			
			}
			for (String n : NodeToBlock) {
				System.out.println(this.myAgent.getLocalName() + " -9-> "+n);

			}
			((fsmAgent)this.myAgent).NodeToBlock = NodeToBlock;
			String AgentPos = ((fsmAgent)this.myAgent).NodeToBlock.get(0);
			((fsmAgent)this.myAgent).blockedAgent.add(AgentPos);
			((fsmAgent)this.myAgent).NodeToBlock.remove(0);
			
			String AgentNextPos = ((fsmAgent)this.myAgent).NodeToBlock.get(0);
			((fsmAgent)this.myAgent).NodeToBlock.remove(0);
			
			if(((fsmAgent)this.myAgent).NodeToBlock.contains(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
				System.out.println(this.myAgent.getLocalName() + " --> I'm already on the Node to block ("+((AbstractDedaleAgent)this.myAgent).getCurrentPosition()+")");
				((fsmAgent)this.myAgent).nextNode = AgentNextPos;
				((fsmAgent)this.myAgent).NodeToBlock.remove(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
				
				exitValue = 6;
				return true;
			}
			//If the Sender want to come to my position
			if(AgentNextPos.equals(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
				ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
				sendMsg.setProtocol("ProtocoleByPass");
				sendMsg.setSender(this.myAgent.getAID());
				sendMsg.setContent(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
		
				sendMsg.addReceiver(new AID(msgBlock.getSender().getLocalName(),AID.ISLOCALNAME));

				((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
				
				System.out.println(this.myAgent.getLocalName() + " --> I'm not a Wumpus "+AgentNextPos+" ! (ExploCoopBehaviour)");
				return false;
			}
			/*
			if(!AgentNextPos.equals(((fsmAgent)this.myAgent).nextNode)) {
				System.out.println(this.myAgent.getLocalName() + " --> Ignore it's didn't concern me at "+AgentNextPos+" (ExploCoopBehaviour)");
				return false;
			}*/
			System.out.println(this.myAgent.getLocalName() + " --> NodeToBlock "+((fsmAgent)this.myAgent).NodeToBlock+" (ExploCoopBehaviour)");
			if(((fsmAgent)this.myAgent).NodeToBlock.size()>0) {
				((fsmAgent)this.myAgent).moveTo = ((fsmAgent)this.myAgent).NodeToBlock.get(0);
				((fsmAgent)this.myAgent).NodeToBlock.remove(0);
				((fsmAgent)this.myAgent).blockedAgent.add(AgentNextPos);
				System.out.println(this.myAgent.getLocalName() + " --> Go to MoveToBehaviour "+((fsmAgent)this.myAgent).moveTo);
				exitValue = 5;
				return true;
			}
			return false;
			
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
    	
    	final MessageTemplate msgBy = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleByPass"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));	
    	final ACLMessage msgB = this.myAgent.receive(msgBy);
    	
    	if (msgB != null) {
    		String m =msgB.getContent();
    		if ( m.equals(((fsmAgent)this.myAgent).nextNode)) {
        		System.out.println(this.myAgent.getLocalName() + " --> Ok it's not a Golem, I will try to by pass you "+msgB.getSender().getLocalName());
        		((fsmAgent)this.myAgent).blockedAgent.add(m);
        		return false;
    		}
    	}
    	
		return false;
	}
	
	public void sendSorryMsg() {
		if (receiveAgentName != null) {
			ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
			sendMsg.setProtocol("ProtocoleSorry");
			sendMsg.setSender(this.myAgent.getAID());

			sendMsg.setContent(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());

			sendMsg.addReceiver(new AID(receiveAgentName,AID.ISLOCALNAME));

			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
			((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
		}
	}
	public void clearMail() {
		while(true) {
			MessageTemplate msgTemplate= MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg=this.myAgent.receive(msgTemplate);
			
			if(msg == null) {
				break;
			}
		}
	}
	@Override
	public int onEnd() {
		return exitValue;
	}

}
