package pl.wp.cloud.service.helper;


import junit.framework.TestCase;

public class TransferTest extends TestCase{

    public TransferTest(String name) {
        super(name);
    }

    public void testSendFile() {

        System.out.print(Transfer.sendFileToServer("http://192.168.1.4:8888/","~/Pobrane/zaswiadczenie.html"));

    }
}
