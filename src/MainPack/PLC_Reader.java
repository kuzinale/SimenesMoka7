package MainPack;

import Moka7.IntByRef;
import Moka7.S7;
import Moka7.S7Client;

/**
 * Created by kuzin.al on 16.01.2017.
 */
public class PLC_Reader extends Thread
{
    private static long Elapsed;
    private static byte[] Buffer = new byte[65536]; // 64K buffer (maximum for S7400 systems)
    private static final S7Client Client = new S7Client();
    private static int ok=0;
    private static int ko=0;
    private static String IpAddress = "192.168.0.40";
    private static int Rack = 0; // Default 0 for S7300
    private static int Slot = 2; // Default 2 for S7300
    private static int DBSample = 250; // Sample DB that must be present in the CPU
    private static int DataToMove = 196; // Data size to read/write
    private static int CurrentStatus = S7.S7CpuStatusUnknown;


//TODO: Запуск чтения и записи данных PLC в память
    @Override
    public void run()
    {
        while(true)
        {
            if (!Client.Connected)
            {
                Connect();
                if (Client.Connected)
                {
                    DBGet();
                }
            }
            while (Client.Connected)
            {
                DBRead();
            }
        }
    }

    public static void HexDump(byte[] Buffer, int Size)
    {
        int r=0;
        String Hex = "";

        for (int i=0; i<Size; i++)
        {
            int v = (Buffer[i] & 0x0FF);
            String hv = Integer.toHexString(v);

            if (hv.length()==1)
                hv="0"+hv+" ";
            else
                hv=hv+" ";

            Hex=Hex+hv;

            r++;
            if (r==16)
            {
                System.out.print(Hex+" ");
                System.out.println(S7.GetPrintableStringAt(Buffer, i-15, 16));
                Hex="";
                r=0;
            }
        }
        int L=Hex.length();
        if (L>0)
        {
            while (Hex.length()<49)
                Hex=Hex+" ";
            System.out.print(Hex);
            System.out.println(S7.GetPrintableStringAt(Buffer, Size-r, r));
        }
        else
            System.out.println();
    }

    static void TestBegin(String FunctionName)
    {
        System.out.println();
        System.out.println("+================================================================");
        System.out.println("| "+FunctionName);
        System.out.println("+================================================================");
        Elapsed = System.currentTimeMillis();
    }

    static void TestEnd(int Result)
    {
        if (Result!=0)
        {
            ko++;
            Error(Result);
        }
        else
            ok++;
        System.out.println("Execution time "+(System.currentTimeMillis()-Elapsed)+" ms");
    }

    static void Error(int Code)
    {
        System.out.println(S7Client.ErrorText(Code));
    }

    public static boolean DBGet()
    {
        IntByRef SizeRead = new IntByRef(0);
        TestBegin("DBGet()");
        int Result = Client.DBGet(DBSample, Buffer, SizeRead);
        TestEnd(Result);
        if (Result==0)
        {
            DataToMove = SizeRead.Value; // Stores DB size for next test
            System.out.println("DB "+DBSample+" - Size read "+DataToMove+" bytes");
            HexDump(Buffer, DataToMove);
            return true;
        }
        return false;
    }

    public static void DBRead()
    {
        TestBegin("ReadArea()");
        int Result = Client.ReadArea(S7.S7AreaDB, DBSample, 0, DataToMove, Buffer);
        if (Result==0)
        {
            System.out.println("DB "+DBSample+" succesfully read using size reported by DBGet()");
        }
        TestEnd(Result);
    }

    /**
     * Performs read and write on a givtrueen DB
     */
    public static void DBPlay()
    {
        // We use DBSample (default = DB 1) as DB Number
        // modify it if it doesn't exists into the CPU.
        if (DBGet())
        {
            DBRead();
        }
    }

    public static void Delay(int millisec)
    {
        try {
            Thread.sleep(millisec);
        }
        catch (InterruptedException e) {}
    }

    public static boolean Connect()
    {
        TestBegin("ConnectTo()");
        Client.SetConnectionType(S7.OP);
        int Result = Client.ConnectTo(IpAddress, Rack, Slot);
        if (Result==0)
        {
            System.out.println("Connected to   : " + IpAddress + " (Rack=" + Rack + ", Slot=" + Slot+ ")");
            System.out.println("PDU negotiated : " + Client.PDULength()+" bytes");
        }
        TestEnd(Result);
        return Result == 0;
    }


}
