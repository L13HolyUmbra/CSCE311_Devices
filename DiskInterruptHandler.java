package osp.Devices;
import java.util.*;
import osp.IFLModules.*;
import osp.Hardware.*;
import osp.Interrupts.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Tasks.*;
import osp.Memory.*;
import osp.FileSys.*;

/**
 * @OSPProject Devices
 * @author Dion de Jong
 * @email Ddejong@email.sc.edu
 * @version Final
 * @Date 4-18-15
 * @class DiskInterruptHandler
 * The disk interrupt handler.  When a disk I/O interrupt occurs,
 * this class is called upon the handle the interrupt.
 */ 
    

public class DiskInterruptHandler extends IflDiskInterruptHandler
{
    /** 
        Handles disk interrupts. 
        
        This method obtains the interrupt parameters from the 
        interrupt vector. The parameters are IORB that caused the 
        interrupt: (IORB)InterruptVector.getEvent(), 
        and thread that initiated the I/O operation: 
        InterruptVector.getThread().
        The IORB object contains references to the memory page 
        and open file object that participated in the I/O.
        
        The method must unlock the page, set its IORB field to null,
        and decrement the file's IORB count.
        
        The method must set the frame as dirty if it was memory write 
        (but not, if it was a swap-in, check whether the device was 
        SwapDevice)

        As the last thing, all threads that were waiting for this 
        event to finish, must be resumed.

        @OSPProject Devices 
    */
    public void do_handleInterrupt()
    {
    	//Get the IORB that caused the interrupt to happen.
        IORB Interrupted = (IORB)InterruptVector.getEvent(); 
        //save neccessary variables from IORB that need to be manipulated. 
        ThreadCB InterruptThread = (ThreadCB)InterruptVector.getThread(); 
        PageTableEntry InterruptPage = Interrupted.getPage(); 
        FrameTableEntry InterruptFrame = InterruptPage.getFrame(); 
        OpenFile InterruptOpenFile = Interrupted.getOpenFile();
        
        //Once one is handled there will be once less IORB
        InterruptOpenFile.decrementIORBCount(); 
        
        //If there are no more interrupts and if we can close it
        if (InterruptOpenFile.getIORBCount() == 0 && InterruptOpenFile.closePending)
        {
        	//we will
        	InterruptOpenFile.close(); 
        }
        
        //Unlock this page
        InterruptPage.unlock();
        
        //if the thread is not being killed
        if (InterruptThread.getStatus() != ThreadCB.ThreadKill)
        {
        	//and if there is no frame
        	if (InterruptFrame == null)
        	{
        		return; 
        	}
        	
        	//if the IORB is not a swap
        	if (Interrupted.getDeviceID() != SwapDeviceID)
        	{
        		//set the reference bit
        		InterruptFrame.setReferenced(true);
        		//and if it is a file read that is not being terminated
        		if (Interrupted.getIOType() == FileRead && InterruptThread.getTask().getStatus() != TaskTerm)
        		{
        			//set the dirty bit
        			InterruptFrame.setDirty(true);
        		}
        	}
        	
        	//if the IORB is a swap
        	if (Interrupted.getDeviceID() == SwapDeviceID)
        	{
        		//check if the task is being terminated
        		if (InterruptThread.getTask().getStatus() != TaskTerm)
        		{
        			//set the dirty bit to 0
        			InterruptFrame.setDirty(false);
        		}
        	}
        	
        }
        
        //if the task is being terminated
        if (InterruptThread.getTask().getStatus() == TaskTerm)
        {
        	//Unreserve the frame so that the next IORB can be handled
        	if (InterruptFrame.getReserved() == InterruptThread.getTask())
        	{
        		InterruptFrame.setUnreserved(InterruptThread.getTask()); 
        	}
        }
        
        //Tell the threads what has happened. 
        Interrupted.notifyThreads();
        
        //the device will no longer be busy
        int InterruptDeviceID = Interrupted.getDeviceID();
        Device InterruptDevice = Device.get(InterruptDeviceID);
        InterruptDevice.setBusy(false);
        
        //and we should dequeue it from the Queue 
        IORB nextIORB = InterruptDevice.dequeueIORB(); 
        if (nextIORB != null)
        {
        	//and start the next IORB if there is one
        	InterruptDevice.startIO(nextIORB);
        }
        	    	
        //Then dispatch the thread to be moved along the program
        ThreadCB.dispatch(); 
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
