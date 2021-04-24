package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import java.util.ArrayList;
import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.DumbChaseBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.EndBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.InitMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ReceiveMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SayHelloBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;

public class fsmAgent extends AbstractDedaleAgent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1161691655438824095L;
	private MapRepresentation myMap;
	
	public boolean move=true, succesMerge=false, changeNode=false;
	
	private static final int PokeTime = 3000;
	
	private List<String> list_agentNames;
	
	private List<Behaviour> lb;
	
	private static final String A = "Exploration";
	private static final String B = "Poke";
	private static final String C = "ShareMap";
	private static final String D = "ReceiveMap";
	private static final String E = "Chase";
	private static final String F = "End";

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
		this.list_agentNames = agentNames;

		FSMBehaviour fsm = new FSMBehaviour(this);
		// Define the different states and behaviours
		fsm.registerFirstState(new ExploCoopBehaviour(this,this.myMap, PokeTime), A);
		fsm.registerState(new SayHelloBehaviour(this, list_agentNames), B);
		fsm.registerState(new ShareMapBehaviour(this, this.myMap, this.list_agentNames), C);
		fsm.registerState(new ReceiveMapBehaviour(this), D);
		fsm.registerState(new DumbChaseBehaviour(this,this.myMap, PokeTime), E);
		fsm.registerLastState(new EndBehaviour(), F);
		// Register the transitions
		fsm.registerDefaultTransition(A,A);//Back to explo
		fsm.registerTransition(A,B, 1) ;//Cond 1, poke every PokeTime
		fsm.registerDefaultTransition(B,A);//Back to explo
		fsm.registerTransition(A,C, 2) ;//Cond 2, 'A' receive msg
		fsm.registerDefaultTransition(C,D);//Wait to receive map
		fsm.registerDefaultTransition(D,D) ;//wait to receive a msg
		fsm.registerTransition(D,A, 1) ;//Back to explo
		fsm.registerTransition(A,E, 3) ;//Cond 3, End Explo
		fsm.registerDefaultTransition(E,E) ;//Back to Chase
		fsm.registerTransition(E,F, 1) ;//Cond 1, End Chase
		
		this.lb=new ArrayList<Behaviour>();
		this.lb.add(new InitMapBehaviour());
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
}
