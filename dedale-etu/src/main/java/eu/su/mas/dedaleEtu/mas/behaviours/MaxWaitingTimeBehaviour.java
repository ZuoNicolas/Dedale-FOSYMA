package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

public class MaxWaitingTimeBehaviour extends SimpleBehaviour{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5545668776691747724L;

	private boolean finished=false;
	
	private int timer, start, now;
	
	
	public MaxWaitingTimeBehaviour (final Agent myagent, int timer) {
		super(myagent);
		this.timer=timer;
		this.start = (int) System.currentTimeMillis();
	}
	
	@Override
	public void action() {
		
		if (((ExploreCoopAgent)this.myAgent).move) {
			this.finished=true;
			System.out.println(this.myAgent.getLocalName()+"<---Echange réussi");
			return ;
		}
		this.now = (int) System.currentTimeMillis();
		
		if ( this.now - this.start > this.timer) {
			((ExploreCoopAgent)this.myAgent).move=true;
			System.out.println(this.myAgent.getLocalName()+"<---Echange raté");
			this.finished=true;
		}
		
		
	}

	@Override
	public boolean done() {
		return finished;
	}

}
