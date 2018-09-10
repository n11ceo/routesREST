package ru.bpc.cm.items.routes;

public class RoutingException extends Exception {

	private int code;

	public static final int MISSING_PARAMETERS = 1;
	public static final int INCORRECT_PARAMETERS = 2;
	public static final int NO_ENCASHMENTS = 3;
	public static final int MOVE_FORBIDDEN = 4;

    private static final long serialVersionUID = 1L;
    public RoutingException(int code){
    	super(String.valueOf(code));
    	this.setCode(code);
    }

	public RoutingException(String string) {
		super(string);
    }

	public void setCode(int code) {
	    this.code = code;
    }
	public int getCode() {
	    return code;
    }

}
