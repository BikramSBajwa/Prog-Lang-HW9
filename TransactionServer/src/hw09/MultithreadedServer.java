package hw09;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


class Cache{
	
	private int name;
	private int value;
	private int initialValue;
	private String operation;
	private boolean lhv;
	
	public Cache(int n, int initialVal, String op){
		name = n;
		value = initialVal;
		operation = op;
		initialValue = initialVal;
	}
	
	public int getValue(){	return value;	}
	public int getName(){	return name;	}
	public int getInitialValue(){return initialValue;}
	public String getOperation(){return operation;}
	public void setOperation(String op){operation = op;}
	public void setName(int n){name = n;}
	public void setValue(int n){value = n;}
}

// TO DO: Task is currently an ordinary class.
// You will need to modify it to make it a task,
// so it can be given to an Executor thread pool.
//
class Task implements Runnable {
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;

    private Account[] accounts;
    private Cache[] caches;
    private ArrayList<Cache> currentCaches;
    private String transaction;

    // TO DO: The sequential version of Task peeks at accounts
    // whenever it needs to get a value, and opens, updates, and closes
    // an account whenever it needs to set a value.  This won't work in
    // the parallel version.  Instead, you'll need to cache values
    // you've read and written, and then, after figuring out everything
    // you want to do, (1) open all accounts you need, for reading,
    // writing, or both, (2) verify all previously peeked-at values,
    // (3) perform all updates, and (4) close all opened accounts.

    public Task(Account[] allAccounts, String trans) {
        accounts = allAccounts;
        transaction = trans;
        caches = new Cache[numLetters];
        currentCaches = new ArrayList<Cache>();
    }
    
