package osp.Devices;
import osp.IFLModules.*;
import osp.FileSys.OpenFile;
import osp.Threads.ThreadCB;
import osp.Memory.PageTableEntry;

/**
 * @OSPProject Devices
 * @author Dion de Jong
 * @email Ddejong@email.sc.edu
 * @version Final
 * @Date 4-18-15
 * @class IORB
 * This class contains all the information necessary to carry out
 * an I/O request.
 */ 
    
public class IORB extends IflIORB
{
    /**
     * @OSPProject Devices
     * The IORB constructor:
     * Must have super(thread,page,blockNumber,deviceID,ioType,openFile);
	 * as its first statement.
     */
    public IORB(ThreadCB thread, PageTableEntry page, 
		int blockNumber, int deviceID, 
		int ioType, OpenFile openFile) 
    {
        //Call the constructor 
    	super(thread, page, blockNumber, deviceID, ioType, openFile);
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
