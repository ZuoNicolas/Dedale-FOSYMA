package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import java.util.ArrayList;
import java.util.List;

import org.graphstream.graph.Node;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.CheckWumpusBlockedBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.DumbChaseBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.EndBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ImNotWumpus;
import eu.su.mas.dedaleEtu.mas.behaviours.InitMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.MoveToBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.NeedHelpBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ReceiveMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SayHelloBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SomethingBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.StrangeWaitBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SuccessBlockBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.WaitSomethingBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;

public class fsmAgent extends AbstractDedaleAgent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1161691655438824095L;
	private MapRepresentation myMap;
	
	public boolean move=true, succesMerge=false, changeNode=false, successBlock=false, forceChangeNode=false, endExplo=false;
	public List<String> NodeToBlock, blockedAgent= new ArrayList<String>(), GolemPoop = new ArrayList<String>();
	public String nextNode, agentToContact, moveTo;
	
	private static final int PokeTime = 3000;
	
	public final int AgentSpeed=300;//doWait every AgentSpeed ms
	public final int AgentSensitivity=30;//number of times it is blocked before checking if it is a golem
	
	private int nbAgent;
	
	private List<String> list_agentNames;
	
	private List<Behaviour> lb;
	
	private FSMBehaviour fsm;
	
	private static final String INIT = "initMap";
	private static final String A = "Exploration";
	private static final String B = "Poke";
	private static final String C = "ShareMap";
	private static final String D = "ReceiveMap";
	private static final String E = "Chase";
	private static final String F = "CheckWumpus";
	private static final String G = "SuccessBlock";
	private static final String H = "NeedHelpToBlock";
	private static final String I = "StrangeWait";
	private static final String J = "NotGolem";
	private static final String K = "Semething";
	private static final String L = "WaitSomething";
	private static final String M = "MoveTo";
	private static final String Z = "End";

	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){

		super.setup();
		
		final Object[] args = getArguments();
		
		List<String> agentNames=new ArrayList<String>();
		
		if(args.length==0){
			System.err.println("Error while creating the agent, names of agent to contact expected");
			System.exit(-1);
		}else{
			int i=2;// WARNING YOU SHOULD ALWAYS START AT 2. This will be corrected in the next release.
			while (i<args.length) {
				agentNames.add((String)args[i]);
				i++;
			}
		}
		this.nbAgent = agentNames.size();
		this.list_agentNames = agentNames;

		fsm = new FSMBehaviour(this);
		// Define the different states and behaviours
		fsm.registerFirstState(new InitMapBehaviour(),INIT);
		fsm.registerState(new ExploCoopBehaviour(this,this.myMap, PokeTime), A);
		fsm.registerState(new SayHelloBehaviour(this, list_agentNames), B);
		fsm.registerState(new ShareMapBehaviour(this, this.myMap, this.list_agentNames), C);
		fsm.registerState(new ReceiveMapBehaviour(this), D);
		fsm.registerState(new DumbChaseBehaviour(this,this.myMap, PokeTime), E);
		fsm.registerState(new CheckWumpusBlockedBehaviour(this),F);
		fsm.registerState(new SuccessBlockBehaviour(this),G);
		fsm.registerState(new NeedHelpBehaviour(this),H);
		fsm.registerState(new StrangeWaitBehaviour(this),I);
		fsm.registerState(new ImNotWumpus(),J);
		fsm.registerState(new SomethingBehaviour(),K);
		fsm.registerState(new WaitSomethingBehaviour(),L);
		fsm.registerState(new MoveToBehaviour(),M);
		fsm.registerLastState(new EndBehaviour(), Z);
		// Register the transitions
		fsm.registerDefaultTransition(INIT,A);//Wait to receive map
		fsm.registerDefaultTransition(A,A);//Back to explo
		fsm.registerTransition(A,B, 1) ;//Cond 1, poke every PokeTime
		fsm.registerDefaultTransition(B,A);//Back to explo
		fsm.registerTransition(A,C, 2) ;//Cond 2, 'A' receive msg
		fsm.registerDefaultTransition(C,D);//Wait to receive map
		fsm.registerDefaultTransition(D,D) ;//wait to receive a msg
		fsm.registerTransition(D,A, 1) ;//Back to explo
		fsm.registerTransition(D,G, 2) ;//Cond 5, Go to help block
		fsm.registerTransition(A,E, 4) ;//Cond 4, End Explo
		fsm.registerTransition(A,F, 3) ;//Cond 3, Check Wumpus
		fsm.registerTransition(A,H, 5) ;//Cond 5, Go to help block
		fsm.registerDefaultTransition(F,A) ;//Back to Explo
		fsm.registerDefaultTransition(E,E) ;//Back to Chase
		
		fsm.registerTransition(F,G, 1) ;//Cond 1, go to succes block
		fsm.registerTransition(F,H, 2) ;//Cond 2, go to need help block
		fsm.registerTransition(F,I, 3) ;//Cond 3, got strange wait
		
		fsm.registerTransition(H,G, 1) ;//Cond 1, got strange wait
		fsm.registerTransition(H,A, 2) ;//Cond 1, back to Explo
		fsm.registerTransition(I,A, 1) ;//Cond 1, back to Explo
		
		fsm.registerTransition(G,C, 1) ;//Cond 1, got strange wait
		fsm.registerTransition(G,A, 2) ;//Cond 1, got strange wait
		
		fsm.registerDefaultTransition(G,G) ;//wait to success block
		fsm.registerDefaultTransition(H,H) ;//wait to need help block
		fsm.registerDefaultTransition(I,I) ;//wait to strange Wait
		
		//CHECK SOMETHING
		fsm.registerTransition(E,F, 2) ;//Cond 3, got strange wait
		//fsm.registerTransition(E,K, 3) ;//Cond 3, got strange wait
		//fsm.registerTransition(E,J, 7) ;//Cond 3, got strange wait
		fsm.registerDefaultTransition(J,E) ;//wait to success block
		fsm.registerTransition(E,C, 4) ;//Cond 3, got strange wait
		fsm.registerDefaultTransition(K,J) ;//wait to success block
		fsm.registerTransition(J,L, 1) ;//Cond 3, got strange wait
		fsm.registerTransition(L,E, 1) ;//Cond 3, got strange wait
		fsm.registerTransition(L,F, 2) ;//Cond 3, got strange wait
		fsm.registerDefaultTransition(L,L) ;//wait to success block
		
		fsm.registerDefaultTransition(M,M) ;//wait to success block
		fsm.registerTransition(M,H, 10) ;//Cond 3, got strange wait
		fsm.registerTransition(M,A, 2) ;//Cond 3, got strange wait
		fsm.registerTransition(A,M, 6) ;//Cond 3, got strange wait
		fsm.registerTransition(E,M, 5) ;//Cond 3, got strange wait
		fsm.registerTransition(E,H, 6) ;//Cond 3, got strange wait
		
		fsm.registerTransition(E,Z, 1) ;//Cond 1, End Chase
		
		this.lb=new ArrayList<Behaviour>();
		this.lb.add(fsm);
		addBehaviour(new startMyBehaviours(this,this.lb));

		System.out.println("the  agent "+this.getLocalName()+ " is started mode FSM");

	}
	
	public void updateMap(MapRepresentation Map) {
		this.myMap = Map;
	}
	
	public MapRepresentation getMap() {
		return this.myMap;
	}
	
	public List<String> getList_AgentNames(){
		return this.list_agentNames;
	}
	
	public List<Behaviour> getLB(){
		return this.lb;
	}
	
	public boolean getFinished() {
		if (!this.myMap.hasOpenNode()){
			return true;
		}
		return false;
	}
	
	public int getNbAgent() {
		return this.nbAgent;
	}
	
	public FSMBehaviour getFSM() {
		return fsm;
	}
}