    // TO DO: parseAccount currently returns a reference to an account.
    // You probably want to change it to return a reference to an
    // account *cache* instead.
    //
    private Cache parseAccount(String name, boolean addCache, String op) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        //Account a = accounts[accountNum];
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            accountNum = (accounts[accountNum].peek() % numLetters);
            //a = accounts[accountNum];
        }
        if(caches[accountNum] == null){
        	Cache c = new Cache(accountNum, accounts[accountNum].peek(), op);
        	//if(addCache){
        		caches[accountNum] = c;
        	//}
        	//System.out.println("1 Name: " + accountNum + " Value: " + c.getValue());
        	return c;
        }
        else {
        	
        	//System.out.println("2 Name: " + accountNum + " Value: " + caches[accountNum].getValue());
        	caches[accountNum].setOperation(op);
        	return caches[accountNum];
        }
    }

    private void parseAccountOrNum(String name, String op) {
        Cache c;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            int rtn = new Integer(name).intValue();
            c = new Cache(-1, rtn, op);
            
        } else {
        	c = parseAccount(name, false, op);
//        	int accountNum = (int) (name.charAt(0)) - (int) 'A';
//            if (accountNum < A || accountNum > Z)
//                throw new InvalidTransactionError();
//            c = new Cache(accountNum, a.peek(), op);
        	
//        	int accountNum = (int) (name.charAt(0)) - (int) 'A';
//        	System.out.println("Name: " + accountNum + " Value: " + c.getValue());
//        	caches[accountNum] = c;
        	
        }
        //System.out.println("Name: " + name+ " Value: " + c.getValue());
        currentCaches.add(c);
    }

    public void run() {
        // tokenize transaction
        String[] commands = transaction.split(";");

        while(true){
        	caches = new Cache[numLetters];
        	for (int i = 0; i < commands.length; i++) {
        		currentCaches.clear();
        		String[] words = commands[i].trim().split("\\s");
        		if (words.length < 3)
        			throw new InvalidTransactionError();
	            Cache lhs = parseAccount(words[0], true, "=");
	            if (!words[1].equals("="))
	                throw new InvalidTransactionError();
	            parseAccountOrNum(words[2], "+");
	            for (int j = 3; j < words.length; j+=2) {
	                if (words[j].equals("+"))
	                    parseAccountOrNum(words[j+1], "+");
	                else if (words[j].equals("-"))
	                    parseAccountOrNum(words[j+1], "-");
	                else
	                    throw new InvalidTransactionError();
	            }
	            int rhs = 0;
	            for(int j = 0; j < currentCaches.size(); j++){
	            	//System.out.println("J: " + j + " Val: " + currentCaches.get(j).getName());
	            	if(currentCaches.get(j).getOperation() == "+"){
	            		if(currentCaches.get(j).getName() == -1){
	            			rhs += currentCaches.get(j).getValue();
	            		}else{
	            			//System.out.println("J: " + j + " Val: " + caches[currentCaches.get(j).getName()].getValue());
	            			rhs += caches[currentCaches.get(j).getName()].getValue();
	            		}
	            	}
	            	else if(currentCaches.get(j).getOperation() == "-"){
	            		if(currentCaches.get(j).getName() == -1){
	            			rhs -= currentCaches.get(j).getValue();
	            		}else{
	            			rhs -= caches[currentCaches.get(j).getName()].getValue();
	            		}
	            	}
	            }
	            caches[lhs.getName()].setValue(rhs);
        	}
	            
	            
	        int j = 0;
	        boolean opened = true;
	        try {
	            	
	        	for(; j < caches.length; j++){
	        		if(caches[j] != null){
	        			accounts[caches[j].getName()].open(false);
	        			opened = false;
	        			if(caches[j].getInitialValue() != caches[j].getValue()){
	        				accounts[caches[j].getName()].open(true);
		        			//System.out.println("Opened for Writing: " + caches[j].getName());
	        			}
	        			opened = true;
	        			//System.out.println("Opened for Reading: " + caches[j].getName());
	        		}
	        	}
	        	
	        	for(int k = 0; k < caches.length; k++){
	        		if(caches[k] != null){
	        			accounts[caches[k].getName()].verify(caches[k].getInitialValue());
	        			//System.out.println("Verified: " + caches[k].getName());
	        		}
	        	}
        		
	        	
//	        	for(j = 0; j < caches.length; j++){
//	        		if(caches[j] != null &&
//	        				accounts[caches[j].getName()].getValue() != caches[j].getValue()){
//	        			//System.out.println("Opening for Writing: " + caches[j].getName());
//	        			accounts[caches[j].getName()].open(true);
//	        			System.out.println("Opened for Writing: " + caches[j].getName());
//	        		}
//	        	}
	        	
			                
	        } catch (TransactionAbortException e) {
//	        	e.printStackTrace();
//	        	if(opened){
//	        	for(j = 0;j < caches.length; j++){
//	        		if(caches[j]!= null){
//	        			accounts[caches[j].getName()].close();
//	        			//System.out.println("Closed: " + caches[j].getName());
//	        		}
//	        	}
//	        	}else{
//	        		j--;
//	        		for(;j>-1;j--){
//	        			if(caches[j]!= null){
//		        			accounts[caches[j].getName()].close();
//		        			//System.out.println("Closed: " + caches[j].getName());
//		        		}
//	        		}
//	        	}
	        	
	        	if(opened)
	        		j--;
	        	
	        	for(;j>-1;j--){
        			if(caches[j] != null){
        				
        				//try{
	        			accounts[caches[j].getName()].close();
//        				}catch(TransactionUsageError e2){
//        					System.out.println(transaction + " " + j);
//        				}
	        			//System.out.println("Closed: " + caches[j].getName());
	        		}
        		}
	        	
	        	continue;
	        } 
	        /*System.out.println("Current Command:\n" + commands);
			            for(int j = 0; j < caches.size(); j++){
			            	System.out.println(caches.get(j).getName() + " " + caches.get(j).getOperation() + " " + caches.get(j).getValue());
			            }	
			            System.out.println();*/
			            
	        for(j = 0; j < caches.length; j++){
        		if(caches[j] != null){
        			if(caches[j].getInitialValue() != caches[j].getValue()){
	        			accounts[caches[j].getName()].update(caches[j].getValue());
	        			//System.out.println("Updated: " + caches[j].getName());
        			}
        			
	        		accounts[caches[j].getName()].close();
	        			
        		}
        	}
	        
//	        for(j = 0; j < caches.length; j++){
//	        	if(caches[j] != null){
//	        		accounts[caches[j].getName()].close();
//	        		//System.out.println("Closed: " + caches[j].getName());
//	        	}
//	        }
//	        int accountNum = (int) (lhsName.charAt(0)) - (int) 'A';
//	        for(j = 0; j < caches.size(); j++){
//	        	if(caches.get(j).getName() != -1 && accountNum != caches.get(j).getName()){
//	        		accounts[caches.get(j).getName()].close();
//	        	}
//	        }
			            //lhs.close();
			        
	            
        
        System.out.println("commit: " + transaction);
        break;
        }
    }
}

public class MultithreadedServer {

	// requires: accounts != null && accounts[i] != null (i.e., accounts are properly initialized)
	// modifies: accounts
	// effects: accounts change according to transactions in inputFile
    public static void runServer(String inputFile, Account accounts[])
        throws IOException {

        // read transactions from input file
        String line;
        BufferedReader input =
            new BufferedReader(new FileReader(inputFile));

        // TO DO: you will need to create an Executor and then modify the
        // following loop to feed tasks to the executor instead of running them
        // directly.  
        
        ExecutorService taskExecutor = Executors.newCachedThreadPool();
        while ((line = input.readLine()) != null) {
            Task t = new Task(accounts, line);
            //t.run();
            taskExecutor.execute(t);
        }
        taskExecutor.shutdown();
        try {
          taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
        }
        
        input.close();

    }
}
