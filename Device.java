package osp.Devices;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Tasks.*;
import java.util.*;

/**
 * @OSPProject Devices
 * @author Dion de Jong
 * @email Ddejong@email.sc.edu
 * @version Final
 * @Date 4-18-15
 * @class DiskInterruptHandler
 *  This class stores all pertinent information about a device in
 *  the device table.  This class should be sub-classed by all
 *  device classes, such as the Disk class.
 */ 


public class Device extends IflDevice
{
   /**
	* @OSPProject Devices
	* @method Constructor
	* This constructor initializes a device with the provided parameters.
	* As a first statement it must have the following: super(id,numberOfBlocks); 
	*
	* @param numberOfBlocks -- number of blocks on device, id
	* @return N/A
	*/
    public Device(int id, int numberOfBlocks)
    {
        super(id, numberOfBlocks);
        //This will initialize the Queue. 
        iorbQueue = new GenericList();
    }

   /**
	* @OSPProject Devices
	* @method init()
	* This method is called once at the beginning of the
    * simulation. Can be used to initialize static variables.
	*
	* @param N/A
	* @return N/A
	*/
    public static void init()
    {
    	//Some initialization? 
    }

   /**
	* @OSPProject Devices
	* @method do_enqueueIORB 
    * This method must lock the page (which may trigger a page fault),
    * check the device's state and call startIO() if the 
    * device is idle, otherwise append the IORB to the IORB queue.
    *
	* @param iorb-- The IORB that is being enqueued.
	* @return SUCCESS or FAILURE.
    * FAILURE is returned if the IORB wasn't enqueued 
    * (for instance, locking the page fails or thread is killed).
    * SUCCESS is returned if the IORB is fine and either the page was 
    * valid and device started on the IORB immediately or the IORB
    * was successfully enqueued (possibly after causing pagefault pagefault)
	*/
    public int do_enqueueIORB(IORB iorb)
    {
    	//When an iorb is queued, we must lock the page that holds it,
        iorb.getPage().lock(iorb);
        
        //we must keep track of the number of IORBs open on the Device, so we increment.
        iorb.getOpenFile().incrementIORBCount();
        
        //This part involves calculating the cylinder      
        int blocknumber = iorb.getBlockNumber(); 
        int numberOfPlatters = ((Disk)this).getPlatters(); 

        //Calculate the different numbers needed for the formula for cylinder, most are part of the Disk class
        //and will have to be converted to (Disk) 
        int pageSize = (MMU.getVirtualAddressBits() - MMU.getPageAddressBits()); 
        int bytesPerBlock = (int)Math.pow(2, pageSize);
        int bytesPerTrack = ((Disk)this).getSectorsPerTrack() * bytesPerSector; 
        int blocksPerTrack = bytesPerTrack/bytesPerBlock; 
        
        //calculate the cylinder value and set it to the IORB
        int Cylinder = blocknumber/(blocksPerTrack * numberOfPlatters);
        iorb.setCylinder(Cylinder);
        
        //If the thread is being killed, we fail
        if (iorb.getThread().getStatus() == ThreadKill)
        {
        	return FAILURE; 
        }
        
        //Otherwise, we assume the thread is not being killed, and will return success
        else 
        {
        	if (!this.isBusy())
        	{
        		//furthermore, if the iorb hasn't began, we must start it 
        		startIO(iorb); 
        		return SUCCESS; 
        	}
        	else
        	{
        		//if the read is not killed and the iorb is busy, we must add it the the queue and return success
	        	((GenericList)iorbQueue).append(iorb);
	        	return SUCCESS; 
        	}
        }
    }

   /**
	* @OSPProject Devices
	* @method do_dequeue()
	* Selects an IORB (according to some scheduling strategy)
    * and dequeues it from the IORB queue.
    *  
	* @param N/A
	* @return The dequeued IORB
	*/
    public IORB do_dequeueIORB()
    {
    	//if there is nothing to dequeue return null
        if (iorbQueue.isEmpty())
        {
        	return null; 
        }
        //otherwise there is something
        else 
        {
        	//pull the first item from the queue (the head) and return
        	IORB head = (IORB)((GenericList)iorbQueue).removeHead(); 
        	return head; 
        }
    }

   /**
	* @OSPProject Devices
	* @method do_cancelPendingIO()
	* Remove all IORBs that belong to the given ThreadCB from 
	* this device's IORB queue
    * The method is called when the thread dies and the I/O 
    * operations it requested are no longer necessary. The memory 
    * page used by the IORB must be unlocked and the IORB count for 
    * the IORB's file must be decremented.
    *  
	* @param the thread having an IO cancelled
	* @return N/A
    */
    public void do_cancelPendingIO(ThreadCB thread)
    {
    	//if the queue is already empty we don't really need to do anything
        if (iorbQueue.isEmpty())
        {
        	return; 
        }
        
        //If a devices queue has a set amount of IORBs
        else 
        {
        	//Create an enumeration (or Iterator) through the queue for a device with IORBs
        	Enumeration<IORB> QueueEnum = ((GenericList)iorbQueue).forwardIterator();
        	IORB currentIORB = null; 
        	//as long as there are more IORBs
        	while (QueueEnum.hasMoreElements())
        	{
        		//save the current IORBs
        		currentIORB = QueueEnum.nextElement();
        		//If the thread matches the IORBs thread
        		if (thread == currentIORB.getThread())
        		{
        			//Unlock the IORBs page
        			currentIORB.getPage().unlock();
        			//decrement the number of IORBs we know exist
        			currentIORB.getOpenFile().decrementIORBCount(); 
        			//if the count of IORBs that are open is 0, and the file is not already closed, we should close it.
        			if (currentIORB.getOpenFile().getIORBCount() == 0 && currentIORB.getOpenFile().closePending)
        			{
        				//and close the IORBs OpenFile
        				currentIORB.getOpenFile().close();
        			}
        			//Remove the IORB from the queue. 
        			((GenericList) iorbQueue).remove(currentIORB); 
        		}
        	}
        }
    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Devices
     */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
	
	@OSPProject Devices
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
