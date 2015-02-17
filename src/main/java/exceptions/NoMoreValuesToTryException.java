package exceptions;

public class NoMoreValuesToTryException extends Exception
{
	public NoMoreValuesToTryException(String message) 
	{
        super(message);
    }

    public NoMoreValuesToTryException(String message, Throwable throwable) 
    {
        super(message, throwable);
    }

}
