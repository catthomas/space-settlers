package stan5674;

import spacesettlers.objects.Base;

public class BaseState {
	/** The base the state is tracking **/
	Base base;

	/** Default constructor for base state **/
	BaseState(Base base){
		this.base = base;
	} //end BaseState

	/** Getter for base **/
	public Base getBase(){
		return base;
	} //end getBase

	/** Number of ships within the base's frontier **/
	public int numberOfShips(double frontier){
		return 0;
	} //end 
} //end BaseState class
