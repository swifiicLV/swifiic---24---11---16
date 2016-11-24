package ibrdtn.example.api;



import java.io.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.* ;

public class LogExample
	{
    
             Logger logger ;
            
            LogExample()
                 {
                        logger = Logger.getLogger("MyLog");  
    			     FileHandler fh;  

    						try {  

        							// This block configure the logger with handler and formatter  
        							fh = new FileHandler("/var/log/baseibrdtn/ibrdtn.log");  
        							logger.addHandler(fh);
        							SimpleFormatter formatter = new SimpleFormatter();  
        							fh.setFormatter(formatter);  

        							// the following statement is used to log any messages  
        							 

    							}
    					      catch (SecurityException e) {  
        							e.printStackTrace();  
    								} 
    					      catch (IOException e) {  
        							e.printStackTrace();  
    							      }  

    				  

		}	
		
		
		public void logExample(String s)
			{
			    
				logger.log(Level.SEVERE,s) ;
			}
                 
                 
                 
         }       
